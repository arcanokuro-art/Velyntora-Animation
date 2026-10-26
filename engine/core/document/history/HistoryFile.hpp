#pragma once
#include <cerrno>
#include <cstdint>
#include <cstdlib>
#include <stdexcept>
#include <string>
#include <unistd.h>
#include <vector>
namespace velyntora::history {
// Unlinked session file: storage is reclaimed even after an abnormal process exit.
class HistoryFile {
  int fd_;
  std::uint64_t end_=0;
 public:
  explicit HistoryFile(const std::string& directory) {
    auto path=directory+"/velyntora-history-XXXXXX";
    std::vector<char> name(path.begin(),path.end());name.push_back(0);
    fd_=mkstemp(name.data());
    if(fd_<0)throw std::runtime_error("Cannot create history storage");
    unlink(name.data());
  }
  ~HistoryFile(){close(fd_);}
  HistoryFile(const HistoryFile&)=delete;
  std::uint64_t append(const void* data,std::size_t bytes) {
    auto offset=end_;auto input=static_cast<const std::uint8_t*>(data);
    std::size_t done=0;
    while(done<bytes) {
      auto count=pwrite64(fd_,input+done,bytes-done,offset+done);
      if(count<0 && errno==EINTR)continue;
      if(count<=0)throw std::runtime_error("History write");
      done+=count;
    }
    end_+=bytes;return offset;
  }
  void read(std::uint64_t offset,void* data,std::size_t bytes) {
    auto output=static_cast<std::uint8_t*>(data);std::size_t done=0;
    while(done<bytes) {
      auto count=pread64(fd_,output+done,bytes-done,offset+done);
      if(count<0 && errno==EINTR)continue;
      if(count<=0)throw std::runtime_error("History read");
      done+=count;
    }
  }
  // Positional writes have no userspace buffering; history intentionally lasts one session.
  void flush() {}
};
}
