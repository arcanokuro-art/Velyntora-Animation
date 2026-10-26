#pragma once
namespace velyntora::blending {
inline RGB hue(RGB src,RGB dst){RGB out{};out=setLum(setSat(src,sat(dst)),lum(dst));return out;}
}
