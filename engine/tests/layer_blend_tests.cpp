#include "velyntora/LayerDocument.hpp"
#include "velyntora/ProjectIO.hpp"
#include "../core/components/layers/blending/BlendModes.hpp"
#include <cassert>
#include <cstring>
#include <sstream>
#include <iostream>
using namespace velyntora;
int main(){
 using blending::composite;
 assert(composite(0xffff0000,0xff0000ff,1,0)==0xffff0000);
 assert(composite(0xffff0000,0xff0000ff,1,1)==0xff000000);
 assert(composite(0xffff0000,0xff0000ff,1,2)==0xff0000ff);
 assert(composite(0xffff0000,0xff0000ff,1,3)==0xff0000ff);
 assert(composite(0xffff0000,0xff0000ff,1,8)==0xffff00ff);
 for(int mode=0;mode<16;mode++){
  assert(composite(0x123456,0x87654321,.6,mode)==0x87654321);
  assert(composite(0x80abcdef,0,1,mode)==0x80abcdef);
  LayerDocument d(2,1);d.addLayer("Color");d.replaceActivePixels({0x80abcdef,0xffff0000});assert(d.setBlendMode(1,mode));d.setOpacity(1,.6);auto flat=d.flatten();assert(flat==d.flattenRegion(0,0,2,1));assert(d.resized(4,2,true).layer(1).blendMode==mode);assert(d.cropped(0,0,1,1).layer(1).blendMode==mode);
  std::stringstream stream;writeProject(d,[&](const void* p,std::size_t n){stream.write((const char*)p,n);});auto restored=readProject([&](void* p,std::size_t n){stream.read((char*)p,n);if(!stream)throw std::runtime_error("EOF");});assert(restored.layer(1).blendMode==mode);assert(restored.flatten()==flat);
  d.duplicateActive();assert(d.layer(2).blendMode==mode&&d.layer(2).pixels==d.layer(1).pixels);
  d.removeLayer(2);auto before=d.flatten();assert(d.mergeDown());assert(d.layerCount()==1);assert(d.flatten()==before);
 }
 LayerDocument d(1,1);assert(!d.setBlendMode(0,16));assert(!d.mergeDown());
 // portable vectors for parity testing, including all alpha cases and nonseparable modes
 for(int mode=0;mode<16;mode++)for(auto src:{0x80abcdefu,0xffff0000u,0x01234567u})for(auto dst:{0x87654321u,0xff0000ffu,0u})std::cout<<mode<<" "<<src<<" "<<dst<<" "<<composite(src,dst,.6f,mode)<<"\n";
}
