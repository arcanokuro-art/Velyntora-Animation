#include "velyntora/PixelEffects.hpp"
#include <cassert>
#include <stdexcept>
using namespace velyntora;
int main(){
 std::vector<std::uint32_t> red={0,0xffff0000,0};auto blurred=pixelEffect(red,3,1,0,1);assert((blurred[1]>>24)==85);assert((blurred[1]&0xffffff)==0xff0000);assert(red[0]==0);
 auto pixelated=pixelEffect({0xffff0000,0xff0000ff},2,1,4,2);assert(pixelated[0]==pixelated[1]&&pixelated[0]==0xff800080);
 auto solid=std::vector<std::uint32_t>(9,0xff808080);assert(pixelEffect(solid,3,3,1,100)==solid);
 auto edges=pixelEffect(solid,3,3,2,100);assert(edges[4]==0xff000000);
 auto noise=pixelEffect(solid,3,3,5,20);assert(noise==pixelEffect(solid,3,3,5,20));for(auto p:noise){assert((p>>24)==255);assert((p&255)>=108&&(p&255)<=148);}
 auto vignette=pixelEffect(std::vector<std::uint32_t>(9,0xffffffff),3,3,6,100);assert(vignette[4]==0xffffffff&&vignette[0]!=vignette[4]);
 bool failed=false;try{pixelEffect(red,2,2,0,1);}catch(const std::invalid_argument&){failed=true;}assert(failed);
 for(int kind=0;kind<7;++kind){auto result=pixelEffect({0,0},2,1,kind,1);assert(result[0]==0&&result[1]==0);}
}
