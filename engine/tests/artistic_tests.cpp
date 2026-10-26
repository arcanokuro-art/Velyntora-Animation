#include "velyntora/ArtisticEffects.hpp"
#include <cassert>
#include <stdexcept>
#include <random>
#include <algorithm>
using namespace velyntora;
int main(){
 std::vector<std::uint32_t> solid(25,0x804080c0);assert(artisticEffect(solid,5,5,0,8,2,80)==solid);assert(artisticEffect(solid,5,5,5,50,2,80)==solid);
 auto impulse=std::vector<std::uint32_t>(25,0xff808080);impulse[12]=0xffffffff;assert(artisticEffect(impulse,5,5,5,50,1,80)[12]==0xff808080);assert(artisticEffect(impulse,5,5,5,100,1,80)[12]==0xffffffff);
 assert(artisticEffect({0x80ff2020,0xff2040c0},2,1,6,100,1,80)[0]==0x80202020);assert(artisticEffect({0x80ff2020,0xff2040c0},2,1,6,100,1,80)[1]==0xff2040c0);
 for(int kind=0;kind<7;kind++){auto result=artisticEffect({0x80ff0000,0x00123456,0xff204080},3,1,kind,kind==0?8:100,1,30);assert(result[1]==0x00123456);assert((result[0]>>24)==128&&(result[2]>>24)==255);if(kind!=0&&kind!=5)assert(artisticEffect(solid,5,5,kind,0,1,30)==solid);}
 auto glow=artisticEffect(solid,5,5,3,100,2,80);assert((glow[12]&255)>(solid[12]&255));auto pencil=artisticEffect(solid,5,5,1,100,2,80);assert(pencil[12]==0x80ffffff);
 auto hidden=artisticEffect({0xffff0000,0x000000ff,0xffff0000},3,1,0,8,1,80);assert(hidden[0]==0xffff0000&&hidden[2]==0xffff0000);
 // Sorting is an independent reference for the sliding percentile histogram.
 std::mt19937 random(781);for(int w:{1,3,9})for(int h:{1,4,7}){std::vector<std::uint32_t> input(w*h);for(auto& p:input)p=random();for(int radius:{1,4})for(int percentile:{0,25,50,100}){auto result=artisticEffect(input,w,h,5,percentile,radius,80);for(int y=0;y<h;y++)for(int x=0;x<w;x++){auto pixel=input[y*w+x];if(!(pixel>>24)){assert(result[y*w+x]==pixel);continue;}std::uint32_t expected=pixel&0xff000000;for(int shift:{0,8,16}){std::vector<int> values;for(int dy=-radius;dy<=radius;dy++)for(int dx=-radius;dx<=radius;dx++){auto p=input[std::clamp(y+dy,0,h-1)*w+std::clamp(x+dx,0,w-1)];if(p>>24)values.push_back((p>>shift)&255);}std::sort(values.begin(),values.end());expected|=std::uint32_t(values[(values.size()-1)*percentile/100])<<shift;}assert(result[y*w+x]==expected);}}}
 bool invalid=false;try{artisticEffect(solid,5,5,0,1,2,80);}catch(const std::invalid_argument&){invalid=true;}assert(invalid);
}
