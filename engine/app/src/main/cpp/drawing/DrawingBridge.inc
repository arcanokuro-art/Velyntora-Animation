extern "C" JNIEXPORT void JNICALL Java_art_velyntora_core_DrawingView_nativeStroke(
    JNIEnv*, jclass, jfloat x0, jfloat y0, jfloat x1, jfloat y1, jfloat r, jint color) {
  std::lock_guard<std::mutex> lock(guard);
  if (canvas) {
    canvas->stroke(x0, y0, x1, y1, r, static_cast<std::uint32_t>(color));
    storeActive();
  }
}
extern "C" JNIEXPORT void JNICALL Java_art_velyntora_core_DrawingView_nativeShape(
    JNIEnv*, jclass, jint kind, jint x0, jint y0, jint x1, jint y1, jint color) {
  std::lock_guard<std::mutex> lock(guard);
  if (!canvas) return;
  if (kind == 1) canvas->rectangle(x0, y0, x1, y1, static_cast<std::uint32_t>(color), false);
  if (kind == 2) canvas->ellipse(x0, y0, x1, y1, static_cast<std::uint32_t>(color), false);
  if (kind == 3) canvas->stroke(x0, y0, x1, y1, 1.f, static_cast<std::uint32_t>(color));
  if (kind == 7) canvas->rectangle(x0, y0, x1, y1, static_cast<std::uint32_t>(color), true);
  if (kind == 8) canvas->ellipse(x0, y0, x1, y1, static_cast<std::uint32_t>(color), true);
  storeActive();
}
extern "C" JNIEXPORT void JNICALL Java_art_velyntora_core_DrawingView_nativeFill(JNIEnv*, jclass,
                                                                                 jint x, jint y,
                                                                                 jint color) {
  std::lock_guard<std::mutex> lock(guard);
  if (canvas) {
    canvas->fill(x, y, static_cast<std::uint32_t>(color));
    storeActive();
  }
}
extern "C" JNIEXPORT jint JNICALL Java_art_velyntora_core_DrawingView_nativePickColor(JNIEnv*,
                                                                                      jclass,
                                                                                      jint x,
                                                                                      jint y) {
  std::lock_guard<std::mutex> lock(guard);
  if (!layers || x < 0 || y < 0 || x >= layers->width() || y >= layers->height()) return 0;
  const auto pixels=layers->flattenRegion(x,y,1,1);
  return static_cast<jint>(pixels[0]);
}
extern "C" JNIEXPORT void JNICALL Java_art_velyntora_core_DrawingView_nativeStyledStroke(
    JNIEnv*, jclass, jfloat x0, jfloat y0, jfloat x1, jfloat y1, jfloat radius, jint color,
    jfloat opacity, jfloat hardness, jboolean square, jboolean eraser) {
  std::lock_guard<std::mutex> lock(guard);
  if (canvas) {
    canvas->strokeStyled(x0, y0, x1, y1, radius, static_cast<std::uint32_t>(color), opacity,
                         hardness, square, eraser,
                         brushMask.size() == canvas->pixels().size() ? &brushMask : nullptr);
    storeStrokeRegion(x0,y0,x1,y1,radius);
  }
}

extern "C" JNIEXPORT void JNICALL Java_art_velyntora_core_DrawingView_nativeBeginSampled(JNIEnv*,
                                                                                         jclass,
                                                                                         jint x,
                                                                                         jint y) {
  std::lock_guard<std::mutex> lock(guard);
  sampledSource.clear();
  if (!canvas) return;
  sampledSource = canvas->pixels();
  sampledTarget = (x >= 0 && y >= 0 && x < canvas->width() && y < canvas->height())
                      ? sampledSource[y * canvas->width() + x]
                      : 0;
}
extern "C" JNIEXPORT void JNICALL Java_art_velyntora_core_DrawingView_nativeSampledStroke(
    JNIEnv*, jclass, jboolean clone, jint ox, jint oy, jint replacement, jint tolerance, jfloat x0,
    jfloat y0, jfloat x1, jfloat y1, jfloat radius, jfloat opacity, jfloat hardness) {
  std::lock_guard<std::mutex> lock(guard);
  if (!canvas) return;
  canvas->sampledStroke(sampledSource, clone, ox, oy, sampledTarget, replacement, tolerance, x0, y0,
                        x1, y1, radius, opacity, hardness,
                        brushMask.size() == canvas->pixels().size() ? &brushMask : nullptr);
  storeStrokeRegion(x0,y0,x1,y1,radius);
}

