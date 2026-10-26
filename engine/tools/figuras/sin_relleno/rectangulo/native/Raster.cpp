#include <algorithm>
#include <cmath>
#include <stdexcept>
#include <utility>

#include "velyntora/Canvas.hpp"
namespace velyntora {
void Canvas::rectangle(int x0, int y0, int x1, int y1, std::uint32_t color, bool filled) {
  int left = std::max(0, std::min(x0, x1)), right = std::min(width_ - 1, std::max(x0, x1));
  int top = std::max(0, std::min(y0, y1)), bottom = std::min(height_ - 1, std::max(y0, y1));
  if (left > right || top > bottom) return;
  for (int y = top; y <= bottom; ++y)
    for (int x = left; x <= right; ++x)
      if (filled || y == top || y == bottom || x == left || x == right) pixel(x, y, color);
}
}  // namespace velyntora
