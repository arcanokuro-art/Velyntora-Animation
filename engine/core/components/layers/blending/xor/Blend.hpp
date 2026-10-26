#pragma once
namespace velyntora::blending {
inline RGB exclusive_or(RGB src,RGB dst){RGB out{};for(int c=0;c<3;c++){double s=src[c],d=dst[c];out[c]=std::clamp(double(int(std::lround(s*255)) ^ int(std::lround(d*255)))/255,0.,1.);}return out;}
}
