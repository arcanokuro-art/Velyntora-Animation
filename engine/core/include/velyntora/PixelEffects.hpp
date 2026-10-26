#include "../../document/tiles/LayerPixels.hpp"
#pragma once
#include <cstdint>
#include <vector>
namespace velyntora {
// 0 box blur, 1 sharpen, 2 edges, 3 emboss, 4 pixelate, 5 noise, 6 vignette.
tiles::LayerPixels pixelEffect(const tiles::LayerPixels& source, int width,
                                       int height, int kind, int amount);
}  // namespace velyntora
