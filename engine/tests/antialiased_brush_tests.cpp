#include "velyntora/Canvas.hpp"
#include <cassert>
#include <limits>
using namespace velyntora;
int main() {
 Canvas c(400,400);c.clear(0);c.beginOpacityStroke();
 c.strokeAntialiased(100.2,100.4,140.2,126.4,5,0xffff0000,.5,1,false);
 auto first=c.pixels();int partial=0,max=0;
 for(auto p:first){int a=p>>24;max=std::max(max,a);if(a>0&&a<128)partial++;}
 assert(max==128&&partial>20);
 for(int i=0;i<20;i++)c.strokeAntialiased(100.2,100.4,140.2,126.4,5,0xffff0000,.5,1,false);
 assert(c.pixels()==first);c.endOpacityStroke();c.beginOpacityStroke();
 c.strokeAntialiased(100.2,100.4,140.2,126.4,5,0xffff0000,.5,1,false);
 assert((c.pixels()[110*400+115]>>24)==192);c.endOpacityStroke();
 c.clear(0);c.beginOpacityStroke();c.strokeAntialiased(200.5,200.5,200.5,200.5,150,0xff123456,1,1,false);
 assert(c.pixels()[200*400+200]==0xff123456);assert(c.pixels()[200*400+349]==0xff123456);assert(c.pixels()[200*400+351]==0);
 c.clear(0);c.beginOpacityStroke();c.strokeAntialiased(100,100,130,117,.5,0xff123456,1,1,false);
 partial=0;for(auto p:c.pixels())if((p>>24)>0&&(p>>24)<255)partial++;assert(partial>20);
 c.clear(0);std::vector<std::uint8_t> mask(400*400,0);mask[100*400+100]=1;
 c.beginOpacityStroke();c.strokeAntialiased(100.5,100.5,140,130,4,0xffabcdef,1,1,false,&mask);
 assert(c.pixels()[100*400+100]==0xffabcdef);int count=0;for(auto p:c.pixels())if(p)count++;assert(count==1);
 auto before=c.pixels();c.strokeAntialiased(100,100,140,130,4,0xffabcdef,0,1,false);assert(before==c.pixels());
 c.strokeAntialiased(std::numeric_limits<float>::quiet_NaN(),100,140,130,4,0xffabcdef,1,1,false);assert(before==c.pixels());
 c.clear(0);c.beginOpacityStroke();c.strokeAntialiased(100.5,100.5,100.5,100.5,4,0xffffffff,1,1,true);
 assert(c.pixels()[103*400+103]==0xffffffff);assert((c.pixels()[104*400+104]>>24)>0&&(c.pixels()[104*400+104]>>24)<255);
}
