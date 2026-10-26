#pragma once
#include "shared/Math.hpp"
#include "normal/Blend.hpp"
#include "multiply/Blend.hpp"
#include "color_burn/Blend.hpp"
#include "color_dodge/Blend.hpp"
#include "overlay/Blend.hpp"
#include "difference/Blend.hpp"
#include "lighten/Blend.hpp"
#include "darken/Blend.hpp"
#include "screen/Blend.hpp"
#include "xor/Blend.hpp"
#include "hard_light/Blend.hpp"
#include "soft_light/Blend.hpp"
#include "color/Blend.hpp"
#include "luminosity/Blend.hpp"
#include "hue/Blend.hpp"
#include "saturation/Blend.hpp"
namespace velyntora::blending {
inline std::uint32_t composite(std::uint32_t src,std::uint32_t dst,float opacity,int mode){
 double sa=(src>>24)/255.*opacity,da=(dst>>24)/255.,oa=sa+da*(1-sa);if(oa<=0)return 0;
 if(sa<=0)return da>0?dst:0;
 if(mode==0&&sa>=1)return src;
 if(da<=0)return (std::uint32_t(std::lround(sa*255))<<24)|(src&0xffffff);
 RGB s{},d{};for(int c=0;c<3;c++){s[c]=((src>>(16-8*c))&255)/255.;d[c]=((dst>>(16-8*c))&255)/255.;}
 RGB b{};switch(mode){
case 0:b=normal(s,d);break;
case 1:b=multiply(s,d);break;
case 2:b=color_burn(s,d);break;
case 3:b=color_dodge(s,d);break;
case 4:b=overlay(s,d);break;
case 5:b=difference(s,d);break;
case 6:b=lighten(s,d);break;
case 7:b=darken(s,d);break;
case 8:b=screen(s,d);break;
case 9:b=exclusive_or(s,d);break;
case 10:b=hard_light(s,d);break;
case 11:b=soft_light(s,d);break;
case 12:b=color(s,d);break;
case 13:b=luminosity(s,d);break;
case 14:b=hue(s,d);break;
case 15:b=saturation(s,d);break;
default:b=s;}
 std::uint32_t out=std::uint32_t(std::lround(oa*255))<<24;
 for(int c=0;c<3;c++){double v=(sa*(1-da)*s[c]+sa*da*b[c]+(1-sa)*da*d[c])/oa;out|=std::uint32_t(std::clamp(std::lround(v*255),0L,255L))<<(16-8*c);}return out;
}
}
