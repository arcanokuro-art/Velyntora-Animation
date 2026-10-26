#pragma once
#include <array>
#include <vector>
#include <cstdint>
#include "velyntora/Canvas.hpp"
namespace velyntora {
class TipBrushEngine {
 public:
  std::vector<std::uint8_t> tips,texture;
  std::array<float,13> parameters{};
  std::array<std::vector<float>,3> curves;
  float remaining=0;
  bool started=false,hasCarry=false;
  std::uint32_t carry=0;
  unsigned dabIndex=0;
  void stroke(Canvas&,float,float,float,float,float,std::uint32_t,float,float,const std::vector<std::uint8_t>*);
 private:
  float curve(int,float) const;
  void dab(Canvas&,float,float,float,std::uint32_t,float,float,const std::vector<std::uint8_t>*);
};
}
