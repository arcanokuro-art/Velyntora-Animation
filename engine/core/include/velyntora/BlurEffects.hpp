#include "../../document/tiles/LayerPixels.hpp"
#pragma once
#include <cstdint>
#include <vector>
namespace velyntora {
// kind: Gaussian, motion, radial, zoom. Centers expressed as canvas percentages.
tiles::LayerPixels blurEffect(const tiles::LayerPixels& pixels, int width,
                                      int height, int kind, int amount, int angle, int centerX,
                                      int centerY);
}  // namespace velyntora
