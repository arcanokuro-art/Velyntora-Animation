#include "velyntora/Canvas.hpp"
#include "velyntora/LayerDocument.hpp"
#include "../core/document/tiles/StrokeTiles.hpp"
#include <array>
#include <cassert>
#include <iostream>
int main(){
 using namespace velyntora;
 LayerDocument document(3034,4515);
 assert(document.layer(0).pixels.allocatedPixels()==0);
 for(int i=0;i<31;++i)document.addLayer("Capa");
 for(int i=0;i<32;++i)assert(document.layer(i).pixels.allocatedPixels()==0);
 auto before=document;
 tiles::LayerPixels p(std::size_t(3034)*4515,0);
 p[123456]=0xff123456;
 auto original=p;
 p[123456]=0xffaabbcc;
 assert(original[123456]==0xff123456 && p[123456]==0xffaabbcc);
 assert(p.allocatedPixels()==4096 && original.allocatedPixels()==4096);
 std::vector<std::uint32_t> replacement=p;
 auto preserved=p;
 replacement[4095]=0xffabcdef;replacement[4096]=0xff778899;
 p=replacement;
 assert(preserved[4095]==0 && p[4095]==0xffabcdef && p[4096]==0xff778899);
 std::vector<std::uint32_t> span(4100);
 p.readSpan(4094,span.size(),span.data());
 for(std::size_t i=0;i<span.size();i++)assert(span[i]==replacement[4094+i]);
 replacement[4095]=replacement[4096]=0;p=replacement;
 assert(p.allocatedPixels()==4096 && p[123456]==0xffaabbcc);
 tiles::LayerPixels uniform(std::vector<std::uint32_t>(9000,0xffffffff));
 assert(uniform.allocatedPixels()==0);
 auto white=uniform;uniform=std::vector<std::uint32_t>(9000,0xffabcdef);
 assert(uniform.allocatedPixels()==0 && uniform[5000]==0xffabcdef && white[5000]==0xffffffff);
 Canvas canvas(800,800);canvas.clear(0);
 canvas.beginOpacityStroke();canvas.strokeStyled(255,255,265,260,3,0xff123456,.5,1,false,false);
 Canvas copied=canvas;
 copied.strokeStyled(255,255,265,260,3,0xffabcdef,.8,1,false,false);
 assert(canvas.pixels()!=copied.pixels());
 Canvas assigned(1,1);assigned=copied;
 assigned.strokeStyled(260,260,280,270,3,0xffffffff,.8,1,false,false);
 assert(assigned.pixels()!=copied.pixels());
 tiles::LayerPixels large(100000000,0xffffffff);
 tiles::StrokeTiles stroke;stroke.begin(large);
 assert(stroke.allocatedBlocks()==0);
 stroke.coverage(70000)=.5f;large[70000]=0;
 assert(stroke.base(70000)==0xffffffff && stroke.allocatedBlocks()==1);
 stroke.end();assert(stroke.size()==0 && stroke.allocatedBlocks()==0);
 // Stream a full 100 megapixel layer through the bounded page cache.
 tiles::LayerPixels streamed(100000000,0);
 std::array<std::uint32_t,4096> row{};
 for(std::size_t at=0;at<streamed.size();at+=row.size()){
   auto n=std::min(row.size(),streamed.size()-at);
   for(std::size_t j=0;j<n;++j)row[j]=0xff000000u|std::uint32_t(at+j);
   streamed.writeSpan(at,n,row.data());
   assert(pages::cache().bytes<=pages::PageCache::budget);
 }
 auto originalStreamed=streamed;
 streamed[0]=0xffabcdef;
 assert(static_cast<const tiles::LayerPixels&>(originalStreamed)[0]==0xff000000);
 for(std::size_t at=0;at<streamed.size();at+=123457)
   assert(static_cast<const tiles::LayerPixels&>(originalStreamed)[at]==(0xff000000u|std::uint32_t(at)));
 LayerDocument huge(6000,6000);huge.addLayer("Vacía");assert(huge.mergeDown());
 assert(huge.layer(0).pixels.allocatedPixels()==0);
 auto cropped=huge.cropped(0,0,5000,5000);assert(cropped.layer(0).pixels.allocatedPixels()==0);
 auto scaled=huge.resized(5500,5500,true,true,4);assert(scaled.layer(0).pixels.allocatedPixels()==0);
 std::cout<<"Sparse layers, COW snapshots, canvas copies and regional opacity passed\n";
}
