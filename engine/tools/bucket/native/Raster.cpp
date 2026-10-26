#include <algorithm>
#include <cmath>
#include <stdexcept>
#include <utility>

#include "velyntora/Canvas.hpp"
namespace velyntora {
void Canvas::fill(int x, int y, std::uint32_t color, const std::vector<std::uint8_t>* mask) {
  if (x < 0 || y < 0 || x >= width_ || y >= height_ ||
      (mask &&
       (mask->size() != pixels_.size() || !(*mask)[static_cast<std::size_t>(y) * width_ + x])))
    return;
  const std::uint32_t target = pixels_[static_cast<std::size_t>(y) * width_ + x];
  if (target == color) return;
  // Scanline flood fill: each seed expands to a horizontal run, avoiding
  // the four-neighbor push explosion of a pixel-by-pixel DFS.
  std::vector<std::pair<int, int>> pending{{x, y}};
  while (!pending.empty()) {
    auto [sx, sy] = pending.back();
    pending.pop_back();
    if (sx < 0 || sx >= width_ || sy < 0 || sy >= height_) continue;
    const std::size_t row = static_cast<std::size_t>(sy) * width_;
    if (pixels_[row + sx] != target || (mask && !(*mask)[row + sx])) continue;
    int left = sx, right = sx;
    while (left > 0 && pixels_[row + left - 1] == target && (!mask || (*mask)[row + left - 1]))
      --left;
    while (right + 1 < width_ && pixels_[row + right + 1] == target &&
           (!mask || (*mask)[row + right + 1]))
      ++right;
    for (int px = left; px <= right; ++px) pixels_[row + px] = color;
    for (int ny : {sy - 1, sy + 1}) {
      if (ny < 0 || ny >= height_) continue;
      const std::size_t adjacent = static_cast<std::size_t>(ny) * width_;
      bool inRun = false;
      for (int px = left; px <= right; ++px) {
        const bool matches = pixels_[adjacent + px] == target && (!mask || (*mask)[adjacent + px]);
        if (matches && !inRun) {
          pending.emplace_back(px, ny);
          inRun = true;
        } else if (!matches)
          inRun = false;
      }
    }
  }
}
}  // namespace velyntora
