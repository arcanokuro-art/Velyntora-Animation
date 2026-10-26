#pragma once
namespace velyntora::blending {
inline RGB color_dodge(RGB src,RGB dst){RGB out{};for(int c=0;c<3;c++){double s=src[c],d=dst[c];out[c]=std::clamp(d<=0?0:s>=1?1:std::min(1.,d/(1-s)),0.,1.);}return out;}
}
