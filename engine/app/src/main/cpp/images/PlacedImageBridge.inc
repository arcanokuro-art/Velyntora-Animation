#include <fstream>
#include "../../../../../core/components/layers/blending/BlendModes.hpp"
// Isolated staging: no live document/history mutation before an accepted commit.
namespace {
std::unique_ptr<velyntora::LayerDocument> placedImage;
std::uint64_t placedImageRevision=0;
int placedImageIndex=-1;
}
extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeBeginPlacedImage(JNIEnv* env,jclass,jstring name,jfloat opacity){
  std::lock_guard<std::mutex> lock(guard);
  if(!layers || !canvas || placedImage || layers->layerCount()>=32 || !name || !std::isfinite(opacity) || opacity<0 || opacity>1 || env->GetStringUTFLength(name)>4096)return JNI_FALSE;
  const char* text=env->GetStringUTFChars(name,nullptr);if(!text)return JNI_FALSE;
  try{
    auto next=std::make_unique<velyntora::LayerDocument>(*layers);next->selectLayer(next->layerCount()-1);next->addLayer(text);next->setOpacity(next->activeIndex(),opacity);
    placedImage=std::move(next);placedImageIndex=-1;placedImageRevision=revision;env->ReleaseStringUTFChars(name,text);return JNI_TRUE;
  }catch(...){env->ReleaseStringUTFChars(name,text);return JNI_FALSE;}
}
extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativePlacedImageTile(JNIEnv* env,jclass,jint x,jint y,jint w,jint h,jintArray data){
  std::lock_guard<std::mutex> lock(guard);
  if(!placedImage || !data || w<1 || h<1 || std::int64_t(w)*h>1048576 || x<0 || y<0 || std::int64_t(x)+w>placedImage->width() || std::int64_t(y)+h>placedImage->height() || env->GetArrayLength(data)!=std::int64_t(w)*h)return JNI_FALSE;
  try{std::vector<std::uint32_t> pixels(std::size_t(w)*h);env->GetIntArrayRegion(data,0,pixels.size(),reinterpret_cast<jint*>(pixels.data()));if(env->ExceptionCheck())return JNI_FALSE;for(int row=0;row<h;row++)placedImage->writeActiveSpan(std::size_t(y+row)*placedImage->width()+x,w,pixels.data()+std::size_t(row)*w);return JNI_TRUE;}catch(...){return JNI_FALSE;}
}
extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeCommitPlacedImage(JNIEnv*,jclass){
  std::lock_guard<std::mutex> lock(guard);
  if(!placedImage || !layers || !placedImage->layer(placedImage->activeIndex()).image || revision!=placedImageRevision)return JNI_FALSE;
  try{
    auto next=std::make_unique<velyntora::Canvas>(placedImage->width(),placedImage->height());next->setPixels(placedImage->layer(placedImage->activeIndex()).pixels);
    checkpoint();layers.swap(placedImage);canvas.swap(next);placedImage.reset();sampledSource.clear();return JNI_TRUE;
  }catch(...){return JNI_FALSE;}
}
extern "C" JNIEXPORT void JNICALL Java_art_velyntora_core_DrawingView_nativeCancelPlacedImage(JNIEnv*,jclass){std::lock_guard<std::mutex> lock(guard);placedImage.reset();}

extern "C" JNIEXPORT void JNICALL Java_art_velyntora_core_DrawingView_nativeImageStorage(JNIEnv* env,jclass,jstring path){
  std::lock_guard<std::mutex> lock(guard);if(!path)return;const char* p=env->GetStringUTFChars(path,nullptr);if(p){velyntora::images::storageDirectory=p;env->ReleaseStringUTFChars(path,p);}
}
extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeLayerIsImage(JNIEnv*,jclass,jint index){
  std::lock_guard<std::mutex> lock(guard);return layers && index>=0 && std::size_t(index)<layers->layerCount() && bool(layers->layer(index).image);
}
extern "C" JNIEXPORT jfloatArray JNICALL Java_art_velyntora_core_DrawingView_nativeImageTransform(JNIEnv* env,jclass,jint index){
  std::lock_guard<std::mutex> lock(guard);if(!layers || index<0 || std::size_t(index)>=layers->layerCount() || !layers->layer(index).image)return nullptr;
  auto values=layers->layer(index).image->transform;auto out=env->NewFloatArray(9);if(out)env->SetFloatArrayRegion(out,0,9,values.data());return out;
}
extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeBeginPlacedImageEdit(JNIEnv*,jclass,jint index,jfloat opacity){
  std::lock_guard<std::mutex> lock(guard);if(!layers || placedImage || index<0 || std::size_t(index)>=layers->layerCount() || !layers->layer(index).image || !std::isfinite(opacity) || opacity<0 || opacity>1)return JNI_FALSE;
  try{auto next=std::make_unique<velyntora::LayerDocument>(*layers);next->selectLayer(index);next->replaceActivePixels(velyntora::tiles::LayerPixels(std::size_t(next->width())*next->height(),0));next->setOpacity(index,opacity);placedImage=std::move(next);placedImageIndex=index;placedImageRevision=revision;return JNI_TRUE;}catch(...){return JNI_FALSE;}
}
extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativePlacedImageElement(JNIEnv* env,jclass,jstring path,jfloatArray values){
  std::lock_guard<std::mutex> lock(guard);if(!placedImage || !path || !values || env->GetArrayLength(values)!=9)return JNI_FALSE;
  try{auto image=std::make_shared<velyntora::images::Element>();env->GetFloatArrayRegion(values,0,9,image->transform.data());if(env->ExceptionCheck())return JNI_FALSE;
    if(placedImageIndex>=0)image->source=layers->layer(placedImageIndex).image->source;
    else{const char* name=env->GetStringUTFChars(path,nullptr);if(!name)return JNI_FALSE;std::string file(name);env->ReleaseStringUTFChars(path,name);
      std::ifstream input(file,std::ios::binary|std::ios::ate);if(!input)return JNI_FALSE;auto size=input.tellg();if(size<=0 || size>velyntora::images::maxSourceBytes)return JNI_FALSE;input.seekg(0);
      image->source=std::make_shared<velyntora::images::Source>(std::uint32_t(size),[&](void* data,std::size_t n){if(!input.read(static_cast<char*>(data),n))throw std::runtime_error("Image read");});}
    placedImage->setActiveImage(std::move(image));return JNI_TRUE;
  }catch(...){return JNI_FALSE;}
}
namespace {
struct PlacedPreview {
  int w,h,sample,index,mode;bool visible;
  std::vector<std::uint32_t> lower;
  struct Above {std::vector<std::uint32_t> pixels;float opacity;int mode;};std::vector<Above> above;
};
std::shared_ptr<const PlacedPreview> placedPreview;
}
extern "C" JNIEXPORT jintArray JNICALL Java_art_velyntora_core_DrawingView_nativePrepareImageEdit(JNIEnv* env,jclass,jint index,jint fd){
  std::unique_lock<std::mutex> lock(guard);if(!layers || index<0 || std::size_t(index)>=layers->layerCount() || !layers->layer(index).image || fd<0)return nullptr;
  try{
    auto document=std::make_unique<velyntora::LayerDocument>(*layers);auto expectedRevision=revision;lock.unlock();
    document->layer(index).image->source->write([&](const void* bytes,std::size_t n){auto p=static_cast<const char*>(bytes);while(n){auto done=write(fd,p,n);if(done<0 && errno==EINTR)continue;if(done<=0)throw std::runtime_error("Image restore");p+=done;n-=done;}});
    auto next=std::make_shared<PlacedPreview>();next->sample=1;next->index=index;next->mode=document->layer(index).blendMode;next->visible=document->layer(index).visible;
    int width=document->width(),height=document->height();
    while(std::int64_t((width+next->sample-1)/next->sample)*((height+next->sample-1)/next->sample)*std::max<std::size_t>(8,document->layerCount())>2097152)next->sample*=2;
    next->w=(width+next->sample-1)/next->sample;next->h=(height+next->sample-1)/next->sample;next->lower.resize(std::size_t(next->w)*next->h,0);
    for(std::size_t i=0;i<document->layerCount();i++){
      const auto& layer=document->layer(i);if(i==std::size_t(index) || !layer.visible || layer.opacity<=0)continue;
      std::vector<std::uint32_t> pixels(next->lower.size());std::vector<std::uint32_t> row(width);
      // Area average with premultiplied alpha; no canvas-sized bitmap.
      if(layer.pixels.isUniform())std::fill(pixels.begin(),pixels.end(),layer.pixels.background());
      else for(int y=0;y<next->h;y++){
        std::vector<std::array<std::uint64_t,4>> sums(next->w);
        int rows=std::min(next->sample,height-y*next->sample);
        for(int dy=0;dy<rows;dy++){{std::lock_guard<std::mutex> pageLock(guard);layer.pixels.readSpan(std::size_t(y*next->sample+dy)*width,width,row.data());}
          for(int x=0;x<width;x++){auto p=row[x],a=p>>24;auto& s=sums[x/next->sample];s[0]+=a;s[1]+=((p>>16)&255)*a;s[2]+=((p>>8)&255)*a;s[3]+=(p&255)*a;}}
        for(int x=0;x<next->w;x++){auto s=sums[x];auto n=rows*std::min(next->sample,width-x*next->sample);pixels[std::size_t(y)*next->w+x]=s[0]?((std::uint32_t((s[0]+n/2)/n)<<24)|(std::uint32_t((s[1]+s[0]/2)/s[0])<<16)|(std::uint32_t((s[2]+s[0]/2)/s[0])<<8)|std::uint32_t((s[3]+s[0]/2)/s[0])):0;}
      }
      if(i<std::size_t(index)){for(std::size_t p=0;p<pixels.size();p++)next->lower[p]=velyntora::blending::composite(pixels[p],next->lower[p],layer.opacity,layer.blendMode);}
      else next->above.push_back({std::move(pixels),layer.opacity,layer.blendMode});
    }
    lock.lock();if(!layers || revision!=expectedRevision)return nullptr;
    jint info[]={next->w,next->h,next->sample};auto out=env->NewIntArray(3);if(out)env->SetIntArrayRegion(out,0,3,info);placedPreview=std::move(next);return out;
  }catch(...){return nullptr;}
}
extern "C" JNIEXPORT jintArray JNICALL Java_art_velyntora_core_DrawingView_nativeComposeImageEdit(JNIEnv* env,jclass,jintArray moving,jfloat opacity){
  std::shared_ptr<const PlacedPreview> preview;{std::lock_guard<std::mutex> lock(guard);preview=placedPreview;}
  if(!preview || !moving || !std::isfinite(opacity) || opacity<0 || opacity>1 || env->GetArrayLength(moving)!=jsize(preview->lower.size()))return nullptr;
  try{std::vector<jint> pixels(preview->lower.size());env->GetIntArrayRegion(moving,0,pixels.size(),pixels.data());if(env->ExceptionCheck())return nullptr;
    for(std::size_t p=0;p<pixels.size();p++){auto value=preview->visible?velyntora::blending::composite(std::uint32_t(pixels[p]),preview->lower[p],opacity,preview->mode):preview->lower[p];
      for(const auto& layer:preview->above)value=velyntora::blending::composite(layer.pixels[p],value,layer.opacity,layer.mode);pixels[p]=jint(value);}
    auto out=env->NewIntArray(pixels.size());if(out)env->SetIntArrayRegion(out,0,pixels.size(),pixels.data());return out;
  }catch(...){return nullptr;}
}
extern "C" JNIEXPORT void JNICALL Java_art_velyntora_core_DrawingView_nativeCloseImageEdit(JNIEnv*,jclass){std::lock_guard<std::mutex> lock(guard);placedPreview.reset();}

extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeRasterizeImage(JNIEnv*,jclass,jint index){
  std::lock_guard<std::mutex> lock(guard);if(!layers || index<0 || std::size_t(index)>=layers->layerCount() || placedImage)return JNI_FALSE;
  if(!layers->layer(index).image)return JNI_TRUE;
  try{checkpoint();auto active=layers->activeIndex();layers->selectLayer(index);layers->setActiveImage(nullptr);layers->selectLayer(active);return JNI_TRUE;}catch(...){return JNI_FALSE;}
}
