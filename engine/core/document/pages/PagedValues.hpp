#pragma once
#include "../tiles/LayerPixels.hpp"
#include <array>
#include <cstring>
#include <type_traits>
namespace velyntora::pages {
// Effect work buffers use the same disk-backed cache as document pixels.
template<class T> class PagedValues {
  static_assert(std::is_trivially_copyable_v<T> && sizeof(T)%4==0);
  static constexpr std::size_t words=sizeof(T)/4;
  tiles::LayerPixels storage_;
  std::size_t count_;
  T get(std::size_t i)const {
    std::array<std::uint32_t,words> bytes{};storage_.readSpan(i*words,words,bytes.data());
    T value;std::memcpy(&value,bytes.data(),sizeof(T));return value;
  }
  void set(std::size_t i,const T& value){
    std::array<std::uint32_t,words> bytes{};std::memcpy(bytes.data(),&value,sizeof(T));storage_.writeSpan(i*words,words,bytes.data());
  }
 public:
  explicit PagedValues(std::size_t count):storage_(count*words,0),count_(count){}
  std::size_t size()const noexcept{return count_;}
  void readSpan(std::size_t offset,std::size_t count,T* output)const{
    std::vector<std::uint32_t> bytes(count*words);storage_.readSpan(offset*words,bytes.size(),bytes.data());std::memcpy(output,bytes.data(),count*sizeof(T));
  }
  void writeSpan(std::size_t offset,std::size_t count,const T* input){
    std::vector<std::uint32_t> bytes(count*words);std::memcpy(bytes.data(),input,count*sizeof(T));storage_.writeSpan(offset*words,bytes.size(),bytes.data());
  }
  template<class Value> struct Component {
    PagedValues* owner;std::size_t index,channel;
    operator Value()const{return owner->get(index)[channel];}
    Component& operator=(Value value){auto item=owner->get(index);item[channel]=value;owner->set(index,item);return *this;}
  };
  struct Reference {
    PagedValues* owner;std::size_t index;
    operator T()const{return owner->get(index);}
    Reference& operator=(const T& value){owner->set(index,value);return *this;}
    Reference& operator=(const Reference& value){return *this=T(value);}
    auto operator[](std::size_t c){return Component<typename T::value_type>{owner,index,c};}
  };
  Reference operator[](std::size_t i){return {this,i};}
  T operator[](std::size_t i)const{return get(i);}
};
}
