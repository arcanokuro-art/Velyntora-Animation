#include <algorithm>
#include <cmath>
#include <stdexcept>
#include <utility>

#include "velyntora/Canvas.hpp"
namespace velyntora {
void Canvas::dab(float x, float y, float radius, std::uint32_t color) {
  if (!std::isfinite(x) || !std::isfinite(y) || !std::isfinite(radius) || radius <= 0) return;
  radius = std::min(radius, 2048.f);
  if (x + radius < 0.f || y + radius < 0.f || x - radius >= width_ || y - radius >= height_) return;
  int left = static_cast<int>(std::max(0.f, std::floor(x - radius)));
  int top = static_cast<int>(std::max(0.f, std::floor(y - radius)));
  int right = static_cast<int>(std::min(static_cast<float>(width_ - 1), std::ceil(x + radius)));
  int bottom = static_cast<int>(std::min(static_cast<float>(height_ - 1), std::ceil(y + radius)));
  for (int py = top; py <= bottom; ++py)
    for (int px = left; px <= right; ++px) {
      float dx = (px + 0.5f) - x, dy = (py + 0.5f) - y;
      if (dx * dx + dy * dy <= radius * radius) pixel(px, py, color);
    }
}
void Canvas::stroke(float x0, float y0, float x1, float y1, float radius, std::uint32_t color) {
  if (!std::isfinite(x0) || !std::isfinite(y0) || !std::isfinite(x1) || !std::isfinite(y1) ||
      !std::isfinite(radius) || radius <= 0)
    return;
  float dx = x1 - x0, dy = y1 - y0, dist = std::hypot(dx, dy), step = std::max(0.5f, radius * 0.5f);
  if (!std::isfinite(dist) || dist / step > 100000.f) return;
  int count = static_cast<int>(std::ceil(dist / step));
  for (int i = 0; i <= count; ++i) {
    float t = count == 0 ? 0.f : static_cast<float>(i) / count;
    dab(x0 + dx * t, y0 + dy * t, radius, color);
  }
}
}  // namespace velyntora
