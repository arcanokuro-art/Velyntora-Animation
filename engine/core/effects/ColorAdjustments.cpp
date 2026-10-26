#include "../document/tiles/LayerPixels.hpp"
#include "velyntora/ColorAdjustments.hpp"

#include <algorithm>
#include <cmath>
#include <stdexcept>
namespace velyntora {
namespace {
int clamp(int value) { return std::clamp(value, 0, 255); }
std::array<int, 256> levelsMap(const ChannelLevels& p) {
  if (p.inputBlack < 0 || p.inputWhite > 255 || p.inputBlack >= p.inputWhite || p.gamma < 10 ||
      p.gamma > 300 || p.outputBlack < 0 || p.outputWhite > 255 || p.outputBlack > p.outputWhite)
    throw std::invalid_argument("Invalid channel levels");
  std::array<int, 256> map{};
  for (int i = 0; i < 256; ++i) {
    double value = std::clamp((i - p.inputBlack) / double(p.inputWhite - p.inputBlack), 0., 1.);
    map[i] = clamp(int(std::lround(p.outputBlack + (p.outputWhite - p.outputBlack) *
                                                       std::pow(value, 100. / p.gamma))));
  }
  return map;
}
tiles::LayerPixels remap(const tiles::LayerPixels& pixels,
                                 const std::array<std::array<int, 256>, 3>& maps) {
  auto out = pixels;
  for (auto& pixel : out) {
    if (!(pixel >> 24)) continue;
    pixel = (pixel & 0xff000000) | std::uint32_t(maps[0][(pixel >> 16) & 255]) << 16 |
            std::uint32_t(maps[1][(pixel >> 8) & 255]) << 8 | std::uint32_t(maps[2][pixel & 255]);
  }
  return out;
}
}  // namespace
tiles::LayerPixels brightnessContrast(const tiles::LayerPixels& pixels,
                                              int brightness, int contrast) {
  if (brightness < -100 || brightness > 100 || contrast < -100 || contrast > 100)
    throw std::invalid_argument("Invalid brightness/contrast");
  std::array<int, 256> map{};
  double c = contrast * 2.55, factor = (259 * (c + 255)) / (255 * (259 - c));
  for (int i = 0; i < 256; ++i)
    map[i] = clamp(int(std::lround(factor * (i - 128) + 128 + brightness * 2.55)));
  return remap(pixels, {map, map, map});
}
tiles::LayerPixels basicColor(const tiles::LayerPixels& pixels, int kind) {
  if (kind < 0 || kind > 2) throw std::invalid_argument("Invalid basic adjustment");
  auto out = pixels;
  for (auto& p : out) {
    if (!(p >> 24)) continue;
    int r = (p >> 16) & 255, g = (p >> 8) & 255, b = p & 255;
    if (kind == 0) {
      r = g = b = (299 * r + 587 * g + 114 * b + 500) / 1000;
    } else if (kind == 1) {
      r = 255 - r;
      g = 255 - g;
      b = 255 - b;
    } else {
      int nr = clamp((393 * r + 769 * g + 189 * b + 500) / 1000),
          ng = clamp((349 * r + 686 * g + 168 * b + 500) / 1000),
          nb = clamp((272 * r + 534 * g + 131 * b + 500) / 1000);
      r = nr;
      g = ng;
      b = nb;
    }
    p = (p & 0xff000000) | (std::uint32_t(r) << 16) | (std::uint32_t(g) << 8) | std::uint32_t(b);
  }
  return out;
}
tiles::LayerPixels applyCurve(const tiles::LayerPixels& pixels,
                                      const std::array<int, 256>& curve, int channel) {
  if (channel < 0 || channel > 4) throw std::invalid_argument("Invalid curve channel");
  for (auto v : curve)
    if (v < 0 || v > 255) throw std::invalid_argument("Invalid curve value");
  auto out = pixels;
  for (auto& p : out) {
    if (!(p >> 24)) continue;
    int r = (p >> 16) & 255, g = (p >> 8) & 255, b = p & 255;
    if (channel == 0) {
      r = curve[r];
      g = curve[g];
      b = curve[b];
    } else if (channel == 1)
      r = curve[r];
    else if (channel == 2)
      g = curve[g];
    else if (channel == 3)
      b = curve[b];
    else {
      int old = (299 * r + 587 * g + 114 * b + 500) / 1000, value = curve[old];
      if (value == old) continue;
      if (old == 0)
        r = g = b = value;
      else {
        r = clamp((r * value + old / 2) / old);
        g = clamp((g * value + old / 2) / old);
        b = clamp((b * value + old / 2) / old);
      }
    }
    p = (p & 0xff000000) | (std::uint32_t(r) << 16) | (std::uint32_t(g) << 8) | std::uint32_t(b);
  }
  return out;
}
tiles::LayerPixels applyLevels(const tiles::LayerPixels& pixels,
                                       const std::array<ChannelLevels, 3>& levels) {
  return remap(pixels, {levelsMap(levels[0]), levelsMap(levels[1]), levelsMap(levels[2])});
}
tiles::LayerPixels autoLevels(const tiles::LayerPixels& pixels, int clip,
                                      const std::vector<std::uint8_t>* selection) {
  if (selection && selection->size() != pixels.size())
    throw std::invalid_argument("Invalid levels selection");
  if (clip < 0 || clip > 50) throw std::invalid_argument("Invalid histogram clipping");
  std::array<std::array<std::uint64_t, 256>, 3> histogram{};
  std::uint64_t count = 0;
  for (std::size_t i = 0; i < pixels.size(); i++) {
    auto p = pixels[i];
    if ((selection && !(*selection)[i]) || !(p >> 24)) continue;
    ++count;
    for (int c = 0; c < 3; ++c) ++histogram[c][(p >> (16 - c * 8)) & 255];
  }
  if (!count) return pixels;
  std::array<std::array<int, 256>, 3> maps{};
  for (int c = 0; c < 3; ++c) {
    std::uint64_t cut = count * clip / 1000, acc = 0;
    int low = 0, high = 255;
    for (; low < 255; ++low) {
      acc += histogram[c][low];
      if (acc > cut) break;
    }
    acc = 0;
    for (; high > 0; --high) {
      acc += histogram[c][high];
      if (acc > cut) break;
    }
    if (low >= high) {
      for (int i = 0; i < 256; ++i) maps[c][i] = i;
    } else
      maps[c] = levelsMap({low, high, 100, 0, 255});
  }
  return remap(pixels, maps);
}
tiles::LayerPixels posterizeRgb(const tiles::LayerPixels& pixels,
                                        const std::array<int, 3>& levels) {
  std::array<std::array<int, 256>, 3> maps{};
  for (int c = 0; c < 3; ++c) {
    int n = levels[c];
    if (n < 2 || n > 256) throw std::invalid_argument("Invalid posterization levels");
    for (int i = 0; i < 256; ++i) {
      int band = (i * (n - 1) + 127) / 255;
      maps[c][i] = (band * 255 + (n - 1) / 2) / (n - 1);
    }
  }
  return remap(pixels, maps);
}
tiles::LayerPixels hueSaturation(const tiles::LayerPixels& pixels, int hue,
                                         int saturation, int lightness) {
  if (hue < -180 || hue > 180 || saturation < 0 || saturation > 200 || lightness < -100 ||
      lightness > 100)
    throw std::invalid_argument("Invalid HSL parameters");
  if (hue == 0 && saturation == 100 && lightness == 0) return pixels;
  auto out = pixels;
  for (auto& p : out) {
    if (!(p >> 24)) continue;
    double r = ((p >> 16) & 255) / 255., g = ((p >> 8) & 255) / 255., b = (p & 255) / 255.;
    double low = std::min({r, g, b}), high = std::max({r, g, b}), delta = high - low,
           l = (high + low) / 2, s = 0, h = 0;
    if (delta > 0) {
      s = delta / (1 - std::abs(2 * l - 1));
      if (high == r)
        h = 60 * std::fmod((g - b) / delta, 6.);
      else if (high == g)
        h = 60 * ((b - r) / delta + 2);
      else
        h = 60 * ((r - g) / delta + 4);
    }
    h = std::fmod(h + hue + 720, 360);
    s = std::clamp(s * saturation / 100., 0., 1.);
    l = lightness >= 0 ? l + (1 - l) * lightness / 100. : l * (1 + lightness / 100.);
    double chroma = (1 - std::abs(2 * l - 1)) * s,
           x = chroma * (1 - std::abs(std::fmod(h / 60., 2) - 1)), m = l - chroma / 2;
    if (h < 60) {
      r = chroma;
      g = x;
      b = 0;
    } else if (h < 120) {
      r = x;
      g = chroma;
      b = 0;
    } else if (h < 180) {
      r = 0;
      g = chroma;
      b = x;
    } else if (h < 240) {
      r = 0;
      g = x;
      b = chroma;
    } else if (h < 300) {
      r = x;
      g = 0;
      b = chroma;
    } else {
      r = chroma;
      g = 0;
      b = x;
    }
    p = (p & 0xff000000) | (std::uint32_t(clamp(int(std::lround((r + m) * 255)))) << 16) |
        (std::uint32_t(clamp(int(std::lround((g + m) * 255)))) << 8) |
        std::uint32_t(clamp(int(std::lround((b + m) * 255))));
  }
  return out;
}
}  // namespace velyntora
