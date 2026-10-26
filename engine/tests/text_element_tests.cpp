#include "velyntora/ProjectIO.hpp"
#include "../core/document/history/HistoryArchive.hpp"
#include <cassert>
#include <cstring>
#include <iostream>
using namespace velyntora;
int main(){
  LayerDocument doc(8,8);doc.addLayer("Texto");auto image=std::make_shared<images::Element>();image->transform={0,0,8,8,4,4,8,8,27};unsigned char b=1;image->source=std::make_shared<images::Source>(1,[&](void* p,std::size_t n){std::memcpy(p,&b,n);});doc.setActiveImage(image);
  auto text=std::make_shared<text::Element>();text->content="hola 日本語 😀";text->style={32,std::int32_t(0xff123456),2,3,400};doc.setActiveText(text);
  history::HistoryArchive history("/tmp");auto saved=history.capture(doc);doc.duplicateActive();assert(doc.layer(2).text==text);doc.moveLayer(2,0);assert(doc.layer(0).text==text);
  std::vector<unsigned char> bytes;writeProject(doc,[&](const void* p,std::size_t n){auto b=static_cast<const unsigned char*>(p);bytes.insert(bytes.end(),b,b+n);});assert(bytes[7]==4);std::size_t pos=0;
  auto restored=readProject([&](void* p,std::size_t n){if(n>bytes.size()-pos)throw std::runtime_error("EOF");std::memcpy(p,bytes.data()+pos,n);pos+=n;});assert(restored.layer(0).text->content==text->content);assert(restored.layer(2).text->style==text->style);
  doc=history.restore(saved);assert(doc.layer(1).text->content==text->content);doc.setOpacity(1,.5);doc.setBlendMode(1,4);assert(doc.layer(1).text);
  std::uint32_t pixel=1;doc.writeActiveSpan(0,1,&pixel);assert(!doc.layer(1).image && !doc.layer(1).text);doc=history.restore(saved);doc.setActiveImage(nullptr);assert(!doc.layer(1).text);doc=history.restore(saved);doc.mergeDown();assert(!doc.layer(0).text);
  bool rejected=false;auto invalid=std::make_shared<text::Element>(*text);invalid->style[0]=0;try{doc.setActiveText(invalid);}catch(...){rejected=true;}assert(rejected);
  std::cout<<"Unicode text persistence, history, duplicate and rasterization passed\n";
}
