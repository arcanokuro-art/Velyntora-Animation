#include "../document/tiles/LayerPixels.hpp"
#include "velyntora/UtilityEffects.hpp"

#include <algorithm>
#include <array>
#include <cmath>
#include <stdexcept>
namespace velyntora {
namespace {
using Sample = std::array<double, 4>;
int c(std::uint32_t p, int shift) { return (p >> shift) & 255; }
int luminance(std::uint32_t p) {
  return (299 * c(p, 16) + 587 * c(p, 8) + 114 * c(p, 0) + 500) / 1000;
}
void add(Sample& sum, const Sample& p, double weight) {
  for (int i = 0; i < 4; i++) sum[i] += p[i] * weight;
}
Sample unpack(std::uint32_t p) {
  double a = p >> 24;
  return {a, c(p, 16) * a, c(p, 8) * a, c(p, 0) * a};
}
Sample sample(const tiles::LayerPixels& src, int w, int h, double x, double y) {
  x = std::clamp(x, 0., double(w - 1));
  y = std::clamp(y, 0., double(h - 1));
  int ix = int(x), iy = int(y);
  double fx = x - ix, fy = y - iy;
  Sample sum{};
  for (int dy = 0; dy < 2; dy++)
    for (int dx = 0; dx < 2; dx++)
      add(sum, unpack(src[std::size_t(std::min(iy + dy, h - 1)) * w + std::min(ix + dx, w - 1)]),
          (dx ? fx : 1 - fx) * (dy ? fy : 1 - fy));
  return sum;
}
std::uint32_t pack(const Sample& sum) {
  int a = std::clamp(int(std::lround(sum[0])), 0, 255);
  if (a == 0 || sum[0] <= 0) return 0;
  std::uint32_t p = std::uint32_t(a) << 24;
  for (int i = 1; i < 4; i++)
    p |= std::uint32_t(std::clamp(int(std::lround(sum[i] / sum[0])), 0, 255)) << (24 - i * 8);
  return p;
}
std::uint32_t hash(int x, int y, int seed) {
  auto n = std::uint32_t(x) * 0x9e3779b9U + std::uint32_t(y) * 0x85ebca6bU + std::uint32_t(seed);
  n ^= n >> 16;
  n *= 0x7feb352dU;
  n ^= n >> 15;
  n *= 0x846ca68bU;
  return n ^ (n >> 16);
}
double noise(double x, double y, int seed) {
  int ix = int(std::floor(x)), iy = int(std::floor(y));
  double fx = x - ix, fy = y - iy;
  fx = fx * fx * (3 - 2 * fx);
  fy = fy * fy * (3 - 2 * fy);
  return ((hash(ix, iy, seed) / 4294967295.) * (1 - fx) +
          (hash(ix + 1, iy, seed) / 4294967295.) * fx) *
             (1 - fy) +
         ((hash(ix, iy + 1, seed) / 4294967295.) * (1 - fx) +
          (hash(ix + 1, iy + 1, seed) / 4294967295.) * fx) *
             fy;
}
}  // namespace
tiles::LayerPixels utilityEffect(const tiles::LayerPixels& src, int w, int h,
                                         int kind, int amount, int size, int parameter) {
  if (w < 1 || h < 1 || std::int64_t(w) * h != std::int64_t(src.size()) || kind < 0 || kind > 7 ||
      amount < 0 || amount > 100 || size < 1 || size > 128 ||
      (kind == 0 &&
       (amount > 64 || size < 2 || size > 16 || parameter < -180 || parameter > 180)) ||
      (kind == 1 && amount > 32) || (kind == 2 && (parameter < 0 || parameter > 10000)) ||
      (kind == 4 && (size > 3 || parameter < 0 || parameter > 255)) || (kind == 5 && size > 8) ||
      (kind == 6 && (size > 8 || parameter < -180 || parameter > 180)) ||
      (kind == 7 && (size < 2 || size > 16)))
    throw std::invalid_argument("Invalid utility effect");
  if (amount == 0) return src;
  auto out = src;
  constexpr double pi = 3.14159265358979323846;
  std::vector<std::array<double, 2>> offsets;
  if (kind == 0) {
    for (int i = 0; i < size; i++) {
      double angle = (parameter + 360. * i / size) * pi / 180.;
      offsets.push_back({std::cos(angle) * amount, std::sin(angle) * amount});
    }
  }
  if (kind == 1) {
    for (int i = 0; i < 24; i++) {
      double r = amount * std::sqrt((i + .5) / 24.), angle = i * 2.399963229728653;
      offsets.push_back({std::cos(angle) * r, std::sin(angle) * r});
      offsets.push_back({-std::cos(angle) * r, -std::sin(angle) * r});
    }
  }
  constexpr int bayer[16] = {0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5};
  for (int y = 0; y < h; y++)
    for (int x = 0; x < w; x++) {
      auto pos = std::size_t(y) * w + x;
      auto p = src[pos];
      if (kind <= 3) {
        Sample sum{};
        if (kind <= 1) {
          for (auto offset : offsets)
            add(sum, sample(src, w, h, x + offset[0], y + offset[1]), 1. / offsets.size());
        } else if (kind == 2) {
          double sx = x + (noise(x / double(size), y / double(size), parameter) - .5) * amount / 2.,
                 sy = y + (noise(x / double(size), y / double(size), parameter + 7919) - .5) *
                              amount / 2.;
          sum = sample(src, w, h, sx, sy);
        } else {
          double dx = x - (w - 1) / 2., dy = y - (h - 1) / 2., r2 = dx * dx + dy * dy,
                 radius = std::min(w, h) * size / 200.;
          if (r2 == 0) continue;
          double factor = 1 - amount / 100. + amount / 100. * radius * radius / r2;
          sum = sample(src, w, h, (w - 1) / 2. + dx * factor, (h - 1) / 2. + dy * factor);
        }
        out[pos] = pack(sum);
        continue;
      }
      if (!(p >> 24)) continue;
      std::uint32_t result = p & 0xff000000;
      if (kind == 4) {
        Sample sum{};
        double total = 0;
        for (int dy = -size; dy <= size; dy++)
          for (int dx = -size; dx <= size; dx++) {
            auto neighbor =
                src[std::size_t(std::clamp(y + dy, 0, h - 1)) * w + std::clamp(x + dx, 0, w - 1)];
            if (!(neighbor >> 24)) continue;
            int difference = 0;
            for (int shift : {0, 8, 16})
              difference = std::max(difference, std::abs(c(p, shift) - c(neighbor, shift)));
            if (difference > parameter) continue;
            double weight = 1. / (1 + dx * dx + dy * dy);
            add(sum, unpack(neighbor), weight);
            total += weight;
          }
        if (total == 0) continue;
        auto smooth = pack(sum);
        for (int shift : {0, 8, 16})
          result |=
              std::uint32_t((c(p, shift) * (100 - amount) + c(smooth, shift) * amount + 50) / 100)
              << shift;
      } else if (kind == 5) {
        int low = 255, high = 0;
        for (int dy = -size; dy <= size; dy++)
          for (int dx = -size; dx <= size; dx++) {
            auto neighbor =
                src[std::size_t(std::clamp(y + dy, 0, h - 1)) * w + std::clamp(x + dx, 0, w - 1)];
            int value = (neighbor >> 24) ? luminance(neighbor) : luminance(p);
            low = std::min(low, value);
            high = std::max(high, value);
          }
        int edge = 255 - (high - low);
        for (int shift : {0, 8, 16})
          result |= std::uint32_t((c(p, shift) * (100 - amount) + edge * amount + 50) / 100)
                    << shift;
      } else if (kind == 6) {
        double angle = parameter * pi / 180.;
        auto neighbor =
            pack(sample(src, w, h, x + std::cos(angle) * size, y + std::sin(angle) * size));
        for (int shift : {0, 8, 16}) {
          int value = std::clamp(128 + c(p, shift) - c(neighbor, shift), 0, 255);
          result |= std::uint32_t((c(p, shift) * (100 - amount) + value * amount + 50) / 100)
                    << shift;
        }
      } else {
        for (int shift : {0, 8, 16}) {
          double biased =
              c(p, shift) * (size - 1) / 255. + (bayer[(y % 4) * 4 + x % 4] + .5) / 16. - .5;
          int band = std::clamp(int(std::lround(biased)), 0, size - 1),
              value = (band * 255 + (size - 1) / 2) / (size - 1);
          result |= std::uint32_t((c(p, shift) * (100 - amount) + value * amount + 50) / 100)
                    << shift;
        }
      }
      out[pos] = result;
    }
  return out;
}
}  // namespace velyntora
