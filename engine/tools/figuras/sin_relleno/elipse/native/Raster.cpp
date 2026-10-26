#include <algorithm>
#include <cmath>
#include <stdexcept>
#include <utility>

#include "velyntora/Canvas.hpp"
namespace velyntora {
void Canvas::ellipse(int x0, int y0, int x1, int y1, std::uint32_t color, bool filled) {
  int left = std::max(0, std::min(x0, x1)), right = std::min(width_ - 1, std::max(x0, x1));
  int top = std::max(0, std::min(y0, y1)), bottom = std::min(height_ - 1, std::max(y0, y1));
  double cx = (static_cast<double>(x0) + x1) / 2., cy = (static_cast<double>(y0) + y1) / 2.;
  double rx = std::abs(static_cast<double>(x1) - x0) / 2.,
         ry = std::abs(static_cast<double>(y1) - y0) / 2.;
  if (rx < 0.5 || ry < 0.5) return;
  for (int y = top; y <= bottom; ++y)
    for (int x = left; x <= right; ++x) {
      double dx = (x - cx) / rx, dy = (y - cy) / ry, v = dx * dx + dy * dy;
      if (v <= 1. &&
          (filled || v >= std::pow(std::max(0., 1. - 1. / std::max(1., std::min(rx, ry))), 2)))
        pixel(x, y, color);
    }
}
}  // namespace velyntora
