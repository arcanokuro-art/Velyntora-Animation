#pragma once
namespace velyntora::blending {
inline RGB soft_light(RGB src,RGB dst){RGB out{};for(int c=0;c<3;c++){double s=src[c],d=dst[c];out[c]=std::clamp(s<=.5?d-(1-2*s)*d*(1-d):d+(2*s-1)*((d<=.25?((16*d-12)*d+4)*d:std::sqrt(d))-d),0.,1.);}return out;}
}
