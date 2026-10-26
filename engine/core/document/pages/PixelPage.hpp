#pragma once
#include "../history/HistoryFile.hpp"
#include <algorithm>
#include <list>
#include <memory>
#include <vector>
namespace velyntora::pages {
inline std::string directory="/tmp";
inline void configure(const std::string& path){directory=path;}
struct PixelPage;
struct PageCache {
  static constexpr std::size_t budget=16*1024*1024;
  std::list<std::shared_ptr<PixelPage>> order;
  std::size_t bytes=0;
  std::shared_ptr<history::HistoryFile> file;
  std::size_t fileBytes=0;
  void touch(const std::shared_ptr<PixelPage>& page);
  void discardUnused();
};
inline PageCache& cache(){static PageCache value;return value;}
struct PixelPage : std::enable_shared_from_this<PixelPage> {
  using Values=std::vector<std::uint32_t>;
  mutable Values values;
  std::size_t count;
  mutable std::shared_ptr<history::HistoryFile> backing;
  mutable std::uint64_t backingOffset=0;
  mutable std::weak_ptr<history::HistoryFile> archive;
  mutable std::uint64_t offset=0;
  mutable bool listed=false;
  mutable std::list<std::shared_ptr<PixelPage>>::iterator position;
  PixelPage(std::size_t n,std::uint32_t color=0):values(n,color),count(n){}
  template<class It> PixelPage(It first,It last):values(first,last),count(values.size()){}
  PixelPage(const PixelPage& other):values(other.begin(),other.end()),count(other.count),archive(other.archive),offset(other.offset){}
  std::size_t size()const noexcept{return count;}
  void ensure()const {
    if(values.empty()){
      Values loaded(count);backing->read(backingOffset,loaded.data(),count*4);values=std::move(loaded);
    }
    auto& c=cache();
    if(listed && position==c.order.begin())return;
    c.touch(std::const_pointer_cast<PixelPage>(shared_from_this()));
  }
  void changed(){ensure();archive.reset();backing.reset();}
  std::uint32_t& operator[](std::size_t i){ensure();return values[i];}
  const std::uint32_t& operator[](std::size_t i)const{ensure();return values[i];}
  Values::const_iterator begin()const{ensure();return values.begin();}
  Values::const_iterator end()const{ensure();return values.end();}
  Values::iterator begin(){ensure();return values.begin();}
  Values::iterator end(){ensure();return values.end();}
  auto data()const{ensure();return values.data();}
  auto data(){ensure();return values.data();}
  void evict(){
    if(!backing){
      if(auto saved=archive.lock()){backing=saved;backingOffset=offset;}
      else {
        auto& c=cache();if(!c.file || c.fileBytes+count*4>32*1024*1024){c.file=std::make_shared<history::HistoryFile>(directory);c.fileBytes=0;}
        backingOffset=c.file->append(values.data(),count*4);c.fileBytes+=count*4;backing=c.file;
      }
    }
    Values{}.swap(values);
  }
};
inline void PageCache::touch(const std::shared_ptr<PixelPage>& page){
  if(page->listed){order.splice(order.begin(),order,page->position);return;}
  order.push_front(page);page->position=order.begin();page->listed=true;bytes+=page->count*4;
  while(bytes>budget && order.size()>1){
    auto old=order.back();old->evict();old->listed=false;bytes-=old->count*4;order.pop_back();
  }
}
inline void PageCache::discardUnused(){
  for(auto it=order.begin();it!=order.end();){
    if(it->use_count()==1){(*it)->listed=false;bytes-=(*it)->count*4;it=order.erase(it);}else ++it;
  }
  if(order.empty()){file.reset();fileBytes=0;}
}
}
