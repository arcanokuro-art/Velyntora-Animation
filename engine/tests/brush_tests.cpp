#include "velyntora/Canvas.hpp"
#include <cassert>
#include <limits>
using namespace velyntora;
int main(){
 Canvas c(32,32);c.clear(0);
 c.strokeStyled(10.5,10.5,10.5,10.5,4,0xffff0000,.5,1,false,false);
 assert(c.pixels()[10*32+10]==0x80ff0000);
 c.strokeStyled(10.5,10.5,10.5,10.5,4,0xff0000ff,.5,1,false,false);
 auto mixed=c.pixels()[10*32+10];assert((mixed>>24)==192);assert(((mixed>>16)&255)>=84&&((mixed>>16)&255)<=86);assert((mixed&255)>=169&&(mixed&255)<=171);
 c.strokeStyled(10.5,10.5,10.5,10.5,4,0,.5,1,false,true);assert((c.pixels()[10*32+10]>>24)==96);
 c.strokeStyled(10.5,10.5,10.5,10.5,4,0,1,1,false,true);assert(c.pixels()[10*32+10]==0);
 c.clear(0);c.strokeStyled(10.5,10.5,10.5,10.5,4,0xffffffff,1,0,false,false);
 assert((c.pixels()[10*32+10]>>24)==255);assert((c.pixels()[10*32+13]>>24)==64);
 c.clear(0);c.strokeStyled(10.5,10.5,10.5,10.5,4,0xffffffff,1,1,true,false);assert(c.pixels()[14*32+14]==0xffffffff);
 c.clear(0);std::vector<std::uint8_t> mask(32*32,0);mask[10*32+10]=1;
 c.strokeStyled(10.5,10.5,10.5,10.5,4,0xffffffff,1,1,false,false,&mask);
 assert(c.pixels()[10*32+10]==0xffffffff&&c.pixels()[10*32+11]==0);
 auto before=c.pixels();c.strokeStyled(std::numeric_limits<float>::quiet_NaN(),0,0,0,4,0,1,1,false,false);assert(c.pixels()==before);
 c.clear(0);c.strokeStyled(1.5,1.5,25.5,25.5,2,0xff123456,1,1,false,false);assert(c.pixels()[15*32+15]==0xff123456);
}
