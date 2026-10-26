// Remove AI transaction boundary. Existing session/history and tool exports stay unchanged.
extern "C" JNIEXPORT jintArray JNICALL
Java_art_velyntora_core_DrawingView_nativeRemoveSource(JNIEnv* env,jclass) {
 std::lock_guard<std::mutex> lock(guard);
 if(!canvas||!layers)return nullptr;
 jintArray result=env->NewIntArray(static_cast<jsize>(canvas->pixels().size()));
 if(result){
   std::vector<std::uint32_t> block(65536);
   for(std::size_t offset=0;offset<canvas->pixels().size();offset+=block.size()){
     auto count=std::min(block.size(),canvas->pixels().size()-offset);
     canvas->pixels().readSpan(offset,count,block.data());
     env->SetIntArrayRegion(result,offset,count,reinterpret_cast<const jint*>(block.data()));
     if(env->ExceptionCheck())return nullptr;
   }
 }
 return result;
}
extern "C" JNIEXPORT jboolean JNICALL
Java_art_velyntora_core_DrawingView_nativeRemoveCommit(JNIEnv* env,jclass,jintArray source,jbyteArray mask,jint width,jint height,jint layer,jlong expectedRevision) {
 std::lock_guard<std::mutex> lock(guard);
 if(!canvas||!layers||!source||!mask||canvas->width()!=width||canvas->height()!=height||static_cast<int>(layers->activeIndex())!=layer||revision!=static_cast<std::uint64_t>(expectedRevision))return JNI_FALSE;
 const auto count=static_cast<jsize>(canvas->pixels().size());
 if(env->GetArrayLength(source)!=count||env->GetArrayLength(mask)!=count)return JNI_FALSE;
 try{
  std::vector<jint> data(count);std::vector<jbyte> region(count);
  env->GetIntArrayRegion(source,0,count,data.data());env->GetByteArrayRegion(mask,0,count,region.data());if(env->ExceptionCheck())return JNI_FALSE;
  auto composed=canvas->pixels();bool changed=false;
  for(jsize i=0;i<count;i++)if(region[i]){auto pixel=(composed[i]&0xff000000u)|(static_cast<std::uint32_t>(data[i])&0x00ffffffu);changed|=pixel!=composed[i];composed[i]=pixel;}
  if(changed){checkpoint();canvas->setPixels(std::move(composed));storeActive();}
  return JNI_TRUE;
 }catch(...){return JNI_FALSE;}
}
