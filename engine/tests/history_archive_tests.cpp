#include "../core/document/history/HistoryArchive.hpp"
#include <cassert>
#include <iostream>
#include <sys/stat.h>
using namespace velyntora;
int main(){
  history::HistoryArchive archive("/tmp");LayerDocument doc(512,512);
  std::vector<history::HistoryArchive::Entry> states;
  for(int step=0;step<150;++step){
    states.push_back(archive.capture(doc));
    auto pixels=std::vector<std::uint32_t>(doc.layer(0).pixels);
    pixels[step]=0xff000000u|step;doc.selectLayer(0);doc.replaceActivePixels(pixels);
  }
  auto latest=archive.capture(doc);
  for(int step=149;step>=0;--step){
    auto restored=archive.restore(states[step]);
    for(int i=0;i<150;++i)assert(restored.layer(0).pixels[i]==(i<step?(0xff000000u|i):0xffffffffu));
    doc=std::move(restored);
  }
  for(int step=0;step<150;++step)doc=archive.restore(states[step]);
  doc=archive.restore(latest);assert(doc.layer(0).pixels[149]==(0xff000000u|149));
  doc.addLayer("Transparent");doc.setOpacity(1,.45f);doc.setBlendMode(1,2);doc.setVisible(0,false);
  auto multi=archive.capture(doc);doc.removeLayer(0);doc=archive.restore(multi);
  assert(doc.layerCount()==2 && doc.activeIndex()==1 && doc.layer(1).name=="Transparent");
  assert(!doc.layer(0).visible && doc.layer(1).opacity==.45f && doc.layer(1).blendMode==2);
  // Capturing a block must not turn subsequent in-place writes into stale archive offsets.
  tiles::LayerPixels pixels(4096,0);pixels[5]=123;
  auto file=std::make_shared<history::HistoryFile>("/tmp");auto before=pixels.archive(file);
  pixels[5]=456;auto after=pixels.archive(file);file->flush();
  assert(tiles::LayerPixels::restore(before,file)[5]==123);
  assert(tiles::LayerPixels::restore(after,file)[5]==456);
  auto copied=pixels;copied[5]=789;auto copyState=copied.archive(file);
  assert(tiles::LayerPixels::restore(copyState,file)[5]==789);
  assert(tiles::LayerPixels::restore(after,file)[5]==456);
  std::cout<<"150 undo/redo snapshots, metadata and immutable disk blocks passed\n";
}
