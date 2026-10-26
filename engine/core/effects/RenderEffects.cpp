#include "../document/tiles/LayerPixels.hpp"
#include "velyntora/RenderEffects.hpp"

#include <algorithm>
#include <cmath>
#include <limits>
#include <stdexcept>
namespace velyntora {
namespace {
std::uint32_t hash(int x, int y, std::uint32_t seed) {
  std::uint32_t n = std::uint32_t(x) * 0x9e3779b9U + std::uint32_t(y) * 0x85ebca6bU + seed;
  n ^= n >> 16;
  n *= 0x7feb352dU;
  n ^= n >> 15;
  n *= 0x846ca68bU;
  return n ^ (n >> 16);
}
double random(int x, int y, std::uint32_t seed) { return hash(x, y, seed) / 4294967295.; }
double noise(double x, double y, std::uint32_t seed) {
  int ix = int(std::floor(x)), iy = int(std::floor(y));
  double fx = x - ix, fy = y - iy;
  fx = fx * fx * (3 - 2 * fx);
  fy = fy * fy * (3 - 2 * fy);
  return (random(ix, iy, seed) * (1 - fx) + random(ix + 1, iy, seed) * fx) * (1 - fy) +
         (random(ix, iy + 1, seed) * (1 - fx) + random(ix + 1, iy + 1, seed) * fx) * fy;
}
std::uint32_t color(std::uint32_t a, std::uint32_t b, double t) {
  t = std::clamp(t, 0., 1.);
  double aa = (a >> 24) * (1 - t), ab = (b >> 24) * t, alpha = aa + ab;
  int rounded = int(std::lround(alpha));
  if (!rounded) return 0;
  std::uint32_t value = std::uint32_t(rounded) << 24;
  for (int shift : {0, 8, 16})
    value |= std::uint32_t(std::clamp(
                 int(std::lround((((a >> shift) & 255) * aa + ((b >> shift) & 255) * ab) / alpha)),
                 0, 255))
             << shift;
  return value;
}
}  // namespace
tiles::LayerPixels renderEffect(int w, int h, int kind, int scale, int detail,
                                        std::uint32_t seed, std::uint32_t first,
                                        std::uint32_t second) {
  if (w < 1 || h < 1 || w > 8192 || h > 8192 || std::int64_t(w) * h > 4000000 || kind < 0 ||
      kind > 4 || scale < 1 || scale > 512 || detail < 1 || detail > 128)
    throw std::invalid_argument("Invalid render parameters");
  tiles::LayerPixels out(std::size_t(w) * h);
  for (int y = 0; y < h; y++)
    for (int x = 0; x < w; x++) {
      double value = 0;
      if (kind == 0) {
        double total = 0, weight = 1, frequency = 1;
        int octaves = std::min(detail, 8);
        for (int i = 0; i < octaves; i++) {
          value +=
              noise(x * frequency / scale, y * frequency / scale, seed + std::uint32_t(i) * 997) *
              weight;
          total += weight;
          frequency *= 2;
          weight *= .5;
        }
        value /= total;
      } else if (kind <= 2) {
        int ix = x / scale, iy = y / scale;
        double nearest = std::numeric_limits<double>::max();
        int bestX = 0, bestY = 0;
        for (int dy = -2; dy <= 2; dy++)
          for (int dx = -2; dx <= 2; dx++) {
            int gx = ix + dx, gy = iy + dy;
            double px = (gx + random(gx, gy, seed)) * scale,
                   py = (gy + random(gx, gy, seed ^ 0xa5a5a5a5U)) * scale,
                   distance = std::hypot(x - px, y - py);
            if (distance < nearest) {
              nearest = distance;
              bestX = gx;
              bestY = gy;
            }
          }
        value = kind == 1 ? random(bestX, bestY, seed ^ 0x3c6ef372U)
                          : std::clamp(nearest / (scale * .8), 0., 1.);
      } else {
        double span = 3.5 / scale,
               px = (x + .5 - w / 2.) / std::min(w, h) * span + (kind == 3 ? -.75 : 0),
               py = (y + .5 - h / 2.) / std::min(w, h) * span, zx = kind == 3 ? 0 : px,
               zy = kind == 3 ? 0 : py,
               cx = kind == 3   ? px
                    : seed == 0 ? -.8
                                : random(0, 0, seed) * 1.6 - .8,
               cy = kind == 3   ? py
                    : seed == 0 ? .156
                                : random(1, 0, seed) * 1.6 - .8;
        int iteration = 0;
        while (iteration < detail && zx * zx + zy * zy <= 4) {
          double next = zx * zx - zy * zy + cx;
          zy = 2 * zx * zy + cy;
          zx = next;
          iteration++;
        }
        value = iteration == detail ? 0 : 1 - iteration / double(detail);
      }
      out[std::size_t(y) * w + x] = color(first, second, value);
    }
  return out;
}
}  // namespace velyntora
