#pragma once
namespace velyntora::blending {
inline RGB luminosity(RGB src,RGB dst){RGB out{};out=setLum(dst,lum(src));return out;}
}
