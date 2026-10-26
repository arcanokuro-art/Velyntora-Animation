#pragma once
namespace velyntora::blending {
inline RGB color(RGB src,RGB dst){RGB out{};out=setLum(src,lum(dst));return out;}
}
