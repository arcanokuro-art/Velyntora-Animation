#include "../document/tiles/LayerPixels.hpp"
#include "velyntora/DistortionEffects.hpp"

#include <algorithm>
#include <array>
#include <cmath>
#include <random>
#include <stdexcept>
namespace velyntora {
namespace {
std::uint32_t sample(const tiles::LayerPixels& src, int w, int h, double x, double y) {
  x = std::clamp(x, 0., double(w - 1));
  y = std::clamp(y, 0., double(h - 1));
  int ix = int(x), iy = int(y);
  double fx = x - ix, fy = y - iy;
  std::array<double, 4> sum{};
  for (int dy = 0; dy < 2; dy++)
    for (int dx = 0; dx < 2; dx++) {
      auto p = src[std::size_t(std::min(iy + dy, h - 1)) * w + std::min(ix + dx, w - 1)];
      double a = (p >> 24) * (dx ? fx : 1 - fx) * (dy ? fy : 1 - fy);
      sum[0] += a;
      for (int c = 1; c < 4; c++) sum[c] += ((p >> (24 - c * 8)) & 255) * a;
    }
  int alpha = std::clamp(int(std::lround(sum[0])), 0, 255);
  if (!alpha || sum[0] <= 0) return 0;
  std::uint32_t result = std::uint32_t(alpha) << 24;
  for (int c = 1; c < 4; c++)
    result |= std::uint32_t(std::clamp(int(std::lround(sum[c] / sum[0])), 0, 255)) << (24 - c * 8);
  return result;
}
}  // namespace
tiles::LayerPixels distortionEffect(const tiles::LayerPixels& src, int w, int h,
                                            int kind, int amount, int size, int angle, int cx,
                                            int cy) {
  if (w < 1 || h < 1 || std::int64_t(w) * h != std::int64_t(src.size()) || kind < 0 || kind > 4 ||
      amount < -180 || amount > 180 || (kind > 0 && (amount < -100 || amount > 100)) || size < 1 ||
      size > 100 || angle < -180 || angle > 180 || cx < 0 || cx > 100 || cy < 0 || cy > 100)
    throw std::invalid_argument("Invalid distortion parameters");
  if (amount == 0) return src;
  auto out = src;
  constexpr double pi = 3.14159265358979323846;
  double centerX = (w - 1) * cx / 100., centerY = (h - 1) * cy / 100.,
         radius = std::max(.5, std::min(w, h) * size / 200.), direction = angle * pi / 180.;
  std::mt19937 random(0x564c5943);
  std::uniform_real_distribution<double> scatter(-1, 1);
  for (int y = 0; y < h; y++)
    for (int x = 0; x < w; x++) {
      double dx = x - centerX, dy = y - centerY, r = std::hypot(dx, dy), sx = x, sy = y;
      if (kind == 0) {
        if (r >= radius) continue;
        double rotation = amount * pi / 180. * std::pow(1 - r / radius, 2);
        sx = centerX + dx * std::cos(rotation) - dy * std::sin(rotation);
        sy = centerY + dx * std::sin(rotation) + dy * std::cos(rotation);
      } else if (kind == 1) {
        if (r >= radius || r == 0) continue;
        double factor = std::pow(r / radius, amount / 100.);
        sx = centerX + dx * factor;
        sy = centerY + dy * factor;
      } else if (kind == 2) {
        if (r == 0) continue;
        double factor = (r + amount / 5. * std::sin(r * 2 * pi / size)) / r;
        sx = centerX + dx * factor;
        sy = centerY + dy * factor;
      } else if (kind == 3) {
        double u = dx * std::cos(direction) + dy * std::sin(direction),
               v = -dx * std::sin(direction) + dy * std::cos(direction),
               du = amount / 5. * std::sin(v * 2 * pi / size),
               dv = amount / 5. * std::sin(u * 2 * pi / size);
        sx = x + du * std::cos(direction) - dv * std::sin(direction);
        sy = y + du * std::sin(direction) + dv * std::cos(direction);
      } else {
        sx += scatter(random) * std::abs(amount) / 5.;
        sy += scatter(random) * std::abs(amount) / 5.;
      }
      out[std::size_t(y) * w + x] = sample(src, w, h, sx, sy);
    }
  return out;
}
}  // namespace velyntora
