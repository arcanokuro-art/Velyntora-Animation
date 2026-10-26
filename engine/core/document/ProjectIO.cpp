#include "velyntora/ProjectIO.hpp"

#include <algorithm>
#include <array>
#include <bit>
#include <cmath>
#include <cstring>
#include <stdexcept>

namespace velyntora {
namespace {
constexpr std::size_t maxLayers = 32, maxName = 4096;
constexpr std::uint64_t maxPixels = limits::maxDocumentPixels * maxLayers;
constexpr std::array<unsigned char, 8> magic = {'V', 'L', 'Y', 'C', 'O', 'R', 'E', 4};
void put(const ProjectWrite& write, std::uint32_t value) {
  unsigned char b[4];
  for (int i = 0; i < 4; ++i) b[i] = static_cast<unsigned char>(value >> (8 * i));
  write(b, 4);
}
std::uint32_t get(const ProjectRead& read) {
  unsigned char b[4];
  read(b, 4);
  std::uint32_t value = 0;
  for (int i = 0; i < 4; ++i) value |= std::uint32_t(b[i]) << (8 * i);
  return value;
}
void dimensions(std::uint32_t w, std::uint32_t h, std::uint32_t count) {
  if (w == 0 || h == 0 || !limits::accepts(w,h) || count == 0 || count > maxLayers ||
      std::uint64_t(w) * h > maxPixels / count)
    throw std::invalid_argument("Project exceeds document limits");
}
}  // namespace
void writeProject(const LayerDocument& doc, const ProjectWrite& write) {
  dimensions(doc.width(), doc.height(), doc.layerCount());
  for (std::size_t i = 0; i < doc.layerCount(); ++i)
    if (doc.layer(i).name.size() > maxName) throw std::invalid_argument("Layer name too long");
  bool extended=false;for(std::size_t i=0;i<doc.layerCount();i++)extended=extended || bool(doc.layer(i).image);
  bool hasText=false;for(std::size_t i=0;i<doc.layerCount();i++)hasText=hasText || bool(doc.layer(i).text);
  auto signature=magic;signature[7]=hasText?4:extended?3:2;write(signature.data(),signature.size());
  put(write, doc.width());
  put(write, doc.height());
  put(write, doc.layerCount());
  put(write, doc.activeIndex());
  std::array<unsigned char, 4096> block{};
  for (std::size_t i = 0; i < doc.layerCount(); ++i) {
    const auto& layer = doc.layer(i);
    put(write, layer.name.size());
    write(layer.name.data(), layer.name.size());
    put(write, layer.visible ? 1 : 0);
    put(write, std::bit_cast<std::uint32_t>(layer.opacity));
    put(write,layer.blendMode);
    for (std::size_t offset = 0; offset < layer.pixels.size(); offset += block.size() / 4) {
      auto count = std::min(block.size() / 4, layer.pixels.size() - offset);
      std::array<std::uint32_t,1024> decoded{};layer.pixels.readSpan(offset,count,decoded.data());
      for (std::size_t p = 0; p < count; ++p)
        for (int b = 0; b < 4; ++b)
          block[p * 4 + b] = static_cast<unsigned char>(decoded[p] >> (8 * b));
      write(block.data(), count * 4);
    }
    if(extended)put(write,layer.image?1:0);
    if(layer.image){
      if(!layer.image->valid())throw std::invalid_argument("Invalid image element");
      for(float v:layer.image->transform)put(write,std::bit_cast<std::uint32_t>(v));
      put(write,layer.image->source->bytes);layer.image->source->write(write);
    }
    if(hasText){put(write,layer.text?1:0);if(layer.text){if(!layer.text->valid() || !layer.image)throw std::invalid_argument("Invalid text element");put(write,layer.text->content.size());write(layer.text->content.data(),layer.text->content.size());for(auto v:layer.text->style)put(write,std::uint32_t(v));}}
  }
}
LayerDocument readProject(const ProjectRead& read, int requiredWidth, int requiredHeight,
                          std::uint64_t maxDocumentPixels) {
  std::array<unsigned char, 8> signature{};
  read(signature.data(), signature.size());
  const auto version=signature[7]; signature[7]=4;
  if (signature != magic || (version!=1 && version!=2 && version!=3 && version!=4)) throw std::invalid_argument("Unsupported project format");
  auto w = get(read), h = get(read), count = get(read), active = get(read);
  dimensions(w, h, count);
  if (std::uint64_t(w) * h > maxDocumentPixels || active >= count ||
      (requiredWidth && w != std::uint32_t(requiredWidth)) ||
      (requiredHeight && h != std::uint32_t(requiredHeight)))
    throw std::invalid_argument("Invalid project dimensions or active layer");
  LayerDocument doc(w, h);
  tiles::LayerPixels pixels(std::size_t(w)*h,0);
  std::array<unsigned char, 4096> block{};
  for (std::uint32_t i = 0; i < count; ++i) {
    auto size = get(read);
    if (size > maxName) throw std::invalid_argument("Layer name too long");
    std::string name(size, '\0');
    read(name.data(), size);
    auto visible = get(read);
    float opacity = std::bit_cast<float>(get(read));
    auto mode=version>=2?get(read):0;
    if(mode>15)throw std::invalid_argument("Invalid blend mode");
    if (visible > 1 || !std::isfinite(opacity) || opacity < 0 || opacity > 1)
      throw std::invalid_argument("Invalid layer properties");
    if (i == 0)
      doc.renameLayer(0, name);
    else
      doc.addLayer(name);
    doc.selectLayer(i);
    doc.setVisible(i, visible != 0);
    doc.setOpacity(i, opacity);
    doc.setBlendMode(i, mode);
    for (std::size_t offset = 0; offset < pixels.size(); offset += block.size() / 4) {
      auto n = std::min(block.size() / 4, pixels.size() - offset);
      read(block.data(), n * 4);
      std::vector<std::uint32_t> decoded(n);
      for (std::size_t p = 0; p < n; ++p) {
        std::uint32_t pixel = 0;
        for (int b = 0; b < 4; ++b) pixel |= std::uint32_t(block[p * 4 + b]) << (8 * b);
        decoded[p] = pixel;
      }
      if(offset==0)pixels.resize(std::size_t(w)*h,decoded[0]);
      pixels.writeSpan(offset,n,decoded.data());
    }
    doc.replaceActivePixels(pixels);
    if(version>=3){
      auto has=get(read);if(has>1)throw std::invalid_argument("Image element flag");
      if(has){auto image=std::make_shared<images::Element>();
        for(float& v:image->transform)v=std::bit_cast<float>(get(read));
        auto length=get(read);image->source=std::make_shared<images::Source>(length,read);
        doc.setActiveImage(std::move(image));
      }
    }
    if(version>=4){auto has=get(read);if(has>1)throw std::invalid_argument("Text flag");if(has){auto text=std::make_shared<text::Element>();auto size=get(read);if(!size || size>16384)throw std::invalid_argument("Text length");text->content.resize(size);read(text->content.data(),size);for(auto& v:text->style)v=std::int32_t(get(read));doc.setActiveText(std::move(text));}}
  }
    // Reader keeps v1/v2/v3 unchanged; v4 appends text metadata to every layer.
  doc.selectLayer(active);
  return doc;
}
}  // namespace velyntora
