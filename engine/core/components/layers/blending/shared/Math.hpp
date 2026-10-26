#pragma once
#include <algorithm>
#include <array>
#include <cmath>
#include <cstdint>
namespace velyntora::blending {
using RGB=std::array<double,3>;
inline double lum(RGB c){return .3*c[0]+.59*c[1]+.11*c[2];}
inline double sat(RGB c){return *std::max_element(c.begin(),c.end())-*std::min_element(c.begin(),c.end());}
inline RGB setLum(RGB c,double l){double delta=l-lum(c);for(auto& v:c)v+=delta;double lo=*std::min_element(c.begin(),c.end()),hi=*std::max_element(c.begin(),c.end());if(lo<0)for(auto& v:c)v=l+(v-l)*l/(l-lo);if(hi>1)for(auto& v:c)v=l+(v-l)*(1-l)/(hi-l);return c;}
inline RGB setSat(RGB c,double s){std::array<int,3> a{0,1,2};std::sort(a.begin(),a.end(),[&](int i,int j){return c[i]<c[j];});double lo=c[a[0]],hi=c[a[2]];c[a[1]]=hi>lo?(c[a[1]]-lo)*s/(hi-lo):0;c[a[2]]=hi>lo?s:0;c[a[0]]=0;return c;}
}
