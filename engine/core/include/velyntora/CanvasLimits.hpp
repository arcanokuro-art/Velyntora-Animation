#pragma once
#include <cstdint>
namespace velyntora::limits {
inline constexpr int maxSide=6000;
inline constexpr std::uint64_t maxDocumentPixels=36000000ULL;
inline bool accepts(int w,int h) {
  return w>0 && h>0 && w<=maxSide && h<=maxSide
      && std::uint64_t(w)*h<=maxDocumentPixels;
}
}
