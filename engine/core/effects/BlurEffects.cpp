#include "../document/pages/PagedValues.hpp"
#include "../document/tiles/LayerPixels.hpp"
#include "velyntora/BlurEffects.hpp"

#include <algorithm>
#include <array>
#include <cmath>
#include <stdexcept>
namespace velyntora {
namespace {
using Sample = std::array<double, 4>;
Sample unpack(std::uint32_t p) {
  double a = p >> 24;
  return {a, ((p >> 16) & 255) * a, ((p >> 8) & 255) * a, (p & 255) * a};
}
void add(Sample& sum, const Sample& p, double weight) {
  for (int c = 0; c < 4; c++) sum[c] += p[c] * weight;
}
std::uint32_t pack(const Sample& p) {
  int alpha = std::clamp(int(std::lround(p[0])), 0, 255);
  if (!alpha || p[0] <= 0) return 0;
  std::uint32_t result = std::uint32_t(alpha) << 24;
  for (int c = 1; c < 4; c++)
    result |= std::uint32_t(std::clamp(int(std::lround(p[c] / p[0])), 0, 255)) << (24 - c * 8);
  return result;
}
Sample bilinear(const tiles::LayerPixels& pixels, int w, int h, double x, double y) {
  x = std::clamp(x, 0., double(w - 1));
  y = std::clamp(y, 0., double(h - 1));
  int ix = int(x), iy = int(y);
  double fx = x - ix, fy = y - iy;
  Sample sum{};
  add(sum, unpack(pixels[std::size_t(iy) * w + ix]), (1 - fx) * (1 - fy));
  add(sum, unpack(pixels[std::size_t(iy) * w + std::min(ix + 1, w - 1)]), fx * (1 - fy));
  add(sum, unpack(pixels[std::size_t(std::min(iy + 1, h - 1)) * w + ix]), (1 - fx) * fy);
  add(sum, unpack(pixels[std::size_t(std::min(iy + 1, h - 1)) * w + std::min(ix + 1, w - 1)]),
      fx * fy);
  return sum;
}
}  // namespace
tiles::LayerPixels blurEffect(const tiles::LayerPixels& src, int w, int h, int kind,
                                      int amount, int angle, int cx, int cy) {
  if (w < 1 || h < 1 || std::int64_t(w) * h != std::int64_t(src.size()) || kind < 0 || kind > 3 ||
      amount < 0 || amount > 100 || (kind == 0 && amount > 32) || (kind == 2 && amount > 45) ||
      angle < -180 || angle > 180 || cx < 0 || cx > 100 || cy < 0 || cy > 100)
    throw std::invalid_argument("Invalid blur parameters");
  if (amount == 0) return src;
  auto out = src;
  if (kind == 0) {
    int radius = amount;
    double sigma = std::max(.5, amount / 2.), total = 0;
    std::vector<double> weights(2 * radius + 1);
    for (int i = -radius; i <= radius; i++) {
      double value = std::exp(-double(i * i) / (2 * sigma * sigma));
      weights[i + radius] = value;
      total += value;
    }
    for (auto& value : weights) value /= total;
    pages::PagedValues<std::array<float,4>> horizontal(src.size());
    for (int y = 0; y < h; y++)
      for (int x = 0; x < w; x++) {
        Sample sum{};
        for (int dx = -radius; dx <= radius; dx++)
          add(sum, unpack(src[std::size_t(y) * w + std::clamp(x + dx, 0, w - 1)]),
              weights[dx + radius]);
        for (int c = 0; c < 4; c++) horizontal[std::size_t(y) * w + x][c] = float(sum[c]);
      }
    for (int y = 0; y < h; y++)
      for (int x = 0; x < w; x++) {
        Sample sum{};
        for (int dy = -radius; dy <= radius; dy++) {
          auto p = static_cast<std::array<float,4>>(horizontal[std::size_t(std::clamp(y + dy, 0, h - 1)) * w + x]);
          for (int c = 0; c < 4; c++) sum[c] += p[c] * weights[dy + radius];
        }
        out[std::size_t(y) * w + x] = pack(sum);
      }
  } else {
    constexpr double pi = 3.14159265358979323846;
    double direction = angle * pi / 180., centerX = (w - 1) * cx / 100.,
           centerY = (h - 1) * cy / 100.;
    int samples = std::clamp(amount + 1, 2, 32);
    for (int y = 0; y < h; y++)
      for (int x = 0; x < w; x++) {
        Sample sum{};
        for (int i = 0; i < samples; i++) {
          double t = i / double(samples - 1), sx = x, sy = y;
          if (kind == 1) {
            double distance = (t - .5) * amount;
            sx += std::cos(direction) * distance;
            sy += std::sin(direction) * distance;
          } else if (kind == 2) {
            double rotation = (t - .5) * amount * pi / 180., dx = x - centerX, dy = y - centerY;
            sx = centerX + dx * std::cos(rotation) - dy * std::sin(rotation);
            sy = centerY + dx * std::sin(rotation) + dy * std::cos(rotation);
          } else {
            double scale = 1 - t * amount / 200.;
            sx = centerX + (x - centerX) * scale;
            sy = centerY + (y - centerY) * scale;
          }
          add(sum, bilinear(src, w, h, sx, sy), 1. / samples);
        }
        out[std::size_t(y) * w + x] = pack(sum);
      }
  }
  return out;
}
}  // namespace velyntora
