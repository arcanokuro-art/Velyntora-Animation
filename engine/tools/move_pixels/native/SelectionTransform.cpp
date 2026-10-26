#include "velyntora/SelectionTransform.hpp"

#include <algorithm>
#include <cmath>
#include <stdexcept>
namespace velyntora {
namespace {
std::uint32_t over(std::uint32_t src, std::uint32_t dst) {
  double sa = (src >> 24) / 255., da = (dst >> 24) / 255., oa = sa + da * (1 - sa);
  if (oa <= 0) return 0;
  std::uint32_t out = std::uint32_t(std::lround(oa * 255)) << 24;
  for (int shift : {0, 8, 16})
    out |= std::uint32_t(std::clamp(
               std::lround((((src >> shift) & 255) * sa + ((dst >> shift) & 255) * da * (1 - sa)) /
                           oa),
               0L, 255L))
           << shift;
  return out;
}
}  // namespace
tiles::LayerPixels transformSelection(const tiles::LayerPixels& src, int w, int h,
                                              int left, int top, int sw, int sh,
                                              const std::vector<std::uint8_t>& mask, double degrees,
                                              double sx, double sy) {
  if (w <= 0 || h <= 0 || std::int64_t(w) * h != std::int64_t(src.size()) || left < 0 || top < 0 ||
      sw <= 0 || sh <= 0 || std::int64_t(left) + sw > w || std::int64_t(top) + sh > h ||
      std::int64_t(sw) * sh != std::int64_t(mask.size()) || !std::isfinite(degrees) ||
      !std::isfinite(sx) || !std::isfinite(sy) || std::abs(sx) < .1 || std::abs(sy) < .1 ||
      std::abs(sx) > 4 || std::abs(sy) > 4)
    throw std::invalid_argument("Invalid selection transform");
  double r = degrees * 3.14159265358979323846 / 180, c = std::cos(r), s = std::sin(r),
         cx = left + sw / 2., cy = top + sh / 2.;
  auto out = src;
  for (int y = 0; y < sh; ++y)
    for (int x = 0; x < sw; ++x)
      if (mask[std::size_t(y) * sw + x]) out[std::size_t(y + top) * w + x + left] = 0;
  double extentX = std::abs(sw * sx * c) / 2 + std::abs(sh * sy * s) / 2,
         extentY = std::abs(sw * sx * s) / 2 + std::abs(sh * sy * c) / 2;
  int minX = int(std::max(0., std::floor(cx - extentX))),
      maxX = int(std::min(double(w), std::ceil(cx + extentX)));
  int minY = int(std::max(0., std::floor(cy - extentY))),
      maxY = int(std::min(double(h), std::ceil(cy + extentY)));
  for (int y = minY; y < maxY; ++y)
    for (int x = minX; x < maxX; ++x) {
      double dx = x + .5 - cx, dy = y + .5 - cy;
      double sourceX = (dx * c + dy * s) / sx + sw / 2.,
             sourceY = (-dx * s + dy * c) / sy + sh / 2.;
      if (sourceX < 0 || sourceY < 0 || sourceX >= sw || sourceY >= sh) continue;
      int px = int(std::floor(sourceX)), py = int(std::floor(sourceY));
      if (!mask[std::size_t(py) * sw + px]) continue;
      auto pixel = src[std::size_t(py + top) * w + px + left];
      if (!(pixel >> 24)) continue;
      auto& destination = out[std::size_t(y) * w + x];
      destination = over(pixel, destination);
    }
  return out;
}
}  // namespace velyntora
