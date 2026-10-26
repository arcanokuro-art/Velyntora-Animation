#pragma once
#include <algorithm>
#include <cstdint>
#include <iterator>
#include <map>
#include <memory>
#include <vector>
#include <stdexcept>
#include "../pages/PixelPage.hpp"

namespace velyntora::tiles {
// Sparse copy-on-write blocks keep blank layers and unchanged history cheap.
class LayerPixels {
  static constexpr std::size_t blockSize=4096;
  using Block=pages::PixelPage;
  std::size_t size_=0;
  std::uint32_t background_=0;
  std::map<std::size_t,std::shared_ptr<Block>> blocks_;
  std::uint32_t get(std::size_t i) const {
    auto found=blocks_.find(i/blockSize);
    return found==blocks_.end()?background_:(*found->second)[i%blockSize];
  }
  void put(std::size_t i,std::uint32_t value) {
    if(get(i)==value) return;
    auto key=i/blockSize;
    auto found=blocks_.find(key);
    if(found==blocks_.end()) {
      auto block=std::make_shared<Block>(std::min(blockSize,size_-key*blockSize),background_);
      (*block)[i%blockSize]=value;blocks_.emplace(key,std::move(block));
    } else {
      if(found->second.use_count()>(found->second->listed?2:1)) found->second=std::make_shared<Block>(*found->second);
      found->second->changed();
      (*found->second)[i%blockSize]=value;
    }
  }
 public:
  LayerPixels()=default;
  explicit LayerPixels(std::size_t size):size_(size){}
  void resize(std::size_t size,std::uint32_t color=0){size_=size;background_=color;blocks_.clear();}
  void assign(std::size_t size,std::uint32_t color){resize(size,color);}
  void clear(){resize(0);}
  bool empty()const noexcept{return size_==0;}
  void swap(LayerPixels& other)noexcept{std::swap(size_,other.size_);std::swap(background_,other.background_);blocks_.swap(other.blocks_);}
  std::uint32_t& mutablePixel(std::size_t i){
    auto key=i/blockSize;auto found=blocks_.find(key);
    if(found==blocks_.end())found=blocks_.emplace(key,std::make_shared<Block>(std::min(blockSize,size_-key*blockSize),background_)).first;
    else if(found->second.use_count()>(found->second->listed?2:1))found->second=std::make_shared<Block>(*found->second);
    found->second->changed();return (*found->second)[i%blockSize];
  }
  LayerPixels(const std::vector<std::uint32_t>& pixels):size_(pixels.size()),
      background_(pixels.empty()?0:pixels[0]) {
    for(std::size_t offset=0;offset<size_;offset+=blockSize) {
      auto first=pixels.begin()+offset,last=first+std::min(blockSize,size_-offset);
      if(std::all_of(first,last,[this](std::uint32_t value){return value==background_;}))continue;
      auto block=std::make_shared<Block>(first,last);blocks_.emplace(offset/blockSize,block);block->ensure();
    }
  }
  LayerPixels(std::initializer_list<std::uint32_t> pixels):LayerPixels(std::vector<std::uint32_t>(pixels)){}
  LayerPixels(std::size_t size,std::uint32_t background):size_(size),background_(background) {}
  std::size_t size() const noexcept {return size_;}
  bool isUniform() const noexcept {return blocks_.empty();}
  std::uint32_t background() const noexcept {return background_;}
  std::size_t allocatedPixels() const noexcept {
    std::size_t count=0;for(const auto& [key,block]:blocks_) if(block)count+=block->size();return count;
  }
  struct ArchivedBlock {std::uint64_t key,offset;};
  struct Archived {
    std::uint64_t size;std::uint32_t background;
    std::vector<ArchivedBlock> blocks;
  };
  Archived archive(const std::shared_ptr<history::HistoryFile>& file) const {
    Archived result{size_,background_,{}};
    result.blocks.reserve(blocks_.size());
    for(const auto& [key,block]:blocks_) {
      if(block->archive.lock()!=file) {
        block->offset=file->append(block->data(),block->size()*sizeof(std::uint32_t));
        block->archive=file;
        block->backing=file;block->backingOffset=block->offset;
      }
      result.blocks.push_back({key,block->offset});
    }
    return result;
  }
  static LayerPixels restore(const Archived& saved,const std::shared_ptr<history::HistoryFile>& file) {
    LayerPixels result(saved.size,saved.background);
    for(const auto& entry:saved.blocks) {
      if(entry.key>= (saved.size+blockSize-1)/blockSize)throw std::runtime_error("History block");
      auto block=std::make_shared<Block>(std::min<std::uint64_t>(blockSize,saved.size-entry.key*blockSize));
      block->values.clear();block->values.shrink_to_fit();block->backing=file;block->backingOffset=entry.offset;
      block->archive=file;block->offset=entry.offset;
      result.blocks_.emplace(entry.key,std::move(block));
    }
    return result;
  }
  struct Reference {
    LayerPixels* owner;std::size_t index;
    operator std::uint32_t() const {return owner->get(index);}
    Reference& operator=(std::uint32_t value) {owner->put(index,value);return *this;}
    Reference& operator=(const Reference& value) {return *this=std::uint32_t(value);}
  };
  std::uint32_t& operator[](std::size_t i) {return mutablePixel(i);}
  std::uint32_t operator[](std::size_t i) const {return get(i);}
  template<bool Constant> struct Iterator {
    using Owner=std::conditional_t<Constant,const LayerPixels,LayerPixels>;
    using iterator_category=std::random_access_iterator_tag;
    using value_type=std::uint32_t;
    using difference_type=std::ptrdiff_t;
    using reference=std::conditional_t<Constant,std::uint32_t,std::uint32_t&>;
    using pointer=void;
    Owner* owner;difference_type index;
    reference operator*() const {return (*owner)[std::size_t(index)];}
    reference operator[](difference_type n) const {return (*owner)[std::size_t(index+n)];}
    Iterator& operator++(){++index;return *this;}
    Iterator operator++(int){auto old=*this;++*this;return old;}
    Iterator& operator--(){--index;return *this;}
    Iterator operator--(int){auto old=*this;--*this;return old;}
    Iterator& operator+=(difference_type n){index+=n;return *this;}
    Iterator& operator-=(difference_type n){index-=n;return *this;}
    Iterator operator+(difference_type n)const {return {owner,index+n};}
    Iterator operator-(difference_type n)const {return {owner,index-n};}
    difference_type operator-(Iterator other)const{return index-other.index;}
    bool operator==(Iterator other)const{return owner==other.owner&&index==other.index;}
    bool operator!=(Iterator other)const{return !(*this==other);}
    bool operator<(Iterator other)const{return index<other.index;}
    bool operator>(Iterator other)const{return index>other.index;}
    bool operator<=(Iterator other)const{return index<=other.index;}
    bool operator>=(Iterator other)const{return index>=other.index;}
    friend Iterator operator+(difference_type n,Iterator value){return value+n;}
  };
  Iterator<false> begin(){return {this,0};}
  Iterator<false> end(){return {this,std::ptrdiff_t(size_)};}
  Iterator<true> begin()const{return {this,0};}
  Iterator<true> end()const{return {this,std::ptrdiff_t(size_)};}
  operator std::vector<std::uint32_t>() const {
    std::vector<std::uint32_t> result;
    copyTo(result);
    return result;
  }
  void copyTo(std::vector<std::uint32_t>& result) const {
    result.assign(size_,background_);
    for(const auto& [key,block]:blocks_)if(block)
      std::copy(block->begin(),block->end(),result.begin()+key*blockSize);
  }
  // Bulk reads perform one map lookup per block, not one per pixel.
  void readSpan(std::size_t offset,std::size_t count,std::uint32_t* output) const {
    if(offset>size_ || count>size_-offset)throw std::out_of_range("Pixel span");
    while(count) {
      std::size_t key=offset/blockSize,inBlock=offset%blockSize;
      std::size_t length=std::min(count,blockSize-inBlock);
      auto found=blocks_.find(key);
      if(found==blocks_.end())std::fill_n(output,length,background_);
      else std::copy_n(found->second->begin()+inBlock,length,output);
      output+=length;offset+=length;count-=length;
    }
  }
  void writeSpan(std::size_t offset,std::size_t count,const std::uint32_t* input){
    if(offset>size_ || count>size_-offset)throw std::out_of_range("Pixel span");
    while(count){
      auto key=offset/blockSize,inBlock=offset%blockSize,length=std::min(count,blockSize-inBlock);
      auto found=blocks_.find(key);
      if(found==blocks_.end()){
        if(std::all_of(input,input+length,[this](std::uint32_t value){return value==background_;})){
          offset+=length;input+=length;count-=length;continue;
        }
        found=blocks_.emplace(key,std::make_shared<Block>(std::min(blockSize,size_-key*blockSize),background_)).first;
      }else if(found->second.use_count()>(found->second->listed?2:1))found->second=std::make_shared<Block>(*found->second);
      found->second->changed();std::copy_n(input,length,found->second->begin()+inBlock);
      offset+=length;input+=length;count-=length;
    }
  }
  LayerPixels& operator=(const std::vector<std::uint32_t>& pixels) {
    if(pixels.size()!=size_) {
      LayerPixels replacement(pixels);*this=std::move(replacement);return *this;
    }
    if(!pixels.empty() && std::all_of(pixels.begin(),pixels.end(),
        [color=pixels[0]](std::uint32_t value){return value==color;})) {
      LayerPixels replacement(pixels.size(),pixels[0]);*this=std::move(replacement);return *this;
    }
    // Preserve unchanged shared blocks; publish only after all allocations succeed.
    auto replacement=blocks_;
    for(std::size_t offset=0;offset<size_;offset+=blockSize) {
      auto first=pixels.begin()+offset,last=first+std::min(blockSize,size_-offset);
      auto found=blocks_.find(offset/blockSize);
      bool uniform=std::all_of(first,last,[this](std::uint32_t value){return value==background_;});
      if(uniform){replacement.erase(offset/blockSize);continue;}
      if(found!=blocks_.end() && std::equal(first,last,found->second->begin()))continue;
      replacement[offset/blockSize]=std::make_shared<Block>(first,last);replacement[offset/blockSize]->ensure();
    }
    blocks_=std::move(replacement);return *this;
  }
  friend bool operator==(const LayerPixels& a,const LayerPixels& b) {
    if(a.size_!=b.size_)return false;
    auto sameBlock=[&](std::size_t key){
      auto ai=a.blocks_.find(key),bi=b.blocks_.find(key);
      if(ai==a.blocks_.end() && bi==b.blocks_.end())return a.background_==b.background_;
      if(ai!=a.blocks_.end() && bi!=b.blocks_.end()){
        if(ai->second==bi->second)return true;
        return std::equal(ai->second->begin(),ai->second->end(),bi->second->begin());
      }
      const auto& block=ai==a.blocks_.end()?bi->second:ai->second;
      auto color=ai==a.blocks_.end()?a.background_:b.background_;
      return std::all_of(block->begin(),block->end(),[color](std::uint32_t value){return value==color;});
    };
    if(a.background_!=b.background_){
      for(std::size_t key=0;key<(a.size_+blockSize-1)/blockSize;key++)if(!sameBlock(key))return false;
    }else{
      for(const auto& [key,block]:a.blocks_)if(!sameBlock(key))return false;
      for(const auto& [key,block]:b.blocks_)if(!a.blocks_.contains(key) && !sameBlock(key))return false;
    }
    return true;
  }
  friend bool operator==(const LayerPixels& a,const std::vector<std::uint32_t>& b) {
    if(a.size_!=b.size())return false;
    for(std::size_t i=0;i<a.size_;++i)if(a.get(i)!=b[i])return false;
    return true;
  }
};
}
