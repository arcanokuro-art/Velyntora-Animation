#pragma once
#include "HistoryFile.hpp"
#include "velyntora/LayerDocument.hpp"
#include <cstring>
#include <memory>
namespace velyntora::history {
class HistoryArchive {
  std::shared_ptr<HistoryFile> file_;
  std::vector<std::shared_ptr<const images::Element>> images_;
  std::map<const images::Element*,std::uint64_t> imageIds_;
  struct Writer {
    std::vector<std::uint8_t> bytes;
    template<class T> void put(T value){auto p=reinterpret_cast<const std::uint8_t*>(&value);bytes.insert(bytes.end(),p,p+sizeof(T));}
    void text(const std::string& value){put(std::uint64_t(value.size()));bytes.insert(bytes.end(),value.begin(),value.end());}
  };
  struct Reader {
    const std::vector<std::uint8_t>& bytes;std::size_t pos=0;
    template<class T> T get(){if(sizeof(T)>bytes.size()-pos)throw std::runtime_error("History metadata");T value;std::memcpy(&value,bytes.data()+pos,sizeof(T));pos+=sizeof(T);return value;}
    std::string text(){auto size=get<std::uint64_t>();if(size>bytes.size()-pos)throw std::runtime_error("History name");std::string value(reinterpret_cast<const char*>(bytes.data()+pos),size);pos+=size;return value;}
  };
 public:
  struct Entry {std::uint64_t offset,bytes;};
  explicit HistoryArchive(const std::string& directory):file_(std::make_shared<HistoryFile>(directory)){}
  Entry capture(const LayerDocument& document) {
    Writer out;out.put(document.width());out.put(document.height());
    out.put(std::uint64_t(document.activeIndex()));out.put(std::uint64_t(document.layerCount()));
    for(std::size_t i=0;i<document.layerCount();++i) {
      const auto& layer=document.layer(i);auto saved=layer.pixels.archive(file_);
      out.text(layer.name);out.put(std::uint8_t(layer.visible));out.put(layer.opacity);out.put(layer.blendMode);
      std::uint64_t imageId=0;
      if(layer.image){auto [it,added]=imageIds_.emplace(layer.image.get(),images_.size()+1);if(added)images_.push_back(layer.image);imageId=it->second;}
      out.put(imageId);out.put(std::uint8_t(bool(layer.text)));if(layer.text){out.text(layer.text->content);for(auto v:layer.text->style)out.put(v);}
      out.put(saved.size);out.put(saved.background);out.put(std::uint64_t(saved.blocks.size()));
      for(const auto& block:saved.blocks){out.put(block.key);out.put(block.offset);}
    }
    Entry result{file_->append(out.bytes.data(),out.bytes.size()),out.bytes.size()};file_->flush();return result;
  }
  LayerDocument restore(Entry entry) {
    std::vector<std::uint8_t> bytes(entry.bytes);file_->read(entry.offset,bytes.data(),bytes.size());Reader in{bytes};
    int width=in.get<int>(),height=in.get<int>();LayerDocument result(width,height);
    auto active=in.get<std::uint64_t>(),count=in.get<std::uint64_t>();
    if(!count || count>32)throw std::runtime_error("History layer count");
    std::vector<Layer> layers;layers.reserve(count);
    for(std::uint64_t i=0;i<count;++i) {
      Layer layer;layer.name=in.text();layer.visible=in.get<std::uint8_t>();layer.opacity=in.get<float>();layer.blendMode=in.get<int>();
      auto imageId=in.get<std::uint64_t>();if(imageId>images_.size())throw std::runtime_error("History image");if(imageId)layer.image=images_[imageId-1];
      if(in.get<std::uint8_t>()){auto text=std::make_shared<text::Element>();text->content=in.text();for(auto& v:text->style)v=in.get<std::int32_t>();if(!text->valid() || !layer.image)throw std::runtime_error("History text");layer.text=std::move(text);}
      tiles::LayerPixels::Archived saved; saved.size=in.get<std::uint64_t>();saved.background=in.get<std::uint32_t>();
      auto blocks=in.get<std::uint64_t>();if(blocks>(saved.size+4095)/4096)throw std::runtime_error("History blocks");
      saved.blocks.reserve(blocks);
      for(std::uint64_t n=0;n<blocks;++n)saved.blocks.push_back({in.get<std::uint64_t>(),in.get<std::uint64_t>()});
      layer.pixels=tiles::LayerPixels::restore(saved,file_);layers.push_back(std::move(layer));
    }
    result.restoreLayers(std::move(layers),active);return result;
  }
};
}
