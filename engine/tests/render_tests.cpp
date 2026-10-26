#include "velyntora/RenderEffects.hpp"
#include <cassert>
#include <stdexcept>
using namespace velyntora;
int main(){for(int kind=0;kind<5;kind++){auto result=renderEffect(17,13,kind,kind<3?5:1,32,0,0xff000000,0xffffffff);assert(result.size()==221&&result==renderEffect(17,13,kind,kind<3?5:1,32,0,0xff000000,0xffffffff));assert(renderEffect(17,13,kind,5,32,0,0x804080c0,0x804080c0)==std::vector<std::uint32_t>(221,0x804080c0));assert(renderEffect(17,13,kind,5,32,0,0x000000ff,0x00ff0000)==std::vector<std::uint32_t>(221,0));if(kind!=3)assert(result!=renderEffect(17,13,kind,kind<3?5:1,32,102,0xff000000,0xffffffff));}
 auto alpha=renderEffect(17,13,0,5,4,0,0x00ff0000,0xff0000ff);for(auto p:alpha)if(p>>24)assert((p&0xffffff)==0x0000ff);
 bool invalid=false;try{renderEffect(8192,8192,0,5,4,0,0,0);}catch(const std::invalid_argument&){invalid=true;}assert(invalid);
}
