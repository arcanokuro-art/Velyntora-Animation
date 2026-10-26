#include "velyntora/Canvas.hpp"
#include <cassert>
#include <iostream>
using velyntora::Canvas;
int main() {
  Canvas c(64,32); c.clear(0);
  c.beginOpacityStroke();
  for(int i=0;i<20;i++) c.strokeStyled(8.5,16.5,40.5,16.5,4,0xffff0000,.5,1,false,false);
  assert(c.pixels()[16*64+24]==0x80ff0000);
  c.endOpacityStroke();c.beginOpacityStroke();
  c.strokeStyled(8.5,16.5,40.5,16.5,4,0xffff0000,.5,1,false,false);
  assert((c.pixels()[16*64+24]>>24)==192);
  c.endOpacityStroke();c.clear(0xff123456);c.beginOpacityStroke();
  for(int i=0;i<20;i++) c.strokeStyled(8.5,16.5,40.5,16.5,4,0xffffffff,.5,1,false,true);
  assert(c.pixels()[16*64+24]==0x80123456);
  c.endOpacityStroke();c.clear(0);c.beginOpacityStroke();
  c.strokeStyled(24.5,16.5,24.5,16.5,4,0x80ff0000,.5,1,false,false);
  assert(c.pixels()[16*64+24]==0x40ff0000);
  // Soft edge coverage can grow, but repeated events never increase its opacity.
  c.endOpacityStroke();c.clear(0);c.beginOpacityStroke();
  c.strokeStyled(24.5,16.5,24.5,16.5,8,0xff000000,.5,0,false,false);
  auto once=c.pixels();
  for(int i=0;i<10;i++) c.strokeStyled(24.5,16.5,24.5,16.5,8,0xff000000,.5,0,false,false);
  assert(c.pixels()==once);
  c.endOpacityStroke();c.clear(0xff0000ff);auto source=c.pixels();c.beginOpacityStroke();
  for(int i=0;i<20;i++) c.sampledStroke(source,false,0,0,0xff0000ff,0xffff0000,0,8.5,16.5,40.5,16.5,4,.5,1);
  assert(c.pixels()[16*64+24]==0xff800080);
  c.endOpacityStroke();c.clear(0);source.assign(64*32,0x80ff0000);c.beginOpacityStroke();
  for(int i=0;i<20;i++) c.sampledStroke(source,true,0,0,0,0,0,8.5,16.5,40.5,16.5,4,.5,1);
  assert(c.pixels()[16*64+24]==0x40ff0000);
  c.endOpacityStroke();c.clear(0);c.beginOpacityStroke();
  std::vector<std::uint8_t> mask(64*32,0);mask[16*64+24]=1;
  c.strokeStyled(8.5,16.5,40.5,16.5,4,0xff00ff00,.5,1,false,false,&mask);
  assert(c.pixels()[16*64+24]==0x8000ff00);assert(c.pixels()[16*64+25]==0);
  c.endOpacityStroke();auto before=c.pixels();c.beginOpacityStroke();
  c.strokeStyled(8.5,16.5,40.5,16.5,4,0xff000000,0,1,false,false);
  assert(c.pixels()==before);
  // Width is diameter: the new endpoints are radius .5 (1 px) and 150 (300 px).
  Canvas wide(400,400);wide.clear(0);
  wide.strokeStyled(200,200,200,200,.5,0xff000000,1,1,false,false);
  assert(wide.pixels()[200*400+200]==0xff000000);
  assert(wide.pixels()[200*400+201]==0);
  wide.clear(0);wide.strokeStyled(200.5,200.5,200.5,200.5,150,0xff000000,1,1,false,false);
  assert(wide.pixels()[200*400+349]==0xff000000);
  assert(wide.pixels()[200*400+351]==0);
  auto samples=wide.pixels();wide.clear(0);
  wide.sampledStroke(samples,true,0,0,0,0,0,200.5,200.5,200.5,200.5,150,1,1);
  assert(wide.pixels()[200*400+349]==0xff000000);
  std::cout<<"Tool opacity: coverage, separate gestures, alpha, eraser, sampled tools, selection and zero passed\n";
}
