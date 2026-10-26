#include "../document/pages/PagedValues.hpp"
#include "../document/tiles/LayerPixels.hpp"
#include "velyntora/ObjectEffects.hpp"

#include <algorithm>
#include <cmath>
#include <limits>
#include <stdexcept>
namespace velyntora {
namespace {
void transform(const std::vector<float>& input, std::vector<float>& output, int n) {
  std::vector<int> positions(n);
  std::vector<double> boundaries(n + 1);
  int k = 0;
  positions[0] = 0;
  boundaries[0] = -std::numeric_limits<double>::infinity();
  boundaries[1] = std::numeric_limits<double>::infinity();
  for (int q = 1; q < n; q++) {
    double s;
    do {
      int p = positions[k];
      s = (double(input[q]) + double(q) * q - input[p] - double(p) * p) / (2 * (q - p));
      if (s <= boundaries[k])
        --k;
      else
        break;
    } while (k >= 0);
    ++k;
    positions[k] = q;
    boundaries[k] = s;
    boundaries[k + 1] = std::numeric_limits<double>::infinity();
  }
  k = 0;
  for (int q = 0; q < n; q++) {
    while (boundaries[k + 1] < q) ++k;
    double delta = q - positions[k];
    output[q] = float(delta * delta + input[positions[k]]);
  }
}
pages::PagedValues<float> distances(const tiles::LayerPixels& src, int w, int h, int threshold,
                             bool toOpaque) {
  pages::PagedValues<float> distance(src.size());
  for (std::size_t i = 0; i < src.size(); i++)
    distance[i] = ((int(src[i] >> 24) > threshold) == toOpaque) ? 0 : 1000000000.f;
  std::vector<float> input(std::max(w, h)), output(input.size());
  for (int y = 0; y < h; y++) {
    distance.readSpan(std::size_t(y)*w,w,input.data());
    transform(input, output, w);
    distance.writeSpan(std::size_t(y)*w,w,output.data());
  }
  for (int x = 0; x < w; x++) {
    for (int y = 0; y < h; y++) input[y] = distance[std::size_t(y) * w + x];
    transform(input, output, h);
    for (int y = 0; y < h; y++) distance[std::size_t(y) * w + x] = output[y];
  }
  return distance;
}
std::uint32_t over(std::uint32_t top, std::uint32_t bottom) {
  double a = (top >> 24) / 255., b = (bottom >> 24) / 255., alpha = a + b * (1 - a);
  if (alpha <= 0) return 0;
  std::uint32_t out = std::uint32_t(std::lround(alpha * 255)) << 24;
  for (int shift : {0, 8, 16})
    out |= std::uint32_t(std::clamp(
               int(std::lround(
                   (((top >> shift) & 255) * a + ((bottom >> shift) & 255) * b * (1 - a)) / alpha)),
               0, 255))
           << shift;
  return out;
}
}  // namespace
tiles::LayerPixels objectEffect(const tiles::LayerPixels& src, int w, int h,
                                        int kind, int amount, int tolerance, std::uint32_t color,
                                        bool option, const std::vector<std::uint8_t>* selection) {
  if (w < 1 || h < 1 || std::int64_t(w) * h != std::int64_t(src.size()) || kind < 0 || kind > 2 ||
      amount < 0 || amount > (kind == 0 ? 8 : 64) || tolerance < 0 || tolerance > 255 ||
      (selection && selection->size() != src.size()))
    throw std::invalid_argument("Invalid object parameters");
  auto out = src;
  if (kind == 0) {
    int left = w, top = h, right = -1, bottom = -1, sl = w, st = h, sr = -1, sb = -1;
    for (int y = 0; y < h; y++)
      for (int x = 0; x < w; x++) {
        auto pos = std::size_t(y) * w + x;
        if (selection && !(*selection)[pos]) continue;
        sl = std::min(sl, x);
        st = std::min(st, y);
        sr = std::max(sr, x);
        sb = std::max(sb, y);
        if (int(src[pos] >> 24) <= tolerance) continue;
        left = std::min(left, x);
        right = std::max(right, x);
        top = std::min(top, y);
        bottom = std::max(bottom, y);
      }
    if (right < left || bottom < top) return src;
    int bw = right - left + 1, bh = bottom - top + 1, horizontal = amount % 3,
        vertical = amount / 3,
        nx = horizontal == 0   ? sl
             : horizontal == 1 ? sl + (sr - sl + 1 - bw) / 2
                               : sr - bw + 1,
        ny = vertical == 0   ? st
             : vertical == 1 ? st + (sb - st + 1 - bh) / 2
                             : sb - bh + 1;
    if (nx == left && ny == top) return src;
    for (std::size_t i = 0; i < out.size(); i++)
      if (!selection || (*selection)[i]) out[i] = 0;
    for (int y = top; y <= bottom; y++)
      for (int x = left; x <= right; x++) {
        auto source = std::size_t(y) * w + x;
        if (selection && !(*selection)[source]) continue;
        int dx = nx + x - left, dy = ny + y - top;
        auto dest = std::size_t(dy) * w + dx;
        if (!selection || (*selection)[dest]) out[dest] = src[source];
      }
    return out;
  }
  if (amount == 0) return src;
  const auto distance = distances(src, w, h, tolerance, kind == 2);
  for (int y = 0; y < h; y++)
    for (int x = 0; x < w; x++) {
      auto pos = std::size_t(y) * w + x;
      if (selection && !(*selection)[pos]) continue;
      double d = std::sqrt(distance[pos]);
      if (kind == 1) {
        if (!(src[pos] >> 24)) continue;
        if (option) d = std::min(d, double(std::min({x, y, w - 1 - x, h - 1 - y})));
        int alpha = int(std::lround((src[pos] >> 24) * std::min(1., d / amount)));
        out[pos] = (src[pos] & 0x00ffffff) | (std::uint32_t(alpha) << 24);
      } else {
        if (d > amount) continue;
        double weight = option ? std::clamp(1 - d / (amount + 1.), 0., 1.) : 1.;
        int alpha = int(std::lround((color >> 24) * weight));
        auto stroke = (color & 0xffffff) | (std::uint32_t(alpha) << 24);
        out[pos] = over(src[pos], stroke);
      }
    }
  return out;
}
}  // namespace velyntora
