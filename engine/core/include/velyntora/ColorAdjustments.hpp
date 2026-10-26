#include "../../document/tiles/LayerPixels.hpp"
#pragma once
#include <array>
#include <cstdint>
#include <vector>
namespace velyntora {
tiles::LayerPixels brightnessContrast(const tiles::LayerPixels& pixels,
                                              int brightness, int contrast);
tiles::LayerPixels basicColor(const tiles::LayerPixels& pixels, int kind);
struct ChannelLevels {
  int inputBlack = 0, inputWhite = 255, gamma = 100, outputBlack = 0, outputWhite = 255;
};
tiles::LayerPixels applyCurve(const tiles::LayerPixels& pixels,
                                      const std::array<int, 256>& curve, int channel);
tiles::LayerPixels applyLevels(const tiles::LayerPixels& pixels,
                                       const std::array<ChannelLevels, 3>& levels);
tiles::LayerPixels autoLevels(const tiles::LayerPixels& pixels, int clipPermille,
                                      const std::vector<std::uint8_t>* selection = nullptr);
tiles::LayerPixels posterizeRgb(const tiles::LayerPixels& pixels,
                                        const std::array<int, 3>& levels);
tiles::LayerPixels hueSaturation(const tiles::LayerPixels& pixels, int hue,
                                         int saturation, int lightness);
}  // namespace velyntora
