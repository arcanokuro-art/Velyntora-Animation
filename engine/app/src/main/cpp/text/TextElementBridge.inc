// The text module reuses atomic object staging and tile composition, retaining typed text separately.
extern "C" JNIEXPORT jbyteArray JNICALL Java_art_velyntora_core_DrawingView_nativeTextContent(JNIEnv* env,jclass,jint index){
  std::lock_guard<std::mutex> lock(guard);if(!layers || index<0 || std::size_t(index)>=layers->layerCount() || !layers->layer(index).text)return nullptr;
  const auto& text=layers->layer(index).text->content;auto result=env->NewByteArray(text.size());if(result)env->SetByteArrayRegion(result,0,text.size(),reinterpret_cast<const jbyte*>(text.data()));return result;
}
extern "C" JNIEXPORT jintArray JNICALL Java_art_velyntora_core_DrawingView_nativeTextStyle(JNIEnv* env,jclass,jint index){
  std::lock_guard<std::mutex> lock(guard);if(!layers || index<0 || std::size_t(index)>=layers->layerCount() || !layers->layer(index).text)return nullptr;
  auto result=env->NewIntArray(5);if(result)env->SetIntArrayRegion(result,0,5,layers->layer(index).text->style.data());return result;
}
extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativePlacedTextElement(JNIEnv* env,jclass,jstring path,jfloatArray transform,jbyteArray content,jintArray style){
  std::lock_guard<std::mutex> lock(guard);if(!placedImage || !path || !transform || !content || !style || env->GetArrayLength(transform)!=9 || env->GetArrayLength(style)!=5)return JNI_FALSE;
  try{
    auto length=env->GetArrayLength(content);if(length<1 || length>16384)return JNI_FALSE;
    auto text=std::make_shared<velyntora::text::Element>();text->content.resize(length);env->GetByteArrayRegion(content,0,length,reinterpret_cast<jbyte*>(text->content.data()));env->GetIntArrayRegion(style,0,5,text->style.data());if(env->ExceptionCheck() || !text->valid())return JNI_FALSE;
    auto image=std::make_shared<velyntora::images::Element>();env->GetFloatArrayRegion(transform,0,9,image->transform.data());if(env->ExceptionCheck())return JNI_FALSE;
    const char* raw=env->GetStringUTFChars(path,nullptr);if(!raw)return JNI_FALSE;std::string file(raw);env->ReleaseStringUTFChars(path,raw);
    std::ifstream input(file,std::ios::binary|std::ios::ate);if(!input)return JNI_FALSE;auto bytes=input.tellg();if(bytes<=0 || bytes>velyntora::images::maxSourceBytes)return JNI_FALSE;input.seekg(0);
    image->source=std::make_shared<velyntora::images::Source>(std::uint32_t(bytes),[&](void* p,std::size_t n){if(!input.read(static_cast<char*>(p),n))throw std::runtime_error("Text glyph source");});
    placedImage->setActiveImage(std::move(image));placedImage->setActiveText(std::move(text));
    if(placedImageIndex<0)placedImage->renameLayer(placedImage->activeIndex(),"Texto");return JNI_TRUE;
  }catch(...){return JNI_FALSE;}
}
