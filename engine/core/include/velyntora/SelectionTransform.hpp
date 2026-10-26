#pragma once
#include <cstdint>
#include <vector>
#include "../../document/tiles/LayerPixels.hpp"
namespace velyntora {
tiles::LayerPixels transformSelection(const tiles::LayerPixels& source, int width,
                                              int height, int left, int top, int selectionWidth,
                                              int selectionHeight,
                                              const std::vector<std::uint8_t>& mask, double degrees,
                                              double scaleX, double scaleY);
}
