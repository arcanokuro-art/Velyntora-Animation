#pragma once
#include <algorithm>
#include <cstdint>
#include <cstdio>
#include <limits>
#include <list>
#include <map>
#include <memory>
#include <stdexcept>
#include <vector>

namespace velyntora::tiles {
// Experimental, independent of the dense Canvas API until the Android migration.
// Immutable disk records let document/history copies share untouched blocks.
class TileSurface {
 public:
  static constexpr int side = 256;
  static constexpr int maxSide = 6000;
  static constexpr std::size_t tilePixels = side * side;
  using Pixels = std::vector<std::uint32_t>;

 private:
  struct Record { long offset; };
  struct Store {
    struct Cached {
      std::shared_ptr<const Pixels> pixels;
      std::list<long>::iterator position;
    };
    std::FILE* file;
    std::size_t capacity;
    std::list<long> order;
    std::map<long, Cached> cache;
    explicit Store(std::size_t bytes) : file(std::tmpfile()),
        capacity(std::max(std::size_t(1), bytes / (tilePixels * sizeof(std::uint32_t)))) {
      if (!file) throw std::runtime_error("Cannot create canvas backing file");
    }
    ~Store() { if (file) std::fclose(file); }
    Store(const Store&) = delete;
    Store& operator=(const Store&) = delete;
    void remember(long offset, std::shared_ptr<const Pixels> pixels) {
      while (cache.size() >= capacity) {
        cache.erase(order.back()); order.pop_back();
      }
      order.push_front(offset);
      try { cache.emplace(offset, Cached{std::move(pixels), order.begin()}); }
      catch (...) { order.pop_front(); throw; }
    }
    std::shared_ptr<const Pixels> read(const Record& record) {
      auto it = cache.find(record.offset);
      if (it != cache.end()) {
        order.splice(order.begin(), order, it->second.position);
        return it->second.pixels;
      }
      auto pixels = std::make_shared<Pixels>(tilePixels);
      if (std::fseek(file, record.offset, SEEK_SET) != 0 ||
          std::fread(pixels->data(), sizeof(std::uint32_t), tilePixels, file) != tilePixels)
        throw std::runtime_error("Cannot read canvas backing file");
      remember(record.offset, pixels);
      return pixels;
    }
    std::shared_ptr<const Record> append(std::shared_ptr<const Pixels> pixels) {
      if (std::fseek(file, 0, SEEK_END) != 0)
        throw std::runtime_error("Cannot seek canvas backing file");
      const long offset = std::ftell(file);
      if (offset < 0 || offset > std::numeric_limits<long>::max() -
          long(tilePixels * sizeof(std::uint32_t)))
        throw std::runtime_error("Canvas backing file too large");
      if (std::fwrite(pixels->data(), sizeof(std::uint32_t), tilePixels, file) != tilePixels ||
          std::fflush(file) != 0)
        throw std::runtime_error("Cannot write canvas backing file");
      auto result = std::make_shared<Record>(Record{offset});
      remember(offset, std::move(pixels));
      return result;
    }
  };
  int width_, height_, columns_;
  std::uint32_t background_;
  std::shared_ptr<Store> store_;
  std::map<int, std::shared_ptr<const Record>> records_;
  void validateRegion(int x, int y, int w, int h) const {
    if (x < 0 || y < 0 || w <= 0 || h <= 0 || w > width_ || h > height_ ||
        x > width_ - w || y > height_ - h)
      throw std::invalid_argument("Invalid tile region");
    // No caller can accidentally allocate a whole 144-million-pixel image.
    if (std::uint64_t(w) * h > 1048576)
      throw std::invalid_argument("Region must be streamed in bounded strips");
  }

 public:
  explicit TileSurface(int w, int h, std::uint32_t background = 0xFFFFFFFFu,
                       std::size_t cacheBytes = 8 * 1024 * 1024)
      : width_(w), height_(h), columns_(0), background_(background) {
    if (w <= 0 || h <= 0 || w > maxSide || h > maxSide)
      throw std::invalid_argument("Invalid tiled canvas dimensions");
    columns_ = (w + side - 1) / side;
    store_ = std::make_shared<Store>(cacheBytes);
  }
  int width() const noexcept { return width_; }
  int height() const noexcept { return height_; }
  std::size_t allocatedTiles() const noexcept { return records_.size(); }
  std::size_t cachedBytes() const noexcept {
    return store_->cache.size() * tilePixels * sizeof(std::uint32_t);
  }
  // Reclaims unreachable disk versions for this surface. Existing snapshots
  // retain their old backing file until they are released.
  void compact() {
    auto replacementStore = std::make_shared<Store>(
        store_->capacity * tilePixels * sizeof(std::uint32_t));
    std::map<int, std::shared_ptr<const Record>> replacement;
    for (const auto& [key, record] : records_)
      replacement.emplace(key, replacementStore->append(store_->read(*record)));
    records_.swap(replacement);
    store_.swap(replacementStore);
  }
  void clear(std::uint32_t color) noexcept {
    records_.clear(); background_ = color;
  }
  Pixels readRegion(int x, int y, int w, int h) const {
    validateRegion(x,y,w,h);
    Pixels output(std::size_t(w) * h, background_);
    for (int ty = y / side; ty <= (y+h-1) / side; ++ty)
      for (int tx = x / side; tx <= (x+w-1) / side; ++tx) {
        auto found = records_.find(ty * columns_ + tx);
        if (found == records_.end()) continue;
        auto pixels = store_->read(*found->second);
        const int left = std::max(x,tx*side), right = std::min(x+w,(tx+1)*side);
        for (int row = std::max(y,ty*side); row < std::min(y+h,(ty+1)*side); ++row)
          std::copy_n(pixels->begin() + (row-ty*side)*side + left-tx*side, right-left,
                      output.begin() + std::size_t(row-y)*w + left-x);
      }
    return output;
  }
  // Strong document guarantee: failed disk writes/allocation do not replace pixels.
  void writeRegion(int x, int y, int w, int h, const Pixels& input) {
    validateRegion(x,y,w,h);
    if (input.size() != std::size_t(w)*h)
      throw std::invalid_argument("Tile region pixel count mismatch");
    auto replacement = records_;
    for (int ty = y / side; ty <= (y+h-1) / side; ++ty)
      for (int tx = x / side; tx <= (x+w-1) / side; ++tx) {
        const int key = ty * columns_ + tx;
        auto found = records_.find(key);
        auto pixels = found == records_.end()
            ? std::make_shared<Pixels>(tilePixels, background_)
            : std::make_shared<Pixels>(*store_->read(*found->second));
        const int left = std::max(x,tx*side), right = std::min(x+w,(tx+1)*side);
        for (int row = std::max(y,ty*side); row < std::min(y+h,(ty+1)*side); ++row)
          std::copy_n(input.begin() + std::size_t(row-y)*w + left-x, right-left,
                      pixels->begin() + (row-ty*side)*side + left-tx*side);
        if (std::all_of(pixels->begin(), pixels->end(),
                        [this](std::uint32_t p) { return p == background_; }))
          replacement.erase(key);
        else replacement[key] = store_->append(std::move(pixels));
      }
    records_.swap(replacement);
  }
};
} // namespace velyntora::tiles
