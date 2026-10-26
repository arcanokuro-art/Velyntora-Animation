// Empty history is a normal boundary, distinct from a storage/rendering failure.
extern "C" JNIEXPORT jint JNICALL Java_art_velyntora_core_DrawingView_nativeHistoryAvailability(JNIEnv*,jclass) {
  std::lock_guard<std::mutex> lock(guard);
  return (canvas && !undoStack.empty()?1:0) | (canvas && !redoStack.empty()?2:0);
}
