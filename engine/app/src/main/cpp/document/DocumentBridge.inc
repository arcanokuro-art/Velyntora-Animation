#include "velyntora/CanvasLimits.hpp"

extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeCreate(JNIEnv*,
                                                                                       jclass,
                                                                                       jint w,
                                                                                       jint h) {
  std::lock_guard<std::mutex> lock(guard);
  if (w <= 0 || h <= 0 || w > velyntora::limits::maxSide || h > velyntora::limits::maxSide || std::int64_t(w) * h > velyntora::limits::maxDocumentPixels) return JNI_FALSE;
  try {
    auto newCanvas = std::make_unique<velyntora::Canvas>(w, h);
    auto newLayers = std::make_unique<velyntora::LayerDocument>(w, h);
    canvas = std::move(newCanvas);
    layers = std::move(newLayers);
    resetHistory();
    return JNI_TRUE;
  } catch (...) {
    return JNI_FALSE;
  }
}
extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeClear(JNIEnv*,
                                                                                      jclass) {
  std::lock_guard<std::mutex> lock(guard);
  if (!canvas || !layers) return JNI_FALSE;
  checkpoint();
  // Preserve the document's dimensions while resetting its layers and canvas.
  auto freshCanvas = std::make_unique<velyntora::Canvas>(canvas->width(), canvas->height());
  auto freshLayers = std::make_unique<velyntora::LayerDocument>(canvas->width(), canvas->height());
  canvas = std::move(freshCanvas);
  layers = std::move(freshLayers);
  return JNI_TRUE;
}
extern "C" JNIEXPORT jboolean JNICALL
Java_art_velyntora_core_DrawingView_nativeImport(JNIEnv* env, jclass, jintArray source) {
  std::lock_guard<std::mutex> lock(guard);
  if (!canvas || !source) return JNI_FALSE;
  jsize count = env->GetArrayLength(source);
  if (count != static_cast<jsize>(canvas->pixels().size())) return JNI_FALSE;
  std::vector<jint> data(static_cast<std::size_t>(count));
  env->GetIntArrayRegion(source, 0, count, data.data());
  if (env->ExceptionCheck()) return JNI_FALSE;
  checkpoint();
  canvas->setPixels(std::vector<std::uint32_t>(data.begin(), data.end()));
  storeActive();
  return JNI_TRUE;
}
extern "C" JNIEXPORT jboolean JNICALL
Java_art_velyntora_core_DrawingView_nativeSaveProject(JNIEnv*, jclass, jint fd) {
  std::lock_guard<std::mutex> lock(guard);
  if (!layers || fd < 0) return JNI_FALSE;
  try {
    velyntora::writeProject(*layers, [fd](const void* data, std::size_t size) {
      const char* cursor = static_cast<const char*>(data);
      while (size) {
        ssize_t n = ::write(fd, cursor, size);
        if (n < 0 && errno == EINTR) continue;
        if (n <= 0) throw std::runtime_error("Project write failed");
        cursor += n;
        size -= n;
      }
    });
    return JNI_TRUE;
  } catch (...) {
    return JNI_FALSE;
  }
}
extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeSaveRecovery(
    JNIEnv*, jclass, jint fd, jlong expectedRevision) {
  std::lock_guard<std::mutex> lock(guard);
  if (!layers || fd < 0 || revision != static_cast<std::uint64_t>(expectedRevision))
    return JNI_FALSE;
  try {
    velyntora::writeProject(*layers, [fd](const void* data, std::size_t size) {
      const char* cursor = static_cast<const char*>(data);
      while (size) {
        ssize_t n = ::write(fd, cursor, size);
        if (n < 0 && errno == EINTR) continue;
        if (n <= 0) throw std::runtime_error("Project write failed");
        cursor += n;
        size -= n;
      }
    });
    return JNI_TRUE;
  } catch (...) {
    return JNI_FALSE;
  }
}
extern "C" JNIEXPORT jboolean JNICALL
Java_art_velyntora_core_DrawingView_nativeOpenProject(JNIEnv*, jclass, jint fd) {
  std::lock_guard<std::mutex> lock(guard);
  if (fd < 0) return JNI_FALSE;
  try {
    auto restored = std::make_unique<velyntora::LayerDocument>(velyntora::readProject(
        [fd](void* data, std::size_t size) {
          char* cursor = static_cast<char*>(data);
          while (size) {
            ssize_t n = ::read(fd, cursor, size);
            if (n < 0 && errno == EINTR) continue;
            if (n <= 0) throw std::runtime_error("Truncated project");
            cursor += n;
            size -= n;
          }
        },
        0, 0, velyntora::limits::maxDocumentPixels));
    if (restored->width() > velyntora::limits::maxSide || restored->height() > velyntora::limits::maxSide) return JNI_FALSE;
    char trailing;
    ssize_t n;
    do {
      n = ::read(fd, &trailing, 1);
    } while (n < 0 && errno == EINTR);
    if (n != 0) return JNI_FALSE;
    auto restoredCanvas =
        std::make_unique<velyntora::Canvas>(restored->width(), restored->height());
    restoredCanvas->setPixels(restored->layer(restored->activeIndex()).pixels);
    // Publish only after the entire file has been validated and allocated.
    layers = std::move(restored);
    canvas = std::move(restoredCanvas);
    curveTag = 0;
    resetHistory();
    return JNI_TRUE;
  } catch (...) {
    return JNI_FALSE;
  }
}

extern "C" JNIEXPORT jint JNICALL Java_art_velyntora_core_DrawingView_nativeWidth(JNIEnv*, jclass) {
  std::lock_guard<std::mutex> lock(guard);
  return layers ? layers->width() : 0;
}
extern "C" JNIEXPORT jint JNICALL Java_art_velyntora_core_DrawingView_nativeHeight(JNIEnv*,
                                                                                   jclass) {
  std::lock_guard<std::mutex> lock(guard);
  return layers ? layers->height() : 0;
}
static jboolean resizeDocument(int w, int h, bool scalePixels, bool bilinear, int anchor) {
  std::lock_guard<std::mutex> lock(guard);
  if (!layers || anchor < 0 || anchor > 8 || w <= 0 || h <= 0 || w > velyntora::limits::maxSide || h > velyntora::limits::maxSide ||
      std::int64_t(w) * h > velyntora::limits::maxDocumentPixels)
    return JNI_FALSE;
  if (w == layers->width() && h == layers->height()) return JNI_FALSE;
  try {
    auto resized = std::make_unique<velyntora::LayerDocument>(
        layers->resized(w, h, scalePixels, bilinear, anchor));
    auto resizedCanvas = std::make_unique<velyntora::Canvas>(w, h);
    resizedCanvas->setPixels(resized->layer(resized->activeIndex()).pixels);
    checkpoint();
    layers = std::move(resized);
    canvas = std::move(resizedCanvas);
    return JNI_TRUE;
  } catch (...) {
    return JNI_FALSE;
  }
}
extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeResizeDocument(
    JNIEnv*, jclass, jint w, jint h, jboolean scalePixels) {
  return resizeDocument(w, h, scalePixels, false, 0);
}
extern "C" JNIEXPORT jboolean JNICALL
Java_art_velyntora_core_DrawingView_nativeResizeDocumentOptions(JNIEnv*, jclass, jint w, jint h,
                                                                jboolean scalePixels,
                                                                jboolean bilinear, jint anchor) {
  return resizeDocument(w, h, scalePixels, bilinear, anchor);
}
extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeCropDocument(
    JNIEnv*, jclass, jint left, jint top, jint w, jint h) {
  std::lock_guard<std::mutex> lock(guard);
  if (!layers) return JNI_FALSE;
  try {
    auto cropped = std::make_unique<velyntora::LayerDocument>(layers->cropped(left, top, w, h));
    auto croppedCanvas = std::make_unique<velyntora::Canvas>(w, h);
    croppedCanvas->setPixels(cropped->layer(cropped->activeIndex()).pixels);
    checkpoint();
    layers = std::move(cropped);
    canvas = std::move(croppedCanvas);
    return JNI_TRUE;
  } catch (...) {
    return JNI_FALSE;
  }
}

extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeLoadBitmap(
    JNIEnv* env, jclass, jint w, jint h, jintArray source) {
  std::lock_guard<std::mutex> lock(guard);
  if (!source || w <= 0 || h <= 0 || w > velyntora::limits::maxSide || h > velyntora::limits::maxSide || std::int64_t(w) * h > velyntora::limits::maxDocumentPixels ||
      env->GetArrayLength(source) != std::int64_t(w) * h)
    return JNI_FALSE;
  try {
    std::vector<std::uint32_t> pixels(std::size_t(w) * h);
    env->GetIntArrayRegion(source, 0, pixels.size(), reinterpret_cast<jint*>(pixels.data()));
    if (env->ExceptionCheck()) return JNI_FALSE;
    auto imported = std::make_unique<velyntora::LayerDocument>(w, h);
    imported->replaceActivePixels(pixels);
    auto importedCanvas = std::make_unique<velyntora::Canvas>(w, h);
    importedCanvas->setPixels(pixels);
    layers = std::move(imported);
    canvas = std::move(importedCanvas);
    resetHistory();
    return JNI_TRUE;
  } catch (...) {
    return JNI_FALSE;
  }
}


extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeBeginImageImport(JNIEnv*,jclass,jint w,jint h){
  std::lock_guard<std::mutex> lock(guard);
  if(!velyntora::limits::accepts(w,h))return JNI_FALSE;
  try {importing=std::make_unique<velyntora::LayerDocument>(w,h);return JNI_TRUE;}catch(...){return JNI_FALSE;}
}
extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeImageImportRegion(JNIEnv* env,jclass,jint x,jint y,jint w,jint h,jintArray source){
  std::lock_guard<std::mutex> lock(guard);
  if(!importing || !source || x<0 || y<0 || w<=0 || h<=0 || x>importing->width()-w || y>importing->height()-h
      || std::int64_t(w)*h>2097152 || env->GetArrayLength(source)!=std::int64_t(w)*h)return JNI_FALSE;
  try{
    std::vector<std::uint32_t> row(w);
    for(int i=0;i<h;++i){env->GetIntArrayRegion(source,i*w,w,reinterpret_cast<jint*>(row.data()));if(env->ExceptionCheck())return JNI_FALSE;
      importing->writeActiveSpan(std::size_t(y+i)*importing->width()+x,w,row.data());}
    return JNI_TRUE;
  }catch(...){return JNI_FALSE;}
}
extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeFinishImageImport(JNIEnv*,jclass){
  std::lock_guard<std::mutex> lock(guard);if(!importing)return JNI_FALSE;
  try {
    auto next=std::make_unique<velyntora::Canvas>(importing->width(),importing->height());next->setPixels(importing->layer(0).pixels);
    auto previous=std::make_unique<ImportPrevious>();
    previous->canvas=std::move(canvas);previous->layers=std::move(layers);previous->undo=std::move(undoStack);previous->redo=std::move(redoStack);
    previous->archive=std::move(historyArchive);previous->tag=curveTag;previous->revision=revision;importPrevious=std::move(previous);
    layers=std::move(importing);canvas=std::move(next);resetHistory();return JNI_TRUE;
  }catch(...){return JNI_FALSE;}
}
extern "C" JNIEXPORT void JNICALL Java_art_velyntora_core_DrawingView_nativeCancelImageImport(JNIEnv*,jclass){
  std::lock_guard<std::mutex> lock(guard);importing.reset();
  if(importPrevious){canvas=std::move(importPrevious->canvas);layers=std::move(importPrevious->layers);
    undoStack=std::move(importPrevious->undo);redoStack=std::move(importPrevious->redo);historyArchive=std::move(importPrevious->archive);
    curveTag=importPrevious->tag;revision=importPrevious->revision;importPrevious.reset();}
  velyntora::pages::cache().discardUnused();
}

extern "C" JNIEXPORT void JNICALL Java_art_velyntora_core_DrawingView_nativeAcceptImageImport(JNIEnv*,jclass){
  std::lock_guard<std::mutex> lock(guard);importPrevious.reset();velyntora::pages::cache().discardUnused();
}
