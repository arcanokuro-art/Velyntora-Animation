#pragma once
namespace velyntora::blending {
inline RGB hard_light(RGB src,RGB dst){RGB out{};for(int c=0;c<3;c++){double s=src[c],d=dst[c];out[c]=std::clamp(s<=.5?2*s*d:1-2*(1-s)*(1-d),0.,1.);}return out;}
}
