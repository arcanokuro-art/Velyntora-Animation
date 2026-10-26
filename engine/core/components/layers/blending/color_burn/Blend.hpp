#pragma once
namespace velyntora::blending {
inline RGB color_burn(RGB src,RGB dst){RGB out{};for(int c=0;c<3;c++){double s=src[c],d=dst[c];out[c]=std::clamp(d>=1?1:s<=0?0:1-std::min(1.,(1-d)/s),0.,1.);}return out;}
}
