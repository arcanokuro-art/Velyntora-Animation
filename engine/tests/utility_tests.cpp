#include "velyntora/UtilityEffects.hpp"
#include <cassert>
#include <stdexcept>
using namespace velyntora;
int main(){std::vector<std::uint32_t> solid(81,0x804080c0),source(81);for(int i=0;i<81;i++)source[i]=0xff000000|(i%9)*25<<16|(i/9)*25<<8|i*3;
 for(int kind=0;kind<8;kind++){int size=kind==0?4:kind==4?2:kind==7?4:3;assert(utilityEffect(source,9,9,kind,0,size,kind==4?60:20)==source);auto result=utilityEffect(source,9,9,kind,kind==0?3:kind==1?3:100,size,kind==4?60:20);assert(result!=source);assert(result==utilityEffect(source,9,9,kind,kind==0?3:kind==1?3:100,size,kind==4?60:20));if(kind<4||kind==4)assert(utilityEffect(solid,9,9,kind,kind==0?3:kind==1?3:100,size,kind==4?60:20)==solid);}
 std::vector<std::uint32_t> red(81,0x000000ff);red[40]=0xffff0000;for(int kind=0;kind<4;kind++){auto out=utilityEffect(red,9,9,kind,3,4,20);for(auto p:out)if(p>>24)assert((p&0xffffff)==0xff0000);}
 assert(utilityEffect(source,9,9,4,100,2,0)==source);auto dither=utilityEffect(solid,9,9,7,100,2,0);for(auto p:dither){assert((p>>24)==128);for(int shift:{0,8,16})assert(((p>>shift)&255)==0||((p>>shift)&255)==255);}
 bool invalid=false;try{utilityEffect(source,9,9,0,3,1,0);}catch(const std::invalid_argument&){invalid=true;}assert(invalid);
}
