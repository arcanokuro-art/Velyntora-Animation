#include "velyntora/BlurEffects.hpp"
#include <cassert>
#include <stdexcept>
using namespace velyntora;
int main(){
 std::vector<std::uint32_t> src(25,0x000000ff);src[12]=0xffff0000;
 auto gaussian=blurEffect(src,5,5,0,2,0,50,50);assert(gaussian[12]!=src[12]);assert(gaussian[11]==gaussian[13]&&gaussian[7]==gaussian[17]);assert(gaussian[12]>>24>gaussian[11]>>24);
 for(int kind=0;kind<4;kind++){auto pixels=blurEffect(src,5,5,kind,2,35,50,50);for(auto p:pixels)if(p>>24)assert((p&0xffffff)==0xff0000);assert(blurEffect(src,5,5,kind,0,0,50,50)==src);std::vector<std::uint32_t> solid(25,0x804080c0);assert(blurEffect(solid,5,5,kind,4,45,20,80)==solid);}
 auto horizontal=blurEffect(src,5,5,1,4,0,50,50);assert(horizontal[10]>>24&&horizontal[14]>>24);assert(!(horizontal[7]>>24));
 auto vertical=blurEffect(src,5,5,1,4,90,50,50);assert(vertical[2]>>24&&vertical[22]>>24);assert(!(vertical[11]>>24));
 bool rejected=false;try{blurEffect(src,5,5,0,33,0,50,50);}catch(const std::invalid_argument&){rejected=true;}assert(rejected);
}
