#include "../document/tiles/LayerPixels.hpp"
#include "velyntora/ArtisticEffects.hpp"

#include <algorithm>
#include <array>
#include <cmath>
#include <stdexcept>

#include "velyntora/BlurEffects.hpp"
namespace velyntora {
namespace {
int channel(std::uint32_t p, int shift) { return (p >> shift) & 255; }
int luminance(std::uint32_t p) {
  return (299 * channel(p, 16) + 587 * channel(p, 8) + 114 * channel(p, 0) + 500) / 1000;
}
int mix(int old, int value, int strength) {
  return std::clamp((old * (100 - strength) + value * strength + 50) / 100, 0, 255);
}
std::vector<std::uint32_t> neighborhood(const tiles::LayerPixels& src, int w, int h,
                                        int kind, int strength, int radius) {
  auto out = src;
  for (int y = 0; y < h; y++) {
    std::array<std::int64_t, 32> weights{};
    std::array<std::array<std::int64_t, 3>, 32> colors{};
    std::array<std::array<int, 256>, 3> histogram{};
    std::array<std::array<int, 16>, 3> blocks{};
    int total = 0;
    auto column = [&](int x, int sign) {
      x = std::clamp(x, 0, w - 1);
      for (int dy = -radius; dy <= radius; dy++) {
        auto p = src[std::size_t(std::clamp(y + dy, 0, h - 1)) * w + x];
        int alpha = p >> 24;
        if (!alpha) continue;
        if (kind == 0) {
          int bin = luminance(p) * (strength - 1) / 255;
          weights[bin] += sign * alpha;
          for (int c = 0; c < 3; c++) colors[bin][c] += sign * channel(p, 16 - c * 8) * alpha;
        } else {
          total += sign;
          for (int c = 0; c < 3; c++) {
            int value = channel(p, 16 - c * 8);
            histogram[c][value] += sign;
            blocks[c][value / 16] += sign;
          }
        }
      }
    };
    for (int x = -radius; x <= radius; x++) column(x, 1);
    for (int x = 0; x < w; x++) {
      auto pos = std::size_t(y) * w + x;
      auto p = src[pos];
      if (p >> 24) {
        std::uint32_t result = p & 0xff000000;
        if (kind == 0) {
          int best =
              int(std::max_element(weights.begin(), weights.begin() + strength) - weights.begin());
          for (int c = 0; c < 3; c++)
            result |= std::uint32_t((colors[best][c] + weights[best] / 2) / weights[best])
                      << (16 - c * 8);
        } else {
          for (int c = 0; c < 3; c++) {
            int target = (total - 1) * strength / 100, acc = 0, block = 0;
            while (block < 15 && acc + blocks[c][block] <= target) acc += blocks[c][block++];
            int value = block * 16;
            while (value < 255 && acc + histogram[c][value] <= target) acc += histogram[c][value++];
            result |= std::uint32_t(value) << (16 - c * 8);
          }
        }
        out[pos] = result;
      }
      column(x - radius, -1);
      column(x + radius + 1, 1);
    }
  }
  return out;
}

}  // namespace
tiles::LayerPixels artisticEffect(const tiles::LayerPixels& src, int w, int h,
                                          int kind, int strength, int radius, int threshold) {
  if (w < 1 || h < 1 || std::int64_t(w) * h != std::int64_t(src.size()) || kind < 0 || kind > 6 ||
      strength < 0 || strength > 100 || radius < 1 || radius > 16 || threshold < 0 ||
      threshold > 255 || (kind == 0 && (radius > 8 || strength < 2 || strength > 32)) ||
      (kind == 5 && radius > 4))
    throw std::invalid_argument("Invalid artistic parameters");
  if (strength == 0 && kind != 5) return src;
  if (kind == 0 || kind == 5) return neighborhood(src, w, h, kind, strength, radius);
  auto out = src;
  tiles::LayerPixels blurred;
  if (kind == 1 || kind == 3 || kind == 4) blurred = blurEffect(src, w, h, 0, radius, 0, 50, 50);
  for (int y = 0; y < h; y++)
    for (int x = 0; x < w; x++) {
      auto pos = std::size_t(y) * w + x;
      auto p = src[pos];
      if (!(p >> 24)) continue;
      std::uint32_t result = p & 0xff000000;
      if (kind == 1) {
        int base = luminance(p), smooth = luminance(blurred[pos]);
        int sketch = std::min(255, (base * 255 + std::max(1, smooth) / 2) / std::max(1, smooth));
        for (int shift : {0, 8, 16})
          result |= std::uint32_t(mix(channel(p, shift), sketch, strength)) << shift;
      } else if (kind == 2) {
        int gx = 0, gy = 0;
        constexpr int kx[] = {-1, 0, 1, -2, 0, 2, -1, 0, 1}, ky[] = {-1, -2, -1, 0, 0, 0, 1, 2, 1};
        int k = 0;
        for (int dy = -1; dy <= 1; dy++)
          for (int dx = -1; dx <= 1; dx++) {
            auto neighbor =
                src[std::size_t(std::clamp(y + dy, 0, h - 1)) * w + std::clamp(x + dx, 0, w - 1)];
            int value = (neighbor >> 24) ? luminance(neighbor) : luminance(p);
            gx += value * kx[k];
            gy += value * ky[k++];
          }
        bool edge = std::hypot(gx, gy) / 4 > threshold;
        for (int shift : {0, 8, 16}) {
          int old = channel(p, shift), ink = edge ? 0 : ((old * 5 + 127) / 255) * 51;
          result |= std::uint32_t(mix(old, ink, strength)) << shift;
        }
      } else if (kind == 6) {
        int r = channel(p, 16), g = channel(p, 8), b = channel(p, 0);
        if (r >= threshold && r * 2 > 3 * std::max(g, b)) r = mix(r, (g + b) / 2, strength);
        result |= std::uint32_t(r) << 16 | std::uint32_t(g) << 8 | std::uint32_t(b);
      } else {
        for (int shift : {0, 8, 16}) {
          int old = channel(p, shift), soft = channel(blurred[pos], shift),
              value = 255 - ((255 - old) * (255 - soft) + 127) / 255;
          if (kind == 4) {
            int warm = std::clamp(soft + (shift == 16 ? 8 : shift == 0 ? -8 : 0), 0, 255);
            value = (old + warm) / 2;
          }
          result |= std::uint32_t(mix(old, value, strength)) << shift;
        }
      }
      out[pos] = result;
    }
  return out;
}
}  // namespace velyntora
