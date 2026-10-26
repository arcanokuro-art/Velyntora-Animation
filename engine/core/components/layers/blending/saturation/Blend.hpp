#pragma once
namespace velyntora::blending {
inline RGB saturation(RGB src,RGB dst){RGB out{};out=setLum(setSat(dst,sat(src)),lum(dst));return out;}
}
