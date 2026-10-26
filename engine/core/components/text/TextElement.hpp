#pragma once
#include <array>
#include <cstdint>
#include <memory>
#include <string>
namespace velyntora::text {
struct Element {
  std::string content; // UTF-8, independent from the raster glyph cache.
  // Font size px, ARGB color, family (sans/serif/mono), style bits, wrapping width.
  std::array<std::int32_t,5> style;
  bool valid()const{return !content.empty() && content.size()<=16384 && content.find('\0')==std::string::npos && style[0]>=4 && style[0]<=6000 && style[2]>=0 && style[2]<=2 && style[3]>=0 && style[3]<=3 && style[4]>=8 && style[4]<=6000;}
};
}
