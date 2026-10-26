#include "../tools/brush/velyntora_brush/runtime/TipBrushEngine.hpp"
#include <cassert>
#include <fstream>
#include <sstream>
#include <limits>
#include <filesystem>
using namespace velyntora;
std::vector<std::uint8_t> bytes(const std::filesystem::path& p){std::ifstream s(p,std::ios::binary);assert(s);return {std::istreambuf_iterator<char>(s),{}};}
TipBrushEngine brush(){TipBrushEngine e;e.tips.assign(16384,255);e.parameters={.1f,0,1,1,0,0,0,0,0,1,0,0,1};return e;}
int main(int argc,char** argv){
 Canvas c(128,128);c.clear(0);auto e=brush();e.stroke(c,10,30,110,30,5,0xffff0000,.5f,1,nullptr);
 for(auto p:c.pixels())assert((p>>24)<=128);
 auto before=c.pixels();e.stroke(c,10,30,110,30,5,0xffff0000,0,1,nullptr);assert(before==c.pixels());
 e.stroke(c,std::numeric_limits<float>::quiet_NaN(),30,110,30,5,0xffff0000,1,1,nullptr);assert(before==c.pixels());
 c.endOpacityStroke();c.clear(0);auto masked=brush();std::vector<std::uint8_t> mask(128*128);mask[30*128+30]=1;
 masked.stroke(c,20,30,50,30,10,0xff123456,1,1,&mask);int count=0;for(auto p:c.pixels())if(p)count++;assert(count==1);
 Canvas one(128,128),many(128,128);one.clear(0);many.clear(0);auto a=brush(),b=brush();a.stroke(one,10,30,110,30,5,0xff123456,1,1,nullptr);
 for(int x=10;x<110;x++)b.stroke(many,float(x),30,float(x+1),30,5,0xff123456,1,1,nullptr);
 assert(one.pixels()==many.pixels());
 c.endOpacityStroke();c.clear(0);auto blend=brush();blend.parameters[7]=1;blend.stroke(c,10,30,110,30,5,0xffff0000,1,1,nullptr);for(auto p:c.pixels())assert(p==0);
 assert(argc==2);int presets=0;
 for(const auto& entry:std::filesystem::recursive_directory_iterator(argv[1]))if(entry.path().filename()=="preset.json"){
  auto folder=entry.path().parent_path();auto tips=bytes(folder/"tip.bin"),texture=bytes(folder/"texture.bin");assert(tips.size()%16384==0 && tips.size()>0);assert(texture.size()==16384);
  std::ifstream input(entry.path());std::string json((std::istreambuf_iterator<char>(input)),{});auto start=json.find("\"params\"");start=json.find('[',start);auto end=json.find(']',start);std::string values=json.substr(start+1,end-start-1);std::replace(values.begin(),values.end(),',',' ');std::istringstream numbers(values);
  auto real=brush();real.tips=tips;real.texture=texture;for(float& p:real.parameters)assert(bool(numbers>>p));
  Canvas actual(128,128);actual.clear(real.parameters[7]>0?0xffff0000:0);
  real.stroke(actual,20,64,108,64,14,0xff0033ff,1,1,nullptr);bool painted=false;for(auto p:actual.pixels())if(p!=0)painted=true;assert(painted);presets++;
 }
 assert(presets==34);
}
