#include "velyntora/CanvasLimits.hpp"
#include "velyntora/LayerDocument.hpp"
#include "../components/layers/blending/BlendModes.hpp"

#include <algorithm>
#include <array>
#include <cmath>
#include <stdexcept>
#include <utility>

namespace velyntora {
LayerDocument::LayerDocument(int w, int h) : width_(w), height_(h) {
  if (!velyntora::limits::accepts(w,h))
    throw std::invalid_argument("Invalid dimensions");
  layers_.push_back({"Fondo",
                     tiles::LayerPixels(static_cast<std::size_t>(w) * h, 0xFFFFFFFFu), true,
                     1.f});
}
bool LayerDocument::renameLayer(std::size_t index, const std::string& name) {
  if (index >= layers_.size()) return false;
  layers_[index].name = name;
  return true;
}
LayerDocument LayerDocument::resized(int w, int h, bool scalePixels, bool bilinear,
                                     int anchor) const {
  if (anchor < 0 || anchor > 8) throw std::invalid_argument("Invalid canvas anchor");
  LayerDocument result(w, h);
  result.layers_.clear();
  const int offsetX = int(std::floor((double(w) - width_) * (anchor % 3) / 2));
  const int offsetY = int(std::floor((double(h) - height_) * (anchor / 3) / 2));
  for (const auto& layer : layers_) {
    Layer output{layer.name, tiles::LayerPixels(std::size_t(w) * h, 0u), layer.visible,
                 layer.opacity, layer.blendMode};
    if (layer.pixels.isUniform() && (scalePixels || layer.pixels.background()==0)) {
      output.pixels.resize(std::size_t(w)*h,layer.pixels.background());
      result.layers_.push_back(std::move(output));
      continue;
    }
    for (int y = 0; y < h; ++y)
      for (int x = 0; x < w; ++x) {
        if (scalePixels && bilinear) {
          const double sx = std::clamp((x + .5) * width_ / w - .5, 0., double(width_ - 1));
          const double sy = std::clamp((y + .5) * height_ / h - .5, 0., double(height_ - 1));
          const int x0 = int(sx), y0 = int(sy), x1 = std::min(x0 + 1, width_ - 1),
                    y1 = std::min(y0 + 1, height_ - 1);
          const double fx = sx - x0, fy = sy - y0,
                       weights[4] = {(1 - fx) * (1 - fy), fx * (1 - fy), (1 - fx) * fy, fx * fy};
          const std::uint32_t pixels[4] = {layer.pixels[std::size_t(y0) * width_ + x0],
                                           layer.pixels[std::size_t(y0) * width_ + x1],
                                           layer.pixels[std::size_t(y1) * width_ + x0],
                                           layer.pixels[std::size_t(y1) * width_ + x1]};
          double alpha = 0., channels[3] = {0., 0., 0.};
          for (int i = 0; i < 4; ++i) {
            const double weight = weights[i] * (pixels[i] >> 24);
            alpha += weight;
            for (int c = 0; c < 3; ++c) channels[c] += weight * ((pixels[i] >> (16 - 8 * c)) & 255);
          }
          const auto a = std::uint32_t(std::lround(alpha));
          std::uint32_t pixel = a << 24;
          if (a > 0)
            for (int c = 0; c < 3; ++c)
              pixel |= std::uint32_t(std::clamp(std::lround(channels[c] / alpha), 0L, 255L))
                       << (16 - 8 * c);
          output.pixels[std::size_t(y) * w + x] = pixel;
        } else {
          const int sx = scalePixels ? int(std::int64_t(x) * width_ / w) : x - offsetX;
          const int sy = scalePixels ? int(std::int64_t(y) * height_ / h) : y - offsetY;
          if (sx >= 0 && sy >= 0 && sx < width_ && sy < height_)
            output.pixels[std::size_t(y) * w + x] = layer.pixels[std::size_t(sy) * width_ + sx];
        }
      }
    result.layers_.push_back(std::move(output));
  }
  result.active_ = active_;
  return result;
}
LayerDocument LayerDocument::cropped(int left, int top, int w, int h) const {
  if (left < 0 || top < 0 || w <= 0 || h <= 0 || std::int64_t(left) + w > width_ ||
      std::int64_t(top) + h > height_)
    throw std::invalid_argument("Invalid crop bounds");
  LayerDocument result(w, h);
  result.layers_.clear();
  for (const auto& layer : layers_) {
    Layer output{layer.name, tiles::LayerPixels(std::size_t(w) * h,0u), layer.visible,
                 layer.opacity, layer.blendMode};
    if (layer.pixels.isUniform()) output.pixels.resize(std::size_t(w)*h,layer.pixels.background());
    else {
      std::vector<std::uint32_t> row(w);
      for(int y=0;y<h;++y){
        layer.pixels.readSpan(std::size_t(y+top)*width_+left,w,row.data());
        output.pixels.writeSpan(std::size_t(y)*w,w,row.data());
      }
    }
    result.layers_.push_back(std::move(output));
  }
  result.active_ = active_;
  return result;
}
void LayerDocument::addLayer(const std::string& name) {
  layers_.insert(
      layers_.begin() + static_cast<std::ptrdiff_t>(active_ + 1),
      Layer{name, tiles::LayerPixels(static_cast<std::size_t>(width_) * height_,0u), true,
            1.f});
  ++active_;
}
bool LayerDocument::removeLayer(std::size_t index) {
  if (index >= layers_.size() || layers_.size() == 1) return false;
  layers_.erase(layers_.begin() + static_cast<std::ptrdiff_t>(index));
  if (active_ >= layers_.size())
    active_ = layers_.size() - 1;
  else if (index < active_)
    --active_;
  return true;
}
bool LayerDocument::selectLayer(std::size_t index) {
  if (index >= layers_.size()) return false;
  active_ = index;
  return true;
}
bool LayerDocument::moveLayer(std::size_t from, std::size_t to) {
  if (from >= layers_.size() || to >= layers_.size()) return false;
  if (from == to) return true;
  Layer moved = std::move(layers_[from]);
  layers_.erase(layers_.begin() + static_cast<std::ptrdiff_t>(from));
  layers_.insert(layers_.begin() + static_cast<std::ptrdiff_t>(to), std::move(moved));
  if (active_ == from)
    active_ = to;
  else if (from < active_ && to >= active_)
    --active_;
  else if (from > active_ && to <= active_)
    ++active_;
  return true;
}
bool LayerDocument::setVisible(std::size_t index, bool visible) {
  if (index >= layers_.size()) return false;
  layers_[index].visible = visible;
  return true;
}
bool LayerDocument::setOpacity(std::size_t index, float opacity) {
  if (index >= layers_.size() || !std::isfinite(opacity) || opacity < 0.f || opacity > 1.f)
    return false;
  layers_[index].opacity = opacity;
  return true;
}
void LayerDocument::replaceActivePixels(const tiles::LayerPixels& pixels){
  if(pixels.size()!=std::size_t(width_)*height_)throw std::invalid_argument("Layer dimensions");
  if(layers_[active_].image && !(layers_[active_].pixels==pixels)){layers_[active_].image.reset();layers_[active_].text.reset();}
  layers_[active_].pixels=pixels;
}
void LayerDocument::replaceActivePixels(const std::vector<std::uint32_t>& pixels) {
  if (pixels.size() != layers_[active_].pixels.size())
    throw std::invalid_argument("Pixel count mismatch");
  if(layers_[active_].image && !(layers_[active_].pixels==pixels)){layers_[active_].image.reset();layers_[active_].text.reset();}
  layers_[active_].pixels = pixels;
}
std::vector<std::uint32_t> LayerDocument::flatten() const {
  std::vector<std::uint32_t> result(static_cast<std::size_t>(width_) * height_, 0u);
  for (const Layer& layer : layers_) {
    if (!layer.visible || layer.opacity <= 0.f) continue;
    if(layer.pixels.isUniform() && !(layer.pixels.background()>>24)) continue;
    if(layer.pixels.isUniform() && layer.opacity==1 && layer.blendMode==0
        && (layer.pixels.background()>>24)==255) {
      std::fill(result.begin(),result.end(),layer.pixels.background());continue;
    }
    std::array<std::uint32_t,4096> source;
    for(std::size_t offset=0;offset<result.size();offset+=source.size()) {
      std::size_t count=std::min(source.size(),result.size()-offset);
      layer.pixels.readSpan(offset,count,source.data());
      for(std::size_t i=0;i<count;i++)
        result[offset+i]=(layer.opacity==1 && layer.blendMode==0 && (source[i]>>24)==255)?source[i]:blending::composite(source[i],result[offset+i],layer.opacity,layer.blendMode);
    }
  }
  return result;
}
void LayerDocument::replaceActiveRegion(const std::vector<std::uint32_t>& pixels,
                                         int x, int y, int w, int h) {
  if (pixels.size()!=std::size_t(width_)*height_ || x<0 || y<0 || w<=0 || h<=0
      || x>width_-w || y>height_-h) throw std::invalid_argument("Invalid region");
  layers_[active_].image.reset();layers_[active_].text.reset();
  auto& target=layers_[active_].pixels;
  for (int row=y;row<y+h;row++) {
    const auto offset=std::size_t(row)*width_+x;
    std::copy_n(pixels.begin()+offset,w,target.begin()+offset);
  }
}
std::vector<std::uint32_t> LayerDocument::flattenRegion(int x, int y, int w, int h) const {
  if (x<0 || y<0 || w<=0 || h<=0 || x>width_-w || y>height_-h)
    throw std::invalid_argument("Invalid region");
  std::vector<std::uint32_t> result(static_cast<std::size_t>(w) * h, 0u);
  for (const Layer& layer : layers_) {
    if (!layer.visible || layer.opacity <= 0.f) continue;
    if(layer.pixels.isUniform() && !(layer.pixels.background()>>24)) continue;
    if(layer.pixels.isUniform() && layer.opacity==1 && layer.blendMode==0
        && (layer.pixels.background()>>24)==255) {
      std::fill(result.begin(),result.end(),layer.pixels.background());continue;
    }
    std::vector<std::uint32_t> source(w);
    for(int row=0;row<h;row++) {
      layer.pixels.readSpan(std::size_t(y+row)*width_+x,w,source.data());
      std::size_t offset=std::size_t(row)*w;
      for(int col=0;col<w;col++)
        result[offset+col]=(layer.opacity==1 && layer.blendMode==0 && (source[col]>>24)==255)?source[col]:blending::composite(source[col],result[offset+col],layer.opacity,layer.blendMode);
    }
  }
  return result;
}
}  // namespace velyntora

namespace velyntora {
bool LayerDocument::setBlendMode(std::size_t i,int mode){if(i>=layers_.size()||mode<0||mode>15)return false;layers_[i].blendMode=mode;return true;}
bool LayerDocument::duplicateActive(){Layer copy=layers_[active_];copy.name+=" copia";layers_.insert(layers_.begin()+active_+1,std::move(copy));++active_;return true;}
bool LayerDocument::mergeDown(){
 if(active_==0)return false;
 auto& upper=layers_[active_];auto& lower=layers_[active_-1];
 const float lo=lower.visible?lower.opacity:0,up=upper.visible?upper.opacity:0;
 auto combine=[&](std::uint32_t a,std::uint32_t b){return blending::composite(a,blending::composite(b,0,lo,0),up,upper.blendMode);};
 if(lower.pixels.isUniform() && upper.pixels.isUniform()){
   auto color=combine(upper.pixels.background(),lower.pixels.background());
   lower.pixels.resize(lower.pixels.size(),color);
 }else {
   std::array<std::uint32_t,4096> bottom{},top{};
   for(std::size_t at=0;at<lower.pixels.size();at+=bottom.size()){
     auto n=std::min(bottom.size(),lower.pixels.size()-at);
     lower.pixels.readSpan(at,n,bottom.data());upper.pixels.readSpan(at,n,top.data());
     for(std::size_t i=0;i<n;++i)bottom[i]=combine(top[i],bottom[i]);
     lower.pixels.writeSpan(at,n,bottom.data());
   }
 }
 lower.image.reset();lower.text.reset();
 lower.opacity=1;lower.visible=true;lower.blendMode=0;
 layers_.erase(layers_.begin()+active_);--active_;return true;
}
}
