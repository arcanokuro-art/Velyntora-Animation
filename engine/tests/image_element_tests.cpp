#include "velyntora/ProjectIO.hpp"
#include "../core/document/history/HistoryArchive.hpp"
#include <cassert>
#include <cstring>
#include <iostream>
using namespace velyntora;
int main(){
  std::vector<unsigned char> original(150000);for(std::size_t i=0;i<original.size();i++)original[i]=i%251;
  std::size_t pos=0;auto image=std::make_shared<images::Element>();image->transform={1,2,100,80,120,110,160,120,32};
  image->source=std::make_shared<images::Source>(original.size(),[&](void* p,std::size_t n){std::memcpy(p,original.data()+pos,n);pos+=n;});
  LayerDocument doc(256,256);doc.addLayer("Editable");doc.setActiveImage(image);doc.setOpacity(1,.4f);doc.setBlendMode(1,4);
  history::HistoryArchive archive("/tmp");auto before=archive.capture(doc);
  doc.duplicateActive();assert(doc.layer(2).image==image);doc.moveLayer(2,0);assert(doc.layer(0).image==image);
  std::vector<unsigned char> bytes;writeProject(doc,[&](const void* p,std::size_t n){auto b=static_cast<const unsigned char*>(p);bytes.insert(bytes.end(),b,b+n);});assert(bytes[7]==3);
  pos=0;auto restored=readProject([&](void* p,std::size_t n){assert(n<=bytes.size()-pos);std::memcpy(p,bytes.data()+pos,n);pos+=n;});assert(pos==bytes.size());
  for(int i:{0,2}){assert(restored.layer(i).image->transform==image->transform);assert(restored.layer(i).opacity==.4f);assert(restored.layer(i).blendMode==4);
    std::vector<unsigned char> source;restored.layer(i).image->source->write([&](const void* p,std::size_t n){auto b=static_cast<const unsigned char*>(p);source.insert(source.end(),b,b+n);});assert(source==original);}
  doc=archive.restore(before);assert(doc.layerCount()==2 && doc.layer(1).image==image);
  auto unchanged=doc.layer(1).pixels;doc.replaceActivePixels(unchanged);assert(doc.layer(1).image==image);
  std::uint32_t pixel=0xff010203;doc.writeActiveSpan(0,1,&pixel);assert(!doc.layer(1).image);doc=archive.restore(before);assert(doc.layer(1).image==image);
  auto changed=doc.layer(1).pixels;changed[100]=1;doc.replaceActivePixels(changed);assert(!doc.layer(1).image);
  doc=archive.restore(before);doc.mergeDown();assert(!doc.layer(0).image);
  doc=archive.restore(before);assert(!doc.resized(128,128,true).layer(1).image);assert(!doc.cropped(0,0,128,128).layer(1).image);
  bool rejected=false;auto invalid=std::make_shared<images::Element>(*image);invalid->transform[6]=-1;try{doc.setActiveImage(invalid);}catch(...){rejected=true;}assert(rejected);
  std::cout<<"Image source persistence, history, duplicate, reorder and rasterization passed\n";
}
