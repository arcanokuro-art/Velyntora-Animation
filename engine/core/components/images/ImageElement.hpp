#pragma once
#include "../../document/history/HistoryFile.hpp"
#include <array>
#include <algorithm>
#include <cmath>
#include <functional>
#include <memory>
namespace velyntora::images {
inline std::string storageDirectory="/tmp";
constexpr std::uint32_t maxSourceBytes=256*1024*1024;
// Original encoded source lives in an unlinked disk file and is shared by edits/history.
struct Source {
  std::shared_ptr<history::HistoryFile> file;
  std::uint32_t bytes;
  Source(std::uint32_t size,const std::function<void(void*,std::size_t)>& read):file(std::make_shared<history::HistoryFile>(storageDirectory)),bytes(size){
    if(!size || size>maxSourceBytes)throw std::invalid_argument("Image source size");
    std::array<unsigned char,65536> buffer;
    for(std::size_t at=0;at<size;){auto n=std::min<std::size_t>(buffer.size(),size-at);read(buffer.data(),n);file->append(buffer.data(),n);at+=n;}
  }
  void write(const std::function<void(const void*,std::size_t)>& out)const{
    std::array<unsigned char,65536> buffer;
    for(std::size_t at=0;at<bytes;){auto n=std::min<std::size_t>(buffer.size(),bytes-at);file->read(at,buffer.data(),n);out(buffer.data(),n);at+=n;}
  }
};
struct Element {
  std::shared_ptr<const Source> source;
  // crop L,T,R,B; center X,Y; displayed width,height; angle.
  std::array<float,9> transform;
  bool valid()const{
    for(float v:transform)if(!std::isfinite(v) || std::abs(v)>1000000)return false;
    return source && transform[0]>=0 && transform[1]>=0 && transform[2]>transform[0] && transform[3]>transform[1] && transform[2]<=6000 && transform[3]<=6000 && transform[6]>=1 && transform[7]>=1 && transform[6]<=24000 && transform[7]<=24000;
  }
};
}
