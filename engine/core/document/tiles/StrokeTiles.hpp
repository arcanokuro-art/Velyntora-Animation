#pragma once
#include <algorithm>
#include <cstdint>
#include <map>
#include <vector>
#include "LayerPixels.hpp"

namespace velyntora::tiles {
// Preserve a stroke's original pixels only where the stroke actually touches.
// Coverage is requested before baseline reads by both existing rasterizers.
class StrokeTiles {
  static constexpr std::size_t blockSize = 65536;
  struct Block {
    std::vector<std::uint32_t> base;
    std::vector<float> coverage;
  };
  const LayerPixels* source_ = nullptr;
  std::map<std::size_t,Block> blocks_;
  Block& block(std::size_t index) {
    auto key=index/blockSize;
    auto it=blocks_.find(key);
    if(it!=blocks_.end()) return it->second;
    auto start=key*blockSize;
    auto count=std::min(blockSize,source_->size()-start);
    Block value{std::vector<std::uint32_t>(count),std::vector<float>(count,0.f)};
    source_->readSpan(start,count,value.base.data());
    return blocks_.emplace(key,std::move(value)).first->second;
  }
 public:
  void begin(const LayerPixels& pixels) {
    end();source_=&pixels;
  }
  void end() noexcept { blocks_.clear();source_=nullptr; }
  void rebind(const LayerPixels& pixels) noexcept {
    if (source_) source_=&pixels;
  }
  std::size_t size() const noexcept {return source_?source_->size():0;}
  std::size_t allocatedBlocks() const noexcept {return blocks_.size();}
  std::uint32_t base(std::size_t i) {return block(i).base[i%blockSize];}
  float& coverage(std::size_t i) {return block(i).coverage[i%blockSize];}
  struct Baseline {
    StrokeTiles* owner;
    std::size_t size() const noexcept {return owner->size();}
    std::uint32_t operator[](std::size_t i) {return owner->base(i);}
  };
  struct Coverage {
    StrokeTiles* owner;
    float& operator[](std::size_t i) {return owner->coverage(i);}
  };
};
}
