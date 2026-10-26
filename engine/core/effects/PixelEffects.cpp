#include "../document/pages/PagedValues.hpp"
#include "../document/tiles/LayerPixels.hpp"
#include "velyntora/PixelEffects.hpp"

#include <algorithm>
#include <array>
#include <cmath>
#include <random>
#include <stdexcept>
namespace velyntora {
namespace {
using Sample = std::array<std::uint16_t, 4>;
using Sum = std::array<std::int64_t, 4>;
Sample sample(std::uint32_t pixel) {
  auto a = pixel >> 24;
  return {std::uint16_t(a), std::uint16_t(((pixel >> 16) & 255) * a),
          std::uint16_t(((pixel >> 8) & 255) * a), std::uint16_t((pixel & 255) * a)};
}
void add(Sum& sum, const Sample& s, int sign) {
  for (int i = 0; i < 4; ++i) sum[i] += sign * s[i];
}
std::uint32_t color(const Sum& sum, int count) {
  if (sum[0] <= 0) return 0;
  auto a = std::uint32_t((sum[0] + count / 2) / count);
  if (a == 0) return 0;
  std::uint32_t result = a << 24;
  for (int i = 1; i < 4; ++i)
    result |= std::uint32_t(
                  std::clamp((sum[i] + sum[0] / 2) / sum[0], std::int64_t(0), std::int64_t(255)))
              << (24 - i * 8);
  return result;
}
}  // namespace
tiles::LayerPixels pixelEffect(const tiles::LayerPixels& src, int w, int h,
                                       int kind, int amount) {
  if (w <= 0 || h <= 0 || std::int64_t(w) * h != std::int64_t(src.size()) || kind < 0 || kind > 6 ||
      amount < 1 || amount > 100)
    throw std::invalid_argument("Invalid effect parameters");
  auto out = src;
  if (kind == 0) {
    int radius = std::min(amount, 64), count = radius * 2 + 1;
    pages::PagedValues<Sample> horizontal(src.size());
    for (int y = 0; y < h; ++y) {
      Sum sum{};
      for (int x = -radius; x <= radius; ++x)
        add(sum, sample(src[std::size_t(y) * w + std::clamp(x, 0, w - 1)]), 1);
      for (int x = 0; x < w; ++x) {
        auto pixel = horizontal[std::size_t(y) * w + x];
        for (int c = 0; c < 4; ++c) pixel[c] = std::uint16_t((sum[c] + count / 2) / count);
        add(sum, sample(src[std::size_t(y) * w + std::clamp(x - radius, 0, w - 1)]), -1);
        add(sum, sample(src[std::size_t(y) * w + std::clamp(x + radius + 1, 0, w - 1)]), 1);
      }
    }
    for (int x = 0; x < w; ++x) {
      Sum sum{};
      for (int y = -radius; y <= radius; ++y)
        add(sum, horizontal[std::size_t(std::clamp(y, 0, h - 1)) * w + x], 1);
      for (int y = 0; y < h; ++y) {
        out[std::size_t(y) * w + x] = color(sum, count);
        add(sum, horizontal[std::size_t(std::clamp(y - radius, 0, h - 1)) * w + x], -1);
        add(sum, horizontal[std::size_t(std::clamp(y + radius + 1, 0, h - 1)) * w + x], 1);
      }
    }
  } else if (kind <= 3) {
    const std::array<int, 9> kernel = kind == 1 ? std::array<int, 9>{0, -1, 0, -1, 5, -1, 0, -1, 0}
                                      : kind == 2
                                          ? std::array<int, 9>{-1, -1, -1, -1, 8, -1, -1, -1, -1}
                                          : std::array<int, 9>{-2, -1, 0, -1, 1, 1, 0, 1, 2};
    for (int y = 0; y < h; ++y)
      for (int x = 0; x < w; ++x) {
        auto pos = std::size_t(y) * w + x;
        if (!(src[pos] >> 24)) continue;
        std::uint32_t result = src[pos] & 0xff000000;
        for (int shift : {0, 8, 16}) {
          int value = kind == 3 ? 128 : 0, k = 0;
          for (int dy = -1; dy <= 1; ++dy)
            for (int dx = -1; dx <= 1; ++dx) {
              auto pixel =
                  src[std::size_t(std::clamp(y + dy, 0, h - 1)) * w + std::clamp(x + dx, 0, w - 1)];
              value += kernel[k++] * int(((pixel >> shift) & 255) * (pixel >> 24) / 255);
            }
          int before = (src[pos] >> shift) & 255;
          value = std::clamp(value, 0, 255);
          value = (before * (100 - amount) + value * amount + 50) / 100;
          result |= std::uint32_t(value) << shift;
        }
        out[pos] = result;
      }
  } else if (kind == 4) {
    int block = std::min(amount, 64);
    for (int top = 0; top < h; top += block)
      for (int left = 0; left < w; left += block) {
        int right = std::min(w, left + block), bottom = std::min(h, top + block);
        Sum sum{};
        for (int y = top; y < bottom; ++y)
          for (int x = left; x < right; ++x) add(sum, sample(src[std::size_t(y) * w + x]), 1);
        auto pixel = color(sum, (right - left) * (bottom - top));
        for (int y = top; y < bottom; ++y)
          for (int x = left; x < right; ++x) out[std::size_t(y) * w + x] = pixel;
      }
  } else if (kind == 5) {
    std::mt19937 random(0x564c5943);
    std::uniform_int_distribution<int> noise(-amount, amount);
    for (std::size_t i = 0; i < src.size(); ++i) {
      if (!(src[i] >> 24)) continue;
      int delta = noise(random);
      std::uint32_t result = src[i] & 0xff000000;
      for (int shift : {0, 8, 16})
        result |= std::uint32_t(std::clamp(int((src[i] >> shift) & 255) + delta, 0, 255)) << shift;
      out[i] = result;
    }
  } else {
    for (int y = 0; y < h; ++y)
      for (int x = 0; x < w; ++x) {
        auto pos = std::size_t(y) * w + x;
        double dx = (x + .5 - w / 2.) / (w / 2.), dy = (y + .5 - h / 2.) / (h / 2.);
        double factor = std::clamp(1 - amount / 100. * (dx * dx + dy * dy) / 2, 0., 1.);
        auto pixel = src[pos] & 0xff000000;
        for (int shift : {0, 8, 16})
          pixel |= std::uint32_t(std::lround(((src[pos] >> shift) & 255) * factor)) << shift;
        out[pos] = pixel;
      }
  }
  return out;
}
}  // namespace velyntora
