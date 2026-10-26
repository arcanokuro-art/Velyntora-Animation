extern "C" JNIEXPORT jintArray JNICALL Java_art_velyntora_core_DrawingView_nativePixels(JNIEnv* env,
                                                                                        jclass) {
  std::lock_guard<std::mutex> lock(guard);
  if (!layers) return nullptr;
  const auto pixels = layers->flatten();
  jintArray result = env->NewIntArray(static_cast<jsize>(pixels.size()));
  if (result)
    env->SetIntArrayRegion(result, 0, static_cast<jsize>(pixels.size()),
                           reinterpret_cast<const jint*>(pixels.data()));
  return result;
}
extern "C" JNIEXPORT jintArray JNICALL Java_art_velyntora_core_DrawingView_nativeRegionPixels(
    JNIEnv* env,jclass,jint x,jint y,jint w,jint h) {
  std::lock_guard<std::mutex> lock(guard);
  if (!layers || x<0 || y<0 || w<=0 || h<=0 || x>layers->width()-w || y>layers->height()-h)
    return nullptr;
  try {
    auto pixels=layers->flattenRegion(x,y,w,h);
    auto result=env->NewIntArray(jsize(pixels.size()));
    if(result) env->SetIntArrayRegion(result,0,jsize(pixels.size()),
        reinterpret_cast<const jint*>(pixels.data()));
    return result;
  } catch (...) { return nullptr; }
}

extern "C" JNIEXPORT jintArray JNICALL Java_art_velyntora_core_DrawingView_nativeSampleRegion(JNIEnv* env,jclass,jint x,jint y,jint sample,jint w,jint h){
  std::unique_lock<std::mutex> lock(guard);
  if(!layers || x<0 || y<0 || sample<1 || sample>128 || w<1 || h<1 || std::int64_t(w)*h>1048576
      || std::int64_t(x)+(w-1LL)*sample>=layers->width() || std::int64_t(y)+(h-1LL)*sample>=layers->height())return nullptr;
  try {
    const bool bounded=std::int64_t(w)*h*sample*sample<=262144;
    std::unique_ptr<velyntora::LayerDocument> snapshot;
    if(!bounded){snapshot=std::make_unique<velyntora::LayerDocument>(*layers);lock.unlock();}
    const auto& document=bounded?*layers:*snapshot;
    std::vector<std::uint32_t> output(std::size_t(w)*h);
    bool uniform=true;for(std::size_t i=0;i<document.layerCount();++i)uniform=uniform && document.layer(i).pixels.isUniform();
    if(uniform){std::vector<std::uint32_t> pixel;if(bounded)pixel=document.flattenRegion(0,0,1,1);else {std::lock_guard<std::mutex> pageLock(guard);pixel=document.flattenRegion(0,0,1,1);}std::fill(output.begin(),output.end(),pixel[0]);}
    else if(sample==1){if(bounded)output=document.flattenRegion(x,y,w,h);else {std::lock_guard<std::mutex> pageLock(guard);output=document.flattenRegion(x,y,w,h);}}
    else for(int row=0;row<h;++row){
      int top=y+row*sample,rows=std::min(sample,document.height()-top),cols=std::min(w*sample,document.width()-x);
      std::vector<std::uint32_t> strip;
      if(bounded)strip=document.flattenRegion(x,top,cols,rows);else {std::lock_guard<std::mutex> pageLock(guard);strip=document.flattenRegion(x,top,cols,rows);}
      for(int col=0;col<w;++col){
        int columns=std::min(sample,cols-col*sample);std::uint64_t a=0,r=0,g=0,b=0;
        for(int sy=0;sy<rows;++sy)for(int sx=0;sx<columns;++sx){auto pixel=strip[std::size_t(sy)*cols+col*sample+sx];auto alpha=pixel>>24;
          a+=alpha;r+=((pixel>>16)&255)*alpha;g+=((pixel>>8)&255)*alpha;b+=(pixel&255)*alpha;}
        auto count=rows*columns;output[std::size_t(row)*w+col]=a?((std::uint32_t((a+count/2)/count)<<24)
          |(std::uint32_t((r+a/2)/a)<<16)|(std::uint32_t((g+a/2)/a)<<8)|std::uint32_t((b+a/2)/a)):0;
      }
    }
    auto result=env->NewIntArray(output.size());if(result)env->SetIntArrayRegion(result,0,output.size(),reinterpret_cast<const jint*>(output.data()));return result;
  }catch(...){return nullptr;}
}
