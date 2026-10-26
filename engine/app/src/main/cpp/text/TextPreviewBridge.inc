#include <bit>
// Read immutable, bounded background/foreground planes for sharp vector text previews.
extern "C" JNIEXPORT jintArray JNICALL Java_art_velyntora_core_DrawingView_nativeTextPreviewStyle(JNIEnv* env,jclass){
 std::lock_guard<std::mutex> lock(guard);if(!placedPreview)return nullptr;
 std::vector<jint> values{placedPreview->mode,placedPreview->visible?1:0,jint(placedPreview->above.size())};
 for(const auto& layer:placedPreview->above){values.push_back(layer.mode);values.push_back(std::bit_cast<jint>(layer.opacity));}
 auto out=env->NewIntArray(values.size());if(out)env->SetIntArrayRegion(out,0,values.size(),values.data());return out;
}
extern "C" JNIEXPORT jintArray JNICALL Java_art_velyntora_core_DrawingView_nativeTextPreviewPlane(JNIEnv* env,jclass,jint index){
 std::lock_guard<std::mutex> lock(guard);if(!placedPreview || index<0 || std::size_t(index)>placedPreview->above.size())return nullptr;
 const auto& pixels=index==0?placedPreview->lower:placedPreview->above[index-1].pixels;
 auto out=env->NewIntArray(pixels.size());if(out)env->SetIntArrayRegion(out,0,pixels.size(),reinterpret_cast<const jint*>(pixels.data()));return out;
}
