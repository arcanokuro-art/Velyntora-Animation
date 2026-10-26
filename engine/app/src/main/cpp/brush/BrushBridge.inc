extern "C" JNIEXPORT void JNICALL Java_art_velyntora_core_DrawingView_nativeAntialiasedStroke(
    JNIEnv*,jclass,jfloat x0,jfloat y0,jfloat x1,jfloat y1,jfloat radius,jint color,
    jfloat opacity,jfloat hardness,jboolean square) {
  std::lock_guard<std::mutex> lock(guard);
  if (!canvas) return;
  canvas->strokeAntialiased(x0,y0,x1,y1,radius,static_cast<std::uint32_t>(color),opacity,hardness,square,
      brushMask.size()==canvas->pixels().size()?&brushMask:nullptr);
  storeStrokeRegion(x0,y0,x1,y1,radius+1);
}
