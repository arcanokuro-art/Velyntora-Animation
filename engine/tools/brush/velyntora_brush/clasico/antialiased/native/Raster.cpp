#include <algorithm>
#include <cmath>
#include "velyntora/Canvas.hpp"
namespace velyntora {
void Canvas::strokeAntialiased(float x0, float y0, float x1, float y1, float radius, std::uint32_t color,
                          float opacity, float hardness, bool square,
                          const std::vector<std::uint8_t>* mask) {
  if (!std::isfinite(x0) || !std::isfinite(y0) || !std::isfinite(x1) || !std::isfinite(y1) ||
      !std::isfinite(radius) || !std::isfinite(opacity) || !std::isfinite(hardness) ||
      radius <= 0 || opacity <= 0)
    return;
  if (mask && mask->size() != pixels_.size()) return;
  radius = std::min(radius, 2048.f);
  opacity = std::clamp(opacity, 0.f, 1.f);
  hardness = std::clamp(hardness, 0.f, 1.f);
  double dx = double(x1) - x0, dy = double(y1) - y0, length2 = dx * dx + dy * dy;
  double l = std::max(0., std::floor(std::min(double(x0), double(x1)) - radius - 1));
  double r = std::min(double(width_ - 1), std::ceil(std::max(double(x0), double(x1)) + radius + 1));
  double t = std::max(0., std::floor(std::min(double(y0), double(y1)) - radius - 1));
  double b = std::min(double(height_ - 1), std::ceil(std::max(double(y0), double(y1)) + radius + 1));
  if (l > r || t > b) return;
  for (int y = int(t); y <= int(b); ++y)
    for (int x = int(l); x <= int(r); ++x) {
      if (mask && !(*mask)[std::size_t(y) * width_ + x]) continue;
      double projection =
          length2 == 0 ? 0
                       : std::clamp(((x + .5 - x0) * dx + (y + .5 - y0) * dy) / length2, 0., 1.);
      double px = x + .5 - (x0 + projection * dx), py = y + .5 - (y0 + projection * dy);
      double distance = square ? std::max(std::abs(px), std::abs(py)) : std::hypot(px, py);
      if (distance > radius + .707107) continue;
      double weight = 0;
      // Interior pixels need one sample. Only the one-pixel boundary uses 4x4 coverage.
      if (hardness >= 1 && distance < radius - .707107) weight = 1;
      else {
        for (int sy=0;sy<4;sy++) for (int sx=0;sx<4;sx++) {
          double ax=x+(sx+.5)/4., ay=y+(sy+.5)/4.;
          double u=length2==0?0:std::clamp(((ax-x0)*dx+(ay-y0)*dy)/length2,0.,1.);
          double vx=ax-(x0+u*dx), vy=ay-(y0+u*dy);
          double d=square?std::max(std::abs(vx),std::abs(vy)):std::hypot(vx,vy);
          if(d<=radius) weight += hardness>=1 || d<=radius*hardness?1:(radius-d)/(radius*(1-hardness));
        }
        weight /= 16;
      }
      double coverage = opacity * weight;
      if (coverage <= 0) continue;
      auto i = std::size_t(y) * width_ + x;
      bool grouped = strokeBase_.size() == pixels_.size();
      if (grouped) {
        if (coverage <= strokeCoverage_[i]) continue;
        strokeCoverage_[i] = float(coverage);
      }
      auto& dst = pixels_[i];
      auto old = grouped ? strokeBase_[i] : dst;
      double da = (old >> 24) / 255.;
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
