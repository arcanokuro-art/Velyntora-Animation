#include "velyntora/LayerDocument.hpp"
#include <cassert>
#include <cstdint>
#include <vector>
int main(){
 velyntora::LayerDocument doc(8,8);
 doc.addLayer("Superior");
 std::vector<std::uint32_t> pixels(64,0u);
 pixels[0]=0xFFFF0000u;
 doc.replaceActivePixels(pixels);
 auto snapshot=doc;
 doc.addLayer("Nueva");
 assert(doc.layerCount()==3);
 doc=snapshot;
 assert(doc.layerCount()==2);
 assert(doc.activeIndex()==1);
 assert(doc.flatten()[0]==0xFFFF0000u);
 doc.setVisible(1,false);
 auto hidden=doc;
 doc.setVisible(1,true);
 assert(doc.flatten()[0]==0xFFFF0000u);
 doc=hidden;
 assert(doc.flatten()[0]==0xFFFFFFFFu);
}
