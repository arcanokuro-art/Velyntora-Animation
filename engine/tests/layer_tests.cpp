#include "velyntora/LayerDocument.hpp"
#include <cassert>
#include <vector>
int main(){
 velyntora::LayerDocument doc(4,4);
 assert(doc.layerCount()==1);
 assert(doc.flatten()[0]==0xFFFFFFFFu);
 doc.addLayer("Dibujo");
 assert(doc.layerCount()==2&&doc.activeIndex()==1);
 std::vector<std::uint32_t> pixels(16,0);
 pixels[0]=0xFFFF0000u;
 doc.replaceActivePixels(pixels);
 assert(doc.flatten()[0]==0xFFFF0000u);
 assert(doc.flatten()[1]==0xFFFFFFFFu);
 assert(doc.setOpacity(1,0.5f));
 assert(doc.flatten()[0]==0xFFFF7F7Fu||doc.flatten()[0]==0xFFFF8080u);
 assert(doc.setVisible(1,false));
 assert(doc.flatten()[0]==0xFFFFFFFFu);
 assert(doc.setVisible(1,true));
 assert(doc.moveLayer(1,0));
 assert(doc.activeIndex()==0);
 assert(doc.removeLayer(0));
 assert(doc.layerCount()==1);
 assert(!doc.removeLayer(0));
 velyntora::LayerDocument region(80,60);
 region.addLayer("Stroke");
 std::vector<std::uint32_t> source(80*60,0x804477AAu);
 region.replaceActiveRegion(source,12,17,8,9);
 assert(region.layer(1).pixels[0]==0);
 assert(region.layer(1).pixels[17*80+12]==0x804477AAu);
 region.setOpacity(1,.37f);
 auto full=region.flatten(), small=region.flattenRegion(10,15,12,13);
 for(int y=0;y<13;y++)for(int x=0;x<12;x++)
   assert(small[y*12+x]==full[(y+15)*80+x+10]);
 region.setVisible(1,false);
 small=region.flattenRegion(10,15,12,13);full=region.flatten();
 for(int y=0;y<13;y++)for(int x=0;x<12;x++)
   assert(small[y*12+x]==full[(y+15)*80+x+10]);
 bool rejected=false;try{region.flattenRegion(79,59,2,2);}catch(...){rejected=true;}
 assert(rejected);
}
