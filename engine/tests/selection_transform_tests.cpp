#include "velyntora/SelectionTransform.hpp"
#include <cassert>
#include <stdexcept>
using namespace velyntora;
int main(){
 std::vector<std::uint32_t> src(25,0);src[11]=0xffff0000;src[12]=0xff00ff00;src[13]=0xff0000ff;std::vector<std::uint8_t> mask(3,1);
 assert(transformSelection(src,5,5,1,2,3,1,mask,0,1,1)==src);
 auto flipped=transformSelection(src,5,5,1,2,3,1,mask,0,-1,1);assert(flipped[11]==src[13]&&flipped[13]==src[11]&&flipped[12]==src[12]);
 auto rotated=transformSelection(src,5,5,1,2,3,1,mask,90,1,1);assert(rotated[7]==src[11]&&rotated[12]==src[12]&&rotated[17]==src[13]);assert(rotated[11]==0&&rotated[13]==0);
 mask[1]=0;src[12]=0xffaabbcc;auto masked=transformSelection(src,5,5,1,2,3,1,mask,90,1,1);assert(masked[12]==src[12]&&masked[7]==src[11]);
 bool failed=false;try{transformSelection(src,5,5,1,2,3,1,mask,0,0,1);}catch(const std::invalid_argument&){failed=true;}assert(failed);
 auto scaled=transformSelection(src,5,5,1,2,3,1,mask,0,2,2);assert(scaled.size()==src.size());
}
