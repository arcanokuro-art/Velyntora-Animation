#include "velyntora/Canvas.hpp"
#include <cassert>
#include <stdexcept>
#include <limits>
int main(){
 velyntora::Canvas c(32,32);
 assert(c.pixels().size()==1024);
 assert(c.pixels()[0]==0xFFFFFFFFu);
 c.stroke(10,10,10,10,3,0xFF000000u);
 assert(c.pixels()[10*32+10]==0xFF000000u);
 c.clear(0xFFFFFFFFu);
 assert(c.pixels()[10*32+10]==0xFFFFFFFFu);
 c.rectangle(2,2,12,12,0xFFFF0000u,false);
 assert(c.pixels()[2*32+2]==0xFFFF0000u);
 assert(c.pixels()[5*32+5]==0xFFFFFFFFu);
 c.fill(5,5,0xFF00FF00u);
 assert(c.pixels()[5*32+5]==0xFF00FF00u);
 assert(c.pixels()[0]==0xFFFFFFFFu);
 c.ellipse(15,15,25,25,0xFF0000FFu,true);
 assert(c.pixels()[20*32+20]==0xFF0000FFu);
 std::vector<std::uint32_t> snapshot=c.pixels();
 c.clear(0xFFFFFFFFu);c.setPixels(snapshot);
 assert(c.pixels()[20*32+20]==0xFF0000FFu);
 c.stroke(1.0e30f,1.0e30f,1.0e30f,1.0e30f,2,0xFF000000u);
 c.stroke(-1.0e30f,-1.0e30f,-1.0e30f,-1.0e30f,2,0xFF000000u);
 c.stroke(0,0,std::numeric_limits<float>::max(),0,2,0xFF000000u);
 assert(c.pixels()[20*32+20]==0xFF0000FFu);
 // A full 800x800 canvas must fill without unbounded DFS growth.
 velyntora::Canvas large(800,800);
 large.rectangle(200,200,599,599,0xFF000000u,false);
 large.fill(400,400,0xFFAA5500u);
 assert(large.pixels()[400*800+400]==0xFFAA5500u);
 assert(large.pixels()[200*800+200]==0xFF000000u);
 assert(large.pixels()[0]==0xFFFFFFFFu);
 large.fill(0,0,0xFF00AAFFu);
 assert(large.pixels()[0]==0xFF00AAFFu);
 assert(large.pixels()[400*800+400]==0xFFAA5500u);
 velyntora::Canvas masked(5,2);std::vector<std::uint8_t> mask={1,1,0,1,1,1,1,0,1,1};masked.fill(0,0,0xff123456,&mask);
 assert(masked.pixels()[0]==0xff123456&&masked.pixels()[6]==0xff123456&&masked.pixels()[3]==0xffffffff&&masked.pixels()[2]==0xffffffff);
 auto untouched=masked.pixels();masked.fill(2,0,0,&mask);assert(masked.pixels()==untouched);mask.pop_back();masked.fill(0,0,0,&mask);assert(masked.pixels()==untouched);
 bool failed=false;try{velyntora::Canvas bad(0,1);}catch(const std::invalid_argument&){failed=true;}assert(failed);
}
