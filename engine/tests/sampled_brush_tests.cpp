#include "velyntora/Canvas.hpp"
#include <cassert>
#include <limits>
using velyntora::Canvas;
int main(){
 Canvas c(4,1);c.setPixels({0xffff0000,0xff00ff00,0,0});auto src=c.pixels();
 c.sampledStroke(src,true,-2,0,0,0,0,2.5,0.5,3.5,0.5,.5,1,1);
 assert(c.pixels()[2]==src[0]&&c.pixels()[3]==src[1]);
 // Frozen samples prevent feedback when a destination overlaps the source.
 c.setPixels({0xffff0000,0xff00ff00,0xff0000ff,0});src=c.pixels();
 c.sampledStroke(src,true,-1,0,0,0,0,1.5,.5,3.5,.5,.5,1,1);
 assert(c.pixels()[1]==src[0]&&c.pixels()[2]==src[1]&&c.pixels()[3]==src[2]);
 c.setPixels({0x80112233,0x80112234,0,0xffabcdef});src=c.pixels();std::vector<std::uint8_t> mask={1,0,1,1};
 c.sampledStroke(src,false,0,0,src[0],0xffff0000,1,.5,.5,3.5,.5,.5,1,1,&mask);
 assert(c.pixels()[0]==0x80ff0000&&c.pixels()[1]==src[1]&&c.pixels()[2]==0&&c.pixels()[3]==src[3]);
 c.setPixels(src);c.sampledStroke(src,false,0,0,src[0],0xff000000,0,.5,.5,.5,.5,.5,.5,1);
 assert(c.pixels()[0]==0x8009111a);
 auto unchanged=c.pixels();c.sampledStroke(src,true,0,0,0,0,0,std::numeric_limits<float>::quiet_NaN(),0,1,1,1,1,1);assert(c.pixels()==unchanged);
 c.sampledStroke(src,true,100,0,0,0,0,0,0,4,0,1,1,1);assert(c.pixels()==unchanged);
}
