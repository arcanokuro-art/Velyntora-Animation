#include "../../document/tiles/LayerPixels.hpp"
#pragma once
#include <cstdint>
#include <vector>
namespace velyntora {
// Fragment, unfocus, dents, polar inversion, reduce noise, outline edges, relief, dithering.
tiles::LayerPixels utilityEffect(const tiles::LayerPixels& pixels, int width,
                                         int height, int kind, int amount, int size, int parameter);
}  // namespace velyntora
