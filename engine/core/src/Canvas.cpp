#include "velyntora/CanvasLimits.hpp"
#include "velyntora/Canvas.hpp"
#include "../document/tiles/LayerPixels.hpp"

#include <algorithm>
#include <cmath>
#include <stdexcept>
#include <utility>
namespace velyntora {
Canvas::Canvas(const Canvas& other)
    : width_(other.width_),height_(other.height_),pixels_(other.pixels_),
      strokeTiles_(other.strokeTiles_) {
  strokeTiles_.rebind(pixels_);
}
Canvas& Canvas::operator=(const Canvas& other) {
  if (this == &other) return *this;
  Canvas replacement(other);
  std::swap(width_,replacement.width_);std::swap(height_,replacement.height_);
  pixels_.swap(replacement.pixels_);std::swap(strokeTiles_,replacement.strokeTiles_);
  strokeTiles_.rebind(pixels_);replacement.strokeTiles_.rebind(replacement.pixels_);
  return *this;
}
Canvas::Canvas(int w, int h) : width_(w), height_(h) {
  if (!velyntora::limits::accepts(w,h))
    throw std::invalid_argument("Invalid canvas size");
  pixels_.resize(static_cast<std::size_t>(w) * h, 0xFFFFFFFFu);
}
void Canvas::beginOpacityStroke() {
  endOpacityStroke();
  strokeTiles_.begin(pixels_);
}
void Canvas::endOpacityStroke() {
  strokeTiles_.end();
}
void Canvas::clear(std::uint32_t color) {
  endOpacityStroke();
  pixels_.resize(pixels_.size(),color);
}
void Canvas::setPixels(const std::vector<std::uint32_t>& pixels) {
  if (pixels.size() != pixels_.size()) throw std::invalid_argument("Pixel count mismatch");
  endOpacityStroke();
  pixels_ = pixels;
}
void Canvas::setPixels(const tiles::LayerPixels& pixels) {
  if (pixels.size()!=pixels_.size()) throw std::invalid_argument("Pixel count mismatch");
  endOpacityStroke();pixels_=pixels;
}
void Canvas::pixel(int x, int y, std::uint32_t color) {
  if (x >= 0 && y >= 0 && x < width_ && y < height_)
    pixels_[static_cast<std::size_t>(y) * width_ + x] = color;
}
}  // namespace velyntora
