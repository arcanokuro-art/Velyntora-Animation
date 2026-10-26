extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeBeginOpacityStroke(
    JNIEnv*, jclass, jboolean grouped) {
  std::lock_guard<std::mutex> lock(guard);
  if (!canvas) return JNI_FALSE;
  try {
    if (grouped) canvas->beginOpacityStroke();
    else canvas->endOpacityStroke();
    return JNI_TRUE;
  } catch (const std::exception&) {
    canvas->endOpacityStroke();
    return JNI_FALSE;
  }
}
extern "C" JNIEXPORT void JNICALL Java_art_velyntora_core_DrawingView_nativeEndOpacityStroke(
    JNIEnv*, jclass) {
  std::lock_guard<std::mutex> lock(guard);
  if (canvas) canvas->endOpacityStroke();
  sampledSource.clear();
}
