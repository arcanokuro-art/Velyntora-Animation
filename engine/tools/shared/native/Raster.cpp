#include <algorithm>
#include <cmath>
#include <stdexcept>
#include <utility>

#include "velyntora/Canvas.hpp"
namespace velyntora {
void Canvas::sampledStroke(const tiles::LayerPixels& source, bool clone, int ox, int oy,
                           std::uint32_t target, std::uint32_t replacement, int tolerance, float x0,
                           float y0, float x1, float y1, float radius, float opacity,
                           float hardness, const std::vector<std::uint8_t>* mask) {
  if (source.size() != pixels_.size() || (mask && mask->size() != pixels_.size()) ||
      !std::isfinite(x0) || !std::isfinite(y0) || !std::isfinite(x1) || !std::isfinite(y1) ||
      !std::isfinite(radius) || !std::isfinite(opacity) || !std::isfinite(hardness) ||
      radius <= 0 || radius > 150)
    return;
  opacity = std::clamp(opacity, 0.f, 1.f);
  hardness = std::clamp(hardness, 0.f, 1.f);
  if (radius <= .5f) {
    x0 = std::floor(x0) + .5f; y0 = std::floor(y0) + .5f;
    x1 = std::floor(x1) + .5f; y1 = std::floor(y1) + .5f;
  }
  tolerance = std::clamp(tolerance, 0, 255);
  const int left = static_cast<int>(
                std::clamp(std::floor(std::min(x0, x1) - radius), 0.f, float(width_))),
            right = static_cast<int>(
                std::clamp(std::ceil(std::max(x0, x1) + radius), 0.f, float(width_)));
  const int top = static_cast<int>(
                std::clamp(std::floor(std::min(y0, y1) - radius), 0.f, float(height_))),
            bottom = static_cast<int>(
                std::clamp(std::ceil(std::max(y0, y1) + radius), 0.f, float(height_)));
  double dx = double(x1) - x0, dy = double(y1) - y0, length = dx * dx + dy * dy;
  for (int y = top; y < bottom; y++)
    for (int x = left; x < right; x++) {
      auto i = static_cast<std::size_t>(y) * width_ + x;
      if (mask && !(*mask)[i]) continue;
      double t =
          length ? std::clamp(((x + .5 - x0) * dx + (y + .5 - y0) * dy) / length, 0., 1.) : 0.;
      double d = std::hypot(x + .5 - (x0 + t * dx), y + .5 - (y0 + t * dy)) / radius;
      if (d > 1) continue;
      double weight = opacity * (hardness >= 1 || d <= hardness ? 1 : (1 - d) / (1 - hardness));
      if (!clone) weight *= (replacement >> 24) / 255.;
      if (weight <= 0) continue;
      bool grouped = strokeBase_.size() == pixels_.size();
      if (grouped) {
        if (weight <= strokeCoverage_[i]) continue;
        strokeCoverage_[i] = float(weight);
      }
      auto old = grouped ? strokeBase_[i] : pixels_[i];
      std::uint32_t sample;
      if (clone) {
        auto sx = static_cast<std::int64_t>(x) + ox, sy = static_cast<std::int64_t>(y) + oy;
        if (sx < 0 || sy < 0 || sx >= width_ || sy >= height_) continue;
        sample = source[sy * width_ + sx];
      } else {
        sample = source[i];
        bool matches = true;
        for (int shift : {0, 8, 16})
          if (std::abs(int((sample >> shift) & 255) - int((target >> shift) & 255)) > tolerance)
            matches = false;
        if (!matches || !(sample >> 24)) continue;
        sample = (old & 0xff000000) | (replacement & 0xffffff);
      }
      double a = (sample >> 24) / 255. * weight, da = (old >> 24) / 255.,
             out = clone ? a + da * (1 - a) : da;
      if (out <= 0) {
        pixels_[i] = 0;
        continue;
      }
      std::uint32_t result = static_cast<std::uint32_t>(std::lround(out * 255)) << 24;
      for (int shift : {0, 8, 16}) {
        double v =
            clone ? (((sample >> shift) & 255) * a + ((old >> shift) & 255) * da * (1 - a)) / out
                  : ((sample >> shift) & 255) * weight + ((old >> shift) & 255) * (1 - weight);
        result |= std::uint32_t(std::clamp(std::lround(v), 0L, 255L)) << shift;
      }
      pixels_[i] = result;
    }
}
void Canvas::strokeStyled(float x0, float y0, float x1, float y1, float radius, std::uint32_t color,
                          float opacity, float hardness, bool square, bool eraser,
                          const std::vector<std::uint8_t>* mask) {
  if (!std::isfinite(x0) || !std::isfinite(y0) || !std::isfinite(x1) || !std::isfinite(y1) ||
      !std::isfinite(radius) || !std::isfinite(opacity) || !std::isfinite(hardness) ||
      radius <= 0 || opacity <= 0)
    return;
  if (mask && mask->size() != pixels_.size()) return;
  radius = std::min(radius, 2048.f);
  opacity = std::clamp(opacity, 0.f, 1.f);
  hardness = std::clamp(hardness, 0.f, 1.f);
  if (radius <= .5f) {
    x0 = std::floor(x0) + .5f; y0 = std::floor(y0) + .5f;
    x1 = std::floor(x1) + .5f; y1 = std::floor(y1) + .5f;
  }
  double dx = double(x1) - x0, dy = double(y1) - y0, length2 = dx * dx + dy * dy;
  double l = std::max(0., std::floor(std::min(double(x0), double(x1)) - radius));
  double r = std::min(double(width_ - 1), std::ceil(std::max(double(x0), double(x1)) + radius));
  double t = std::max(0., std::floor(std::min(double(y0), double(y1)) - radius));
  double b = std::min(double(height_ - 1), std::ceil(std::max(double(y0), double(y1)) + radius));
  if (l > r || t > b) return;
  for (int y = int(t); y <= int(b); ++y)
    for (int x = int(l); x <= int(r); ++x) {
      if (mask && !(*mask)[std::size_t(y) * width_ + x]) continue;
      double projection =
          length2 == 0 ? 0
                       : std::clamp(((x + .5 - x0) * dx + (y + .5 - y0) * dy) / length2, 0., 1.);
      double px = x + .5 - (x0 + projection * dx), py = y + .5 - (y0 + projection * dy);
      double distance = square ? std::max(std::abs(px), std::abs(py)) : std::hypot(px, py);
      if (distance > radius) continue;
      double coverage = opacity * (hardness >= 1 || distance <= radius * hardness
                                       ? 1
                                       : (radius - distance) / (radius * (1 - hardness)));
      auto i = std::size_t(y) * width_ + x;
      bool grouped = strokeBase_.size() == pixels_.size();
      if (grouped) {
        if (coverage <= strokeCoverage_[i]) continue;
        strokeCoverage_[i] = float(coverage);
      }
      auto& dst = pixels_[i];
      auto old = grouped ? strokeBase_[i] : dst;
      double da = (old >> 24) / 255.;
      if (eraser) {
        auto alpha = std::uint32_t(std::lround(255 * da * (1 - coverage)));
        dst = alpha ? (old & 0x00ffffff) | (alpha << 24) : 0;
        continue;
      }
      double sa = (color >> 24) / 255. * coverage, oa = sa + da * (1 - sa);
      if (oa <= 0) continue;
      std::uint32_t result = std::uint32_t(std::lround(oa * 255)) << 24;
      for (int shift : {0, 8, 16})
        result |=
            std::uint32_t(std::clamp(
                std::lround(
                    (((color >> shift) & 255) * sa + ((old >> shift) & 255) * da * (1 - sa)) / oa),
                0L, 255L))
            << shift;
      dst = result;
    }
}
}  // namespace velyntora
