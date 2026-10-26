#pragma once
#include <cstddef>
#include "velyntora/CanvasLimits.hpp"
#include <functional>

#include "velyntora/LayerDocument.hpp"
namespace velyntora {
// Callbacks must transfer exactly size bytes or throw. No whole-file buffer.
using ProjectRead = std::function<void(void*, std::size_t)>;
using ProjectWrite = std::function<void(const void*, std::size_t)>;
void writeProject(const LayerDocument& document, const ProjectWrite& write);
LayerDocument readProject(const ProjectRead& read, int requiredWidth = 0, int requiredHeight = 0,
                          std::uint64_t maxDocumentPixels = limits::maxDocumentPixels);
}  // namespace velyntora
