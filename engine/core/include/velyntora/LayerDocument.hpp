#pragma once
#include <cstddef>
#include <cstdint>
#include <string>
#include <vector>

#include "velyntora/Canvas.hpp"
#include "../../document/tiles/LayerPixels.hpp"

#include "../../components/images/ImageElement.hpp"
#include "../../components/text/TextElement.hpp"

namespace velyntora {
struct Layer {
  std::string name;
  tiles::LayerPixels pixels;
  bool visible = true;
  float opacity = 1.f;
  int blendMode = 0;
  std::shared_ptr<const images::Element> image;
  std::shared_ptr<const text::Element> text;
};
class LayerDocument {
 public:
  LayerDocument(int width, int height);
  int width() const noexcept { return width_; }
  int height() const noexcept { return height_; }
  std::size_t layerCount() const noexcept { return layers_.size(); }
  std::size_t activeIndex() const noexcept { return active_; }
  const Layer& layer(std::size_t index) const { return layers_.at(index); }
  void restoreLayers(std::vector<Layer> layers,std::size_t active) {
    if(layers.empty() || active>=layers.size())throw std::invalid_argument("History layers");
    for(const auto& layer:layers)if(layer.pixels.size()!=std::size_t(width_)*height_)
      throw std::invalid_argument("History dimensions");
    layers_=std::move(layers);active_=active;
  }
  bool renameLayer(std::size_t index, const std::string& name);
  LayerDocument resized(int width, int height, bool scalePixels, bool bilinear = false,
                        int anchor = 0) const;
  LayerDocument cropped(int left, int top, int width, int height) const;
  void addLayer(const std::string& name);
  bool removeLayer(std::size_t index);
  bool selectLayer(std::size_t index);
  bool moveLayer(std::size_t from, std::size_t to);
  bool setVisible(std::size_t index, bool visible);
  bool setOpacity(std::size_t index, float opacity);
  bool setBlendMode(std::size_t index, int mode);
  bool duplicateActive();
  bool mergeDown();
  void replaceActivePixels(const std::vector<std::uint32_t>& pixels);
  void replaceActivePixels(const tiles::LayerPixels& pixels);
  void replaceActivePixels(std::initializer_list<std::uint32_t> pixels){replaceActivePixels(std::vector<std::uint32_t>(pixels));}
  void writeActiveSpan(std::size_t offset,std::size_t count,const std::uint32_t* input){layers_[active_].image.reset();layers_[active_].text.reset();layers_[active_].pixels.writeSpan(offset,count,input);}
  void setActiveImage(std::shared_ptr<const images::Element> image){if(image && !image->valid())throw std::invalid_argument("Invalid image element");if(!image)layers_[active_].text.reset();layers_[active_].image=std::move(image);}
  void setActiveText(std::shared_ptr<const text::Element> text){if(text && (!text->valid() || !layers_[active_].image))throw std::invalid_argument("Invalid text element");layers_[active_].text=std::move(text);}
  std::vector<std::uint32_t> flatten() const;
  void replaceActiveRegion(const std::vector<std::uint32_t>& pixels, int x, int y, int w, int h);
  std::vector<std::uint32_t> flattenRegion(int x, int y, int w, int h) const;

 private:
  int width_, height_;
  std::size_t active_ = 0;
  std::vector<Layer> layers_;
};
}  // namespace velyntora
