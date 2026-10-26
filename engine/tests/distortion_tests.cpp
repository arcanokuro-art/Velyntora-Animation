#include "velyntora/DistortionEffects.hpp"
#include <cassert>
#include <stdexcept>
using namespace velyntora;
int main(){std::vector<std::uint32_t> constant(81,0x804080c0),src(81,0x000000ff);src[22]=0xffff0000;src[40]=0xffff0000;src[48]=0xffff0000;
 for(int kind=0;kind<5;kind++){assert(distortionEffect(constant,9,9,kind,50,60,35,50,50)==constant);assert(distortionEffect(src,9,9,kind,0,60,35,50,50)==src);auto result=distortionEffect(src,9,9,kind,50,60,35,50,50);assert(result!=src);for(auto p:result)if(p>>24)assert((p&0xffffff)==0xff0000);assert(result==distortionEffect(src,9,9,kind,50,60,35,50,50));}
 auto twisted=distortionEffect(src,9,9,0,80,60,0,50,50);assert(twisted[0]==src[0]&&twisted[80]==src[80]);auto bulge=distortionEffect(src,9,9,1,80,100,0,50,50),pinch=distortionEffect(src,9,9,1,-80,100,0,50,50);assert(bulge!=pinch&&bulge[40]==src[40]&&pinch[40]==src[40]);
 bool rejected=false;try{distortionEffect(src,9,9,5,50,60,0,50,50);}catch(const std::invalid_argument&){rejected=true;}assert(rejected);
}
