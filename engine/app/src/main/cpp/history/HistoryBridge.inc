extern "C" JNIEXPORT void JNICALL Java_art_velyntora_core_DrawingView_nativeBeginEdit(JNIEnv*,
                                                                                      jclass) {
  std::lock_guard<std::mutex> lock(guard);
  checkpoint();
}
extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeUndo(JNIEnv*,
                                                                                     jclass) {
  std::lock_guard<std::mutex> lock(guard);
  if (!canvas || undoStack.empty()) return JNI_FALSE;
  try {
  if(redoStack.size()==redoStack.capacity())redoStack.reserve(std::max<std::size_t>(16,redoStack.capacity()*2));
  auto previous=captureSnapshot();
  restore(undoStack.back());
  redoStack.push_back(std::move(previous));
  undoStack.pop_back();
  return JNI_TRUE;
  } catch(const std::exception&) {return JNI_FALSE;}
}
extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeRedo(JNIEnv*,
                                                                                     jclass) {
  std::lock_guard<std::mutex> lock(guard);
  if (!canvas || redoStack.empty()) return JNI_FALSE;
  try {
  if(undoStack.size()==undoStack.capacity())undoStack.reserve(std::max<std::size_t>(16,undoStack.capacity()*2));
  auto previous=captureSnapshot();
  restore(redoStack.back());
  undoStack.push_back(std::move(previous));
  redoStack.pop_back();
  return JNI_TRUE;
  } catch(const std::exception&) {return JNI_FALSE;}
}
extern "C" JNIEXPORT jint JNICALL Java_art_velyntora_core_DrawingView_nativeCurveTag(JNIEnv*,
                                                                                     jclass) {
  std::lock_guard<std::mutex> lock(guard);
  return curveTag;
}
extern "C" JNIEXPORT void JNICALL Java_art_velyntora_core_DrawingView_nativeMarkCurve(JNIEnv*,
                                                                                      jclass,
                                                                                      jint tag) {
  std::lock_guard<std::mutex> lock(guard);
  curveTag = tag;
}

extern "C" JNIEXPORT jlong JNICALL Java_art_velyntora_core_DrawingView_nativeRevision(JNIEnv*,
                                                                                      jclass) {
  std::lock_guard<std::mutex> lock(guard);
  return static_cast<jlong>(revision);
}


extern "C" JNIEXPORT void JNICALL Java_art_velyntora_core_DrawingView_nativeHistoryDirectory(JNIEnv* env,jclass,jstring directory) {
  std::lock_guard<std::mutex> lock(guard);
  const char* value=env->GetStringUTFChars(directory,nullptr);
  if(!value)return;
  historyDirectory=value;velyntora::pages::configure(historyDirectory);env->ReleaseStringUTFChars(directory,value);
}
