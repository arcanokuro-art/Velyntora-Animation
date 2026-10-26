extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeMovePixels(
    JNIEnv*, jclass, jint kind, jint x0, jint y0, jint x1, jint y1, jint dx, jint dy) {
  std::lock_guard<std::mutex> lock(guard);
  if (!canvas || !layers || (kind != 9 && kind != 10)) return JNI_FALSE;
  const int left = std::max(0, std::min(x0, x1)),
            right = std::min(canvas->width(), std::max(x0, x1));
  const int top = std::max(0, std::min(y0, y1)),
            bottom = std::min(canvas->height(), std::max(y0, y1));
  if (left >= right || top >= bottom || (!dx && !dy)) return JNI_FALSE;
  const int w = right - left, h = bottom - top;
  const double cx = (left + right) / 2.0, cy = (top + bottom) / 2.0, rx = w / 2.0, ry = h / 2.0;
  const auto before = canvas->pixels();
  auto after = before;
  for (int py = top; py < bottom; ++py)
    for (int px = left; px < right; ++px) {
      if (kind == 10 && ((px + 0.5 - cx) * (px + 0.5 - cx) / (rx * rx) +
                             (py + 0.5 - cy) * (py + 0.5 - cy) / (ry * ry) >
                         1.0))
        continue;
      after[static_cast<std::size_t>(py) * canvas->width() + px] = 0u;
    }
  for (int py = top; py < bottom; ++py)
    for (int px = left; px < right; ++px) {
      if (kind == 10 && ((px + 0.5 - cx) * (px + 0.5 - cx) / (rx * rx) +
                             (py + 0.5 - cy) * (py + 0.5 - cy) / (ry * ry) >
                         1.0))
        continue;
      const std::int64_t tx = static_cast<std::int64_t>(px) + dx,
                         ty = static_cast<std::int64_t>(py) + dy;
      if (tx < 0 || ty < 0 || tx >= canvas->width() || ty >= canvas->height()) continue;
      after[static_cast<std::size_t>(ty) * canvas->width() + tx] =
          before[static_cast<std::size_t>(py) * canvas->width() + px];
    }
  checkpoint();
  canvas->setPixels(after);
  storeActive();
  return JNI_TRUE;
}
extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativePasteSelection(
    JNIEnv* env, jclass, jintArray source, jint x, jint y) {
  std::lock_guard<std::mutex> lock(guard);
  if (!canvas || !layers || !source) return JNI_FALSE;
  const jsize count = env->GetArrayLength(source);
  if (count < 3) return JNI_FALSE;
  jint dims[2] = {0, 0};
  env->GetIntArrayRegion(source, 0, 2, dims);
  if (env->ExceptionCheck()) return JNI_FALSE;
  const int w = dims[0], h = dims[1];
  if (w <= 0 || h <= 0 || static_cast<std::int64_t>(w) * h != static_cast<std::int64_t>(count) - 2)
    return JNI_FALSE;
  if (x >= canvas->width() || y >= canvas->height() || static_cast<std::int64_t>(x) + w <= 0 ||
      static_cast<std::int64_t>(y) + h <= 0)
    return JNI_FALSE;
  std::vector<jint> data(static_cast<std::size_t>(count));
  env->GetIntArrayRegion(source, 0, count, data.data());
  if (env->ExceptionCheck()) return JNI_FALSE;
  auto pixels = canvas->pixels();
  const auto original = pixels;
  for (int py = std::max(0, -y); py < h && static_cast<std::int64_t>(y) + py < canvas->height();
       ++py) {
    for (int px = std::max(0, -x); px < w && static_cast<std::int64_t>(x) + px < canvas->width();
         ++px) {
      const std::uint32_t src =
          static_cast<std::uint32_t>(data[2 + static_cast<std::size_t>(py) * w + px]);
      const std::uint32_t sa = src >> 24;
      if (sa == 0) continue;
      const std::size_t pos = static_cast<std::size_t>(y + py) * canvas->width() + (x + px);
      if (sa == 255) {
        pixels[pos] = src;
        continue;
      }
      const std::uint32_t dst = pixels[pos], da = dst >> 24;
      const std::uint32_t outA = sa + (da * (255 - sa) + 127) / 255;
      if (outA == 0) {
        pixels[pos] = 0;
        continue;
      }
      std::uint32_t rgb = 0;
      for (int shift : {16, 8, 0}) {
        const std::uint32_t sc = (src >> shift) & 255, dc = (dst >> shift) & 255;
        const std::uint32_t numerator = sc * sa * 255 + dc * da * (255 - sa);
        const std::uint32_t denominator = outA * 255;
        const std::uint32_t value = std::min(255u, (numerator + denominator / 2) / denominator);
        rgb |= value << shift;
      }
      pixels[pos] = (outA << 24) | rgb;
    }
  }
  if (pixels == original) return JNI_FALSE;
  checkpoint();
  canvas->setPixels(pixels);
  storeActive();
  return JNI_TRUE;
}
extern "C" JNIEXPORT jintArray JNICALL Java_art_velyntora_core_DrawingView_nativeCopySelection(
    JNIEnv* env, jclass, jint kind, jint x0, jint y0, jint x1, jint y1) {
  std::lock_guard<std::mutex> lock(guard);
  if (!canvas || !layers || !(kind == 9 || kind == 10)) return nullptr;
  const int left = std::max(0, std::min(x0, x1)),
            right = std::min(canvas->width(), std::max(x0, x1));
  const int top = std::max(0, std::min(y0, y1)),
            bottom = std::min(canvas->height(), std::max(y0, y1));
  if (left >= right || top >= bottom) return nullptr;
  const int width = right - left, height = bottom - top;
  if (width > 8000 || height > 8000) return nullptr;
  std::vector<jint> data(static_cast<std::size_t>(width) * height + 2, 0);
  data[0] = width;
  data[1] = height;
  const auto& pixels = canvas->pixels();
  const double cx = (left + right) / 2.0, cy = (top + bottom) / 2.0;
  const double rx = width / 2.0, ry = height / 2.0;
  for (int py = top; py < bottom; ++py)
    for (int px = left; px < right; ++px) {
      if (kind == 9 || ((px + 0.5 - cx) * (px + 0.5 - cx) / (rx * rx) +
                            (py + 0.5 - cy) * (py + 0.5 - cy) / (ry * ry) <=
                        1.0))
        data[2 + static_cast<std::size_t>(py - top) * width + (px - left)] =
            static_cast<jint>(pixels[static_cast<std::size_t>(py) * canvas->width() + px]);
    }
  jintArray result = env->NewIntArray(static_cast<jsize>(data.size()));
  if (result) env->SetIntArrayRegion(result, 0, static_cast<jsize>(data.size()), data.data());
  return result;
}
extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeEraseSelection(
    JNIEnv*, jclass, jint kind, jint x0, jint y0, jint x1, jint y1) {
  std::lock_guard<std::mutex> lock(guard);
  if (!canvas || !layers || !(kind == 9 || kind == 10)) return JNI_FALSE;
  const int left = std::max(0, std::min(x0, x1)),
            right = std::min(canvas->width(), std::max(x0, x1));
  const int top = std::max(0, std::min(y0, y1)),
            bottom = std::min(canvas->height(), std::max(y0, y1));
  if (left >= right || top >= bottom) return JNI_FALSE;
  checkpoint();
  auto pixels = canvas->pixels();
  const double cx = (left + right) / 2.0, cy = (top + bottom) / 2.0;
  const double rx = (right - left) / 2.0, ry = (bottom - top) / 2.0;
  for (int py = top; py < bottom; ++py)
    for (int px = left; px < right; ++px) {
      if (kind == 9 || ((px + 0.5 - cx) * (px + 0.5 - cx) / (rx * rx) +
                            (py + 0.5 - cy) * (py + 0.5 - cy) / (ry * ry) <=
                        1.0))
        pixels[static_cast<std::size_t>(py) * canvas->width() + px] = 0u;
    }
  canvas->setPixels(pixels);
  storeActive();
  return JNI_TRUE;
}
extern "C" JNIEXPORT jboolean JNICALL
Java_art_velyntora_core_DrawingView_nativeEraseMaskedSelection(JNIEnv* env, jclass, jint x, jint y,
                                                               jint w, jint h, jbyteArray mask) {
  std::lock_guard<std::mutex> lock(guard);
  if (!canvas || !layers || !mask || w <= 0 || h <= 0 || x < 0 || y < 0) return JNI_FALSE;
  if (static_cast<std::int64_t>(x) + w > canvas->width() ||
      static_cast<std::int64_t>(y) + h > canvas->height())
    return JNI_FALSE;
  if (static_cast<std::int64_t>(w) * h != env->GetArrayLength(mask)) return JNI_FALSE;
  std::vector<jbyte> bits(static_cast<std::size_t>(w) * h);
  env->GetByteArrayRegion(mask, 0, static_cast<jsize>(bits.size()), bits.data());
  if (env->ExceptionCheck()) return JNI_FALSE;
  if (std::none_of(bits.begin(), bits.end(), [](jbyte bit) { return bit != 0; })) return JNI_FALSE;
  auto pixels = canvas->pixels();
  for (int row = 0; row < h; ++row)
    for (int col = 0; col < w; ++col)
      if (bits[static_cast<std::size_t>(row) * w + col])
        pixels[static_cast<std::size_t>(y + row) * canvas->width() + x + col] = 0u;
  checkpoint();
  canvas->setPixels(pixels);
  storeActive();
  return JNI_TRUE;
}
extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeMoveMaskedSelection(
    JNIEnv* env, jclass, jint x, jint y, jint w, jint h, jbyteArray mask, jint dx, jint dy) {
  std::lock_guard<std::mutex> lock(guard);
  if (!canvas || !layers || !mask || w <= 0 || h <= 0 || x < 0 || y < 0 || (!dx && !dy))
    return JNI_FALSE;
  if (static_cast<std::int64_t>(x) + w > canvas->width() ||
      static_cast<std::int64_t>(y) + h > canvas->height())
    return JNI_FALSE;
  if (static_cast<std::int64_t>(x) + dx < 0 || static_cast<std::int64_t>(y) + dy < 0 ||
      static_cast<std::int64_t>(x) + w + dx > canvas->width() ||
      static_cast<std::int64_t>(y) + h + dy > canvas->height())
    return JNI_FALSE;
  if (static_cast<std::int64_t>(w) * h != env->GetArrayLength(mask)) return JNI_FALSE;
  std::vector<jbyte> bits(static_cast<std::size_t>(w) * h);
  env->GetByteArrayRegion(mask, 0, static_cast<jsize>(bits.size()), bits.data());
  if (env->ExceptionCheck()) return JNI_FALSE;
  if (std::none_of(bits.begin(), bits.end(), [](jbyte bit) { return bit != 0; })) return JNI_FALSE;
  const auto before = canvas->pixels();
  auto after = before;
  for (int row = 0; row < h; ++row)
    for (int col = 0; col < w; ++col)
      if (bits[static_cast<std::size_t>(row) * w + col])
        after[static_cast<std::size_t>(y + row) * canvas->width() + x + col] = 0u;
  for (int row = 0; row < h; ++row)
    for (int col = 0; col < w; ++col)
      if (bits[static_cast<std::size_t>(row) * w + col])
        after[static_cast<std::size_t>(y + row + dy) * canvas->width() + x + col + dx] =
            before[static_cast<std::size_t>(y + row) * canvas->width() + x + col];
  checkpoint();
  canvas->setPixels(after);
  storeActive();
  return JNI_TRUE;
}
extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeCropActiveSelection(
    JNIEnv*, jclass, jint left, jint top, jint right, jint bottom) {
  std::lock_guard<std::mutex> lock(guard);
  if (!canvas || !layers) return JNI_FALSE;
  const int w = canvas->width(), h = canvas->height();
  if (left < 0 || top < 0 || right > w || bottom > h || left >= right || top >= bottom)
    return JNI_FALSE;
  if (left == 0 && top == 0 && right == w && bottom == h) return JNI_FALSE;
  auto pixels = canvas->pixels();
  bool changed = false;
  for (int y = 0; y < h; ++y)
    for (int x = 0; x < w; ++x) {
      if (x >= left && x < right && y >= top && y < bottom) continue;
      auto& pixel = pixels[static_cast<std::size_t>(y) * w + x];
      if (pixel != 0u) {
        pixel = 0u;
        changed = true;
      }
    }
  if (!changed) return JNI_FALSE;
  checkpoint();
  canvas->setPixels(pixels);
  storeActive();
  return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeTrimActiveMasked(
    JNIEnv* env, jclass, jint left, jint top, jint width, jint height, jbyteArray mask) {
  std::lock_guard<std::mutex> lock(guard);
  if (!canvas || !layers || !mask || width <= 0 || height <= 0 || left < 0 || top < 0)
    return JNI_FALSE;
  const int w = canvas->width(), h = canvas->height();
  if (static_cast<std::int64_t>(left) + width > w || static_cast<std::int64_t>(top) + height > h)
    return JNI_FALSE;
  if (static_cast<std::int64_t>(width) * height != env->GetArrayLength(mask)) return JNI_FALSE;
  std::vector<jbyte> bits(static_cast<std::size_t>(width) * height);
  env->GetByteArrayRegion(mask, 0, static_cast<jsize>(bits.size()), bits.data());
  if (env->ExceptionCheck()) return JNI_FALSE;
  if (std::none_of(bits.begin(), bits.end(), [](jbyte bit) { return bit != 0; })) return JNI_FALSE;
  auto pixels = canvas->pixels();
  bool changed = false;
  for (int y = 0; y < h; ++y)
    for (int x = 0; x < w; ++x) {
      const bool inside = x >= left && y >= top && x < left + width && y < top + height;
      if (inside && bits[static_cast<std::size_t>(y - top) * width + x - left]) continue;
      auto& pixel = pixels[static_cast<std::size_t>(y) * w + x];
      if (pixel != 0u) {
        pixel = 0u;
        changed = true;
      }
    }
  if (!changed) return JNI_FALSE;
  checkpoint();
  canvas->setPixels(pixels);
  storeActive();
  return JNI_TRUE;
}

extern "C" JNIEXPORT void JNICALL Java_art_velyntora_core_DrawingView_nativeSetBrushSelection(
    JNIEnv* env, jclass, jbyteArray source) {
  std::lock_guard<std::mutex> lock(guard);
  brushMask.clear();
  if (!source || !canvas) return;
  auto size = env->GetArrayLength(source);
  if (size != static_cast<jsize>(canvas->pixels().size())) return;
  try {
    brushMask.resize(size);
    env->GetByteArrayRegion(source, 0, size, reinterpret_cast<jbyte*>(brushMask.data()));
    if (env->ExceptionCheck()) brushMask.clear();
  } catch (...) {
    brushMask.clear();
  }
}

extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeTransformSelection(
    JNIEnv* env, jclass, jint left, jint top, jint w, jint h, jbyteArray source, jfloat degrees,
    jfloat sx, jfloat sy) {
  std::lock_guard<std::mutex> lock(guard);
  if (!canvas || !layers || !source || w <= 0 || h <= 0 || std::int64_t(w) * h > velyntora::limits::maxDocumentPixels ||
      env->GetArrayLength(source) != std::int64_t(w) * h)
    return JNI_FALSE;
  try {
    std::vector<std::uint8_t> mask(std::size_t(w) * h);
    env->GetByteArrayRegion(source, 0, mask.size(), reinterpret_cast<jbyte*>(mask.data()));
    if (env->ExceptionCheck()) return JNI_FALSE;
    auto pixels = velyntora::transformSelection(canvas->pixels(), canvas->width(), canvas->height(),
                                                left, top, w, h, mask, degrees, sx, sy);
    if (pixels == canvas->pixels()) return JNI_FALSE;
    checkpoint();
    canvas->setPixels(pixels);
    storeActive();
    return JNI_TRUE;
  } catch (...) {
    return JNI_FALSE;
  }
}

extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeSetEffectSelection(
    JNIEnv* env, jclass, jbyteArray source) {
  std::lock_guard<std::mutex> lock(guard);
  if (!source) {
    effectMask.clear();
    return JNI_TRUE;
  }
  if (!canvas) return JNI_FALSE;
  auto size = env->GetArrayLength(source);
  if (size != static_cast<jsize>(canvas->pixels().size())) return JNI_FALSE;
  try {
    std::vector<std::uint8_t> mask(size);
    env->GetByteArrayRegion(source, 0, size, reinterpret_cast<jbyte*>(mask.data()));
    if (env->ExceptionCheck()) return JNI_FALSE;
    effectMask = std::move(mask);
    return JNI_TRUE;
  } catch (...) {
    return JNI_FALSE;
  }
}

extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeFillSelection(
    JNIEnv* env, jclass, jint x, jint y, jint color, jfloat opacity, jbyteArray selection) {
  std::lock_guard<std::mutex> lock(guard);
  if (!canvas || x < 0 || y < 0 || x >= canvas->width() || y >= canvas->height() ||
      !std::isfinite(opacity) || opacity <= 0 || opacity > 1)
    return JNI_FALSE;
  try {
    std::vector<std::uint8_t> mask;
    if (selection) {
      if (env->GetArrayLength(selection) != static_cast<jsize>(canvas->pixels().size()))
        return JNI_FALSE;
      mask.resize(canvas->pixels().size());
      env->GetByteArrayRegion(selection, 0, static_cast<jsize>(mask.size()),
                              reinterpret_cast<jbyte*>(mask.data()));
      if (env->ExceptionCheck() || !mask[static_cast<std::size_t>(y) * canvas->width() + x])
        return JNI_FALSE;
    }
    velyntora::Canvas mixed(1, 1);
    mixed.setPixels({canvas->pixels()[static_cast<std::size_t>(y) * canvas->width() + x]});
    mixed.strokeStyled(.5f, .5f, .5f, .5f, .5f, static_cast<std::uint32_t>(color), opacity, 1, true,
                       false);
    auto filled = *canvas;
    if(canvas->pixels().isUniform() && mask.empty())filled.clear(mixed.pixels()[0]);
    else filled.fill(x, y, mixed.pixels()[0], mask.empty() ? nullptr : &mask);
    if (filled.pixels() == canvas->pixels()) return JNI_FALSE;
    checkpoint();
    canvas->setPixels(filled.pixels());
    storeActive();
    return JNI_TRUE;
  } catch (const std::exception&) {
    return JNI_FALSE;
  }
}

extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeFillSelectionWorker(
    JNIEnv* env,jclass cls,jint x,jint y,jint color,jfloat opacity,jbyteArray selection){
  return Java_art_velyntora_core_DrawingView_nativeFillSelection(env,cls,x,y,color,opacity,selection);
}
