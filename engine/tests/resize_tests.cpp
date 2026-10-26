#include "velyntora/LayerDocument.hpp"
#include <cassert>
#include <stdexcept>
#include <cmath>
using namespace velyntora;
int main(){
 LayerDocument d(3,2);d.renameLayer(0,"Base");d.replaceActivePixels({1,2,3,4,5,6});d.addLayer("Arriba");d.replaceActivePixels({7,8,9,10,11,12});d.setVisible(1,false);d.setOpacity(1,.5f);
 auto expanded=d.resized(5,4,false);assert(expanded.layerCount()==2&&expanded.activeIndex()==1);
 assert(expanded.layer(0).pixels[0]==1&&expanded.layer(0).pixels[5]==4&&expanded.layer(0).pixels[4]==0&&expanded.layer(0).pixels[19]==0);
 assert(!expanded.layer(1).visible&&expanded.layer(1).opacity==.5f&&expanded.layer(0).name=="Base");
 auto scaled=d.resized(6,4,true);assert(scaled.layer(0).pixels[0]==1&&scaled.layer(0).pixels[1]==1&&scaled.layer(0).pixels[2]==2&&scaled.layer(0).pixels[23]==6);
 auto crop=d.cropped(1,0,2,2);assert(crop.width()==2&&crop.height()==2&&crop.activeIndex()==1);assert((crop.layer(0).pixels==std::vector<std::uint32_t>{2,3,5,6}));
 for(int anchor=0;anchor<9;++anchor)for(auto size:std::vector<std::pair<int,int>>{{6,5},{2,1},{4,3}}){
  const int w=size.first,h=size.second,ox=int(std::floor((w-3.)*(anchor%3)/2)),oy=int(std::floor((h-2.)*(anchor/3)/2));auto anchored=d.resized(w,h,false,false,anchor);
  for(int y=0;y<h;++y)for(int x=0;x<w;++x){int sx=x-ox,sy=y-oy;auto expected=sx>=0&&sx<3&&sy>=0&&sy<2?d.layer(0).pixels[sy*3+sx]:0u;assert(anchored.layer(0).pixels[y*w+x]==expected);}
  assert(anchored.layerCount()==2&&anchored.activeIndex()==1&&anchored.layer(1).opacity==.5f&&!anchored.layer(1).visible);
 }
 LayerDocument colors(2,1);colors.replaceActivePixels({0xffff0000,0x000000ff});auto smooth=colors.resized(3,1,true,true);assert((smooth.layer(0).pixels==std::vector<std::uint32_t>{0xffff0000,0x80ff0000,0}));
 colors.replaceActivePixels({0xffff0000,0xff0000ff});smooth=colors.resized(1,1,true,true);assert(smooth.layer(0).pixels[0]==0xff800080);
 LayerDocument uniform(1,1);uniform.replaceActivePixels({0x40802010});smooth=uniform.resized(8,5,true,true);for(auto pixel:smooth.layer(0).pixels)assert(pixel==0x40802010);
 bool invalidAnchor=false;try{d.resized(2,2,false,false,9);}catch(const std::invalid_argument&){invalidAnchor=true;}assert(invalidAnchor);
 bool failed=false;try{d.cropped(2,0,2,1);}catch(const std::invalid_argument&){failed=true;}assert(failed);assert(d.width()==3&&d.layer(0).pixels[0]==1);
}
