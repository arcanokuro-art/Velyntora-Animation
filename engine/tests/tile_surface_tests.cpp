#include "../core/document/tiles/TileSurface.hpp"
#include <cassert>
#include <iostream>
#include <stdexcept>

using velyntora::tiles::TileSurface;
int main() {
  TileSurface surface(6000,6000,0xFFFFFFFFu,256*256*4);
  assert(surface.allocatedTiles()==0 && surface.cachedBytes()==0);
  assert(surface.readRegion(5999,5999,1,1)[0]==0xFFFFFFFFu);
  auto initial = surface;
  // A continuous edit across four tile boundaries, with a one-tile cache.
  TileSurface::Pixels edit(20*20,0x80442211u);
  surface.writeRegion(250,250,20,20,edit);
  assert(surface.allocatedTiles()==4);
  assert(surface.readRegion(250,250,20,20)==edit);
  assert(surface.cachedBytes()<=256*256*4);
  assert(initial.readRegion(250,250,20,20)==TileSurface::Pixels(400,0xFFFFFFFFu));
  auto history = surface;
  surface.writeRegion(255,255,1,1,{0xFF123456u});
  assert(history.readRegion(255,255,1,1)[0]==0x80442211u);
  assert(surface.readRegion(255,255,1,1)[0]==0xFF123456u);
  surface = history; // undo
  assert(surface.readRegion(255,255,1,1)[0]==0x80442211u);
  surface.writeRegion(5999,5999,1,1,{0xFFABCDEFu});
  surface.compact();
  assert(history.readRegion(255,255,1,1)[0]==0x80442211u);
  assert(surface.readRegion(5999,5999,1,1)[0]==0xFFABCDEFu);
  surface.writeRegion(250,250,20,20,TileSurface::Pixels(400,0xFFFFFFFFu));
  assert(surface.allocatedTiles()==1);
  bool invalid=false;
  try { surface.writeRegion(5999,5999,2,1,{1,2}); }
  catch (const std::invalid_argument&) { invalid=true; }
  assert(invalid && surface.readRegion(5999,5999,1,1)[0]==0xFFABCDEFu);
  invalid=false;
  try { surface.readRegion(0,0,6000,6000); }
  catch (const std::invalid_argument&) { invalid=true; }
  assert(invalid);
  // Stream a real 24.2M-pixel extent; disk-backed cache stays bounded.
  TileSurface image(4031,6000,0,1024*1024);
  for(int y=0;y<6000;y+=256) {
    int rows=std::min(256,6000-y);
    // The public region cap requires narrower strips at this width.
    for(int x=0;x<4031;x+=2048) {
      int cols=std::min(2048,4031-x);
      TileSurface::Pixels pixels(std::size_t(cols)*rows);
      for(int row=0;row<rows;++row)
        for(int col=0;col<cols;++col)
          pixels[std::size_t(row)*cols+col]=0xFF000000u|((y+row)<<12)|(x+col);
      image.writeRegion(x,y,cols,rows,pixels);
      assert(image.readRegion(x,y,cols,rows)==pixels);
      assert(image.cachedBytes()<=1024*1024);
    }
  }
  assert(image.readRegion(4030,5999,1,1)[0]==(0xFF000000u|(5999<<12)|4030));
  image.clear(0);
  assert(image.allocatedTiles()==0 && image.readRegion(4030,5999,1,1)[0]==0);
  invalid=false;
  try { TileSurface tooLarge(6001,6000); }
  catch (const std::invalid_argument&) { invalid=true; }
  assert(invalid);
  std::cout << "Tiled canvas storage tests passed\n";
}
