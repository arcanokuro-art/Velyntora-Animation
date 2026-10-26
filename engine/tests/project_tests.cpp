#include "velyntora/ProjectIO.hpp"
#include <cassert>
#include <cstring>
#include <stdexcept>
#include <vector>
using namespace velyntora;
std::vector<unsigned char> encode(const LayerDocument& d){
 std::vector<unsigned char> bytes;
 writeProject(d,[&](const void* p,std::size_t n){const auto* b=static_cast<const unsigned char*>(p);bytes.insert(bytes.end(),b,b+n);});return bytes;
}
LayerDocument decode(const std::vector<unsigned char>& bytes,int width=0,int height=0){
 std::size_t pos=0;
 return readProject([&](void* p,std::size_t n){if(n>bytes.size()-pos)throw std::runtime_error("EOF");std::memcpy(p,bytes.data()+pos,n);pos+=n;},width,height);
}
void rejected(const std::vector<unsigned char>& bytes){bool failed=false;try{decode(bytes);}catch(const std::exception&){failed=true;}assert(failed);}
int main(){
 LayerDocument d(5,3);d.renameLayer(0,"Fondo transparente");d.replaceActivePixels(std::vector<std::uint32_t>(15,0));
 d.addLayer("Trazo español 日本語");std::vector<std::uint32_t> pixels(15,0);pixels[0]=0x80123456;pixels[14]=0xffaabbcc;d.replaceActivePixels(pixels);
 d.setOpacity(1,.375f);d.addLayer("Oculta");d.setVisible(2,false);d.selectLayer(1);
 auto bytes=encode(d);auto restored=decode(bytes);
 assert(restored.width()==5&&restored.height()==3&&restored.activeIndex()==1&&restored.layerCount()==3);
 for(std::size_t i=0;i<3;++i){assert(restored.layer(i).name==d.layer(i).name);assert(restored.layer(i).visible==d.layer(i).visible);assert(restored.layer(i).opacity==d.layer(i).opacity);assert(restored.layer(i).pixels==d.layer(i).pixels);}
 assert(restored.flatten()==d.flatten());assert(encode(restored)==bytes);
 for(std::size_t n=0;n<bytes.size();++n)rejected({bytes.begin(),bytes.begin()+n});
 // Convert a v2 stream to the legacy v1 layout and verify compatibility.
 auto legacy=bytes;legacy[7]=1;std::size_t offset=24;
 for(std::size_t i=0;i<d.layerCount();i++){std::uint32_t size=0;for(int b=0;b<4;b++)size|=std::uint32_t(legacy[offset+b])<<(8*b);offset+=4+size+8;legacy.erase(legacy.begin()+offset,legacy.begin()+offset+4);offset+=d.layer(i).pixels.size()*4;}
 auto old=decode(legacy);assert(old.flatten()==d.flatten());for(std::size_t i=0;i<old.layerCount();i++)assert(old.layer(i).blendMode==0);
 auto invalid=bytes;invalid[7]=3;rejected(invalid);
 invalid=bytes;invalid[16]=0;rejected(invalid); // zero layers
 invalid=bytes;invalid[16]=33;rejected(invalid);
 invalid=bytes;invalid[20]=3;rejected(invalid); // active layer out of range
 invalid=bytes;invalid[8]=255;invalid[9]=255;invalid[10]=255;invalid[11]=127;rejected(invalid);
 invalid=bytes;invalid[24]=255;invalid[25]=255;rejected(invalid); // unbounded name
 bool failed=false;try{decode(bytes,800,800);}catch(const std::exception&){failed=true;}assert(failed);
 d.renameLayer(0,std::string(4097,'x'));failed=false;try{encode(d);}catch(const std::exception&){failed=true;}assert(failed);
}
