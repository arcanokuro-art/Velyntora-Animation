// Exercise the exact JNI bridge used by the APK, without an Android runtime.
#include "../app/src/main/cpp/native_bridge.cpp"
#include <cassert>
#include <fcntl.h>
#include <cstdio>
int main(){
 historyDirectory="/tmp";
 assert(Java_art_velyntora_core_DrawingView_nativeCreate(nullptr,nullptr,800,800));
 assert(Java_art_velyntora_core_DrawingView_nativeAddLayer(nullptr,nullptr));
 Java_art_velyntora_core_DrawingView_nativeBeginEdit(nullptr,nullptr);
 Java_art_velyntora_core_DrawingView_nativeStroke(nullptr,nullptr,20,20,20,20,4,0x80123456);
 Java_art_velyntora_core_DrawingView_nativeSetLayerOpacity(nullptr,nullptr,.375f);
 auto expected=*layers;
 char path[]="/tmp/velyntora-project-XXXXXX";int fd=mkstemp(path);assert(fd>=0);unlink(path);
 assert(Java_art_velyntora_core_DrawingView_nativeSaveProject(nullptr,nullptr,fd));
 Java_art_velyntora_core_DrawingView_nativeClear(nullptr,nullptr);
 assert(layers->layerCount()==1);assert(lseek(fd,0,SEEK_SET)==0);
 assert(Java_art_velyntora_core_DrawingView_nativeOpenProject(nullptr,nullptr,fd));
 assert(layers->layerCount()==2&&layers->activeIndex()==1);assert(layers->flatten()==expected.flatten());
 assert(undoStack.empty()&&redoStack.empty());
 Java_art_velyntora_core_DrawingView_nativeBeginEdit(nullptr,nullptr);
 Java_art_velyntora_core_DrawingView_nativeStroke(nullptr,nullptr,30,30,30,30,4,0xffabcdef);
 assert(Java_art_velyntora_core_DrawingView_nativeUndo(nullptr,nullptr));assert(layers->flatten()==expected.flatten());
 assert(Java_art_velyntora_core_DrawingView_nativeRedo(nullptr,nullptr));
 auto edited=layers->flatten();auto history=undoStack.size();
 assert(ftruncate(fd,10)==0);assert(lseek(fd,0,SEEK_SET)==0);
 assert(!Java_art_velyntora_core_DrawingView_nativeOpenProject(nullptr,nullptr,fd));
 assert(layers->flatten()==edited&&undoStack.size()==history);
 assert(!Java_art_velyntora_core_DrawingView_nativeSaveProject(nullptr,nullptr,-1));close(fd);
 auto beforeResize=*layers;
 assert(Java_art_velyntora_core_DrawingView_nativeResizeDocument(nullptr,nullptr,240,120,true));
 assert(layers->width()==240&&layers->height()==120&&canvas->width()==240);
 assert(Java_art_velyntora_core_DrawingView_nativeUndo(nullptr,nullptr));
 assert(layers->width()==800&&canvas->width()==800&&layers->flatten()==beforeResize.flatten());
 assert(Java_art_velyntora_core_DrawingView_nativeRedo(nullptr,nullptr));
 assert(layers->width()==240&&canvas->height()==120);
 assert(Java_art_velyntora_core_DrawingView_nativeCropDocument(nullptr,nullptr,10,10,40,30));
 assert(layers->width()==40&&canvas->height()==30);
 assert(Java_art_velyntora_core_DrawingView_nativeUndo(nullptr,nullptr));assert(layers->width()==240);
 assert(!Java_art_velyntora_core_DrawingView_nativeResizeDocument(nullptr,nullptr,6001,6000,true));
 assert(layers->width()==240);

 assert(Java_art_velyntora_core_DrawingView_nativeCreate(nullptr,nullptr,4000,1000));
 for(int i=0;i<31;++i)assert(Java_art_velyntora_core_DrawingView_nativeAddLayer(nullptr,nullptr));
 assert(layers->layerCount()==32);assert(!Java_art_velyntora_core_DrawingView_nativeAddLayer(nullptr,nullptr));
 assert(layers->layerCount()==32);

 // Exact JNI paths: custom creation, persistence, resize/undo and boundary budget.
 assert(Java_art_velyntora_core_DrawingView_nativeCreate(nullptr,nullptr,2560,1600));
 Java_art_velyntora_core_DrawingView_nativeBeginEdit(nullptr,nullptr);
 Java_art_velyntora_core_DrawingView_nativeStyledStroke(nullptr,nullptr,1280.5,800.5,1280.5,800.5,4,0xff123456,1,1,false,false);
 assert(canvas->pixels()[800*2560+1280]==0xff123456);
 char largePath[]="/tmp/velyntora-large-XXXXXX";int largeFd=mkstemp(largePath);assert(largeFd>=0);unlink(largePath);
 assert(Java_art_velyntora_core_DrawingView_nativeSaveProject(nullptr,nullptr,largeFd));
 assert(Java_art_velyntora_core_DrawingView_nativeCreate(nullptr,nullptr,32,32));
 assert(lseek(largeFd,0,SEEK_SET)==0);assert(Java_art_velyntora_core_DrawingView_nativeOpenProject(nullptr,nullptr,largeFd));close(largeFd);
 assert(layers->width()==2560&&layers->height()==1600&&canvas->pixels()[800*2560+1280]==0xff123456);
 assert(Java_art_velyntora_core_DrawingView_nativeResizeDocument(nullptr,nullptr,2560,1700,false));
 assert(Java_art_velyntora_core_DrawingView_nativeUndo(nullptr,nullptr));assert(layers->height()==1600);
 assert(Java_art_velyntora_core_DrawingView_nativeCreate(nullptr,nullptr,3840,2160));
 assert(Java_art_velyntora_core_DrawingView_nativeCreate(nullptr,nullptr,4000,4000));
 assert(canvas->pixels().size()==16000000);
 assert(Java_art_velyntora_core_DrawingView_nativeCreate(nullptr,nullptr,6000,6000));
 assert(canvas->pixels().size()==36000000 && canvas->pixels().allocatedPixels()==0);
 assert(!Java_art_velyntora_core_DrawingView_nativeCreate(nullptr,nullptr,6001,1));
 assert(layers->width()==6000&&layers->height()==6000);
 assert(Java_art_velyntora_core_DrawingView_nativeCreate(nullptr,nullptr,32,32));
 assert(Java_art_velyntora_core_DrawingView_nativeCreate(nullptr,nullptr,6000,6000));
 auto initial=canvas->pixels();
 for(int i=0;i<140;++i){
   Java_art_velyntora_core_DrawingView_nativeBeginEdit(nullptr,nullptr);
   Java_art_velyntora_core_DrawingView_nativeStroke(nullptr,nullptr,32,32,32,32,2,0xff000000u|i);
 }
 assert(Java_art_velyntora_core_DrawingView_nativeHistoryAvailability(nullptr,nullptr)==1);
 auto final=canvas->pixels();assert(undoStack.size()==140);
 for(int i=0;i<140;++i)assert(Java_art_velyntora_core_DrawingView_nativeUndo(nullptr,nullptr));
 assert(Java_art_velyntora_core_DrawingView_nativeHistoryAvailability(nullptr,nullptr)==2);
 assert(!Java_art_velyntora_core_DrawingView_nativeUndo(nullptr,nullptr));
 assert(canvas->pixels()==initial && undoStack.empty());
 assert(!Java_art_velyntora_core_DrawingView_nativeUndo(nullptr,nullptr));
 for(int i=0;i<140;++i)assert(Java_art_velyntora_core_DrawingView_nativeRedo(nullptr,nullptr));
 assert(canvas->pixels()==final);
 assert(Java_art_velyntora_core_DrawingView_nativeUndo(nullptr,nullptr));
 Java_art_velyntora_core_DrawingView_nativeBeginEdit(nullptr,nullptr);
 Java_art_velyntora_core_DrawingView_nativeStroke(nullptr,nullptr,10,10,10,10,2,0xffabcdef);
 assert(redoStack.empty() && !Java_art_velyntora_core_DrawingView_nativeRedo(nullptr,nullptr));
}
