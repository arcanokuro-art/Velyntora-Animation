#include "../../document/tiles/LayerPixels.hpp"
#pragma once
#include <cstdint>
#include <vector>
namespace velyntora {
// Twist, bulge/pinch, radial ripple, tiled glass, frosted glass.
tiles::LayerPixels distortionEffect(const tiles::LayerPixels& source, int width,
                                            int height, int kind, int amount, int size, int angle,
                                            int centerX, int centerY);
}  // namespace velyntora
