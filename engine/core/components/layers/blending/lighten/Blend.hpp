#pragma once
namespace velyntora::blending {
inline RGB lighten(RGB src,RGB dst){RGB out{};for(int c=0;c<3;c++){double s=src[c],d=dst[c];out[c]=std::clamp(std::max(s,d),0.,1.);}return out;}
}
