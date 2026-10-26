#include "../../document/tiles/LayerPixels.hpp"
#pragma once
#include <cstdint>
#include <vector>
namespace velyntora {
// Oil paint, pencil, ink, glow, soft portrait, median, red-eye reduction.
tiles::LayerPixels artisticEffect(const tiles::LayerPixels& pixels, int width,
                                          int height, int kind, int strength, int radius,
                                          int threshold);
}  // namespace velyntora
