#include "../../document/tiles/LayerPixels.hpp"
#pragma once
#include <cstdint>
#include <vector>
namespace velyntora {
// Align, feather alpha inward, outline behind transparent objects.
tiles::LayerPixels objectEffect(const tiles::LayerPixels& pixels, int width,
                                        int height, int kind, int amount, int tolerance,
                                        std::uint32_t color, bool edgeOrSoft,
                                        const std::vector<std::uint8_t>* selection = nullptr);
}  // namespace velyntora
