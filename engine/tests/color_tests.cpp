#include "velyntora/ColorAdjustments.hpp"
#include <cassert>
#include <stdexcept>
using namespace velyntora;
int main(){
 std::vector<std::uint8_t> selected={0,1,1,0};auto localLevels=autoLevels({0xff000000,0xff404040,0xff808080,0xffffffff},0,&selected);assert(localLevels[1]==0xff000000&&localLevels[2]==0xffffffff);

 std::array<int,256> nearBlackIdentity{};for(int i=0;i<256;i++)nearBlackIdentity[i]=i;assert(applyCurve({0xff010000,0xff000001},nearBlackIdentity,4)==std::vector<std::uint32_t>({0xff010000,0xff000001}));
 std::vector<std::uint32_t> sample={0x80102030,0x00abcdef};
 assert(brightnessContrast(sample,0,0)==sample);
 assert(brightnessContrast(sample,100,0)[0]==0x80ffffff);
 assert(brightnessContrast(sample,-100,0)[0]==0x80000000);
 assert(basicColor(sample,1)[0]==0x80efdfcf);
 for(int kind=0;kind<3;kind++){auto result=basicColor(sample,kind);assert(result[1]==sample[1]);assert((result[0]>>24)==0x80);}

 std::vector<std::uint32_t> pixels={0x80123456,0xff4080c0,0x00112233};std::array<int,256> identity{},inverse{};for(int i=0;i<256;++i){identity[i]=i;inverse[i]=255-i;}
 assert(applyCurve(pixels,identity,0)==pixels);auto inverted=applyCurve(pixels,inverse,0);assert(inverted[0]==0x80edcba9&&inverted[2]==pixels[2]);
 auto red=applyCurve(pixels,inverse,1);assert(red[0]==0x80ed3456);
 assert(applyLevels(pixels,{ChannelLevels{},ChannelLevels{},ChannelLevels{}})==pixels);
 auto levels=applyLevels({0xff4080c0},{ChannelLevels{64,192,100,0,255},ChannelLevels{},ChannelLevels{}});assert(levels[0]==0xff0080c0);
 auto normalized=autoLevels({0xff103050,0xff205070},0);assert(normalized[0]==0xff000000&&normalized[1]==0xffffffff);
 assert(autoLevels({0xff4080c0,0xff4080c0},0)==std::vector<std::uint32_t>({0xff4080c0,0xff4080c0}));
 assert(posterizeRgb(pixels,{256,256,256})==pixels);auto posterized=posterizeRgb({0xff4080c0},{2,3,4});assert(posterized[0]==0xff0080aa);
 assert(hueSaturation(pixels,0,100,0)==pixels);assert(hueSaturation({0x80ff0000},120,100,0)[0]==0x8000ff00);
 assert(hueSaturation({0xffff0000},0,0,0)[0]==0xff808080);assert(hueSaturation({0xff123456},0,100,100)[0]==0xffffffff);
 assert(hueSaturation({0xff123456},0,100,-100)[0]==0xff000000);
 bool failed=false;try{applyLevels(pixels,{ChannelLevels{128,64},ChannelLevels{},ChannelLevels{}});}catch(const std::invalid_argument&){failed=true;}assert(failed);
}
