#include "../../document/tiles/LayerPixels.hpp"
#pragma once
#include <cstdint>
#include <vector>
namespace velyntora {
// Clouds, Voronoi, cells, Mandelbrot, Julia. Generated pixels replace selected layer pixels.
tiles::LayerPixels renderEffect(int width, int height, int kind, int scale, int detail,
                                        std::uint32_t seed, std::uint32_t first,
                                        std::uint32_t second);
}  // namespace velyntora
