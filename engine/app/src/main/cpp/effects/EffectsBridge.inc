extern "C" JNIEXPORT jboolean JNICALL
Java_art_velyntora_core_DrawingView_nativeFlipActiveHorizontal(JNIEnv*, jclass) {
  std::lock_guard<std::mutex> lock(guard);
  if (!canvas || !layers) return JNI_FALSE;
  const int w = canvas->width(), h = canvas->height();
  if (w <= 1 || h <= 0) return JNI_FALSE;
  auto pixels = canvas->pixels();
  bool changed = false;
  for (int y = 0; y < h; ++y)
    for (int x = 0; x < w / 2; ++x) {
      const std::size_t a = static_cast<std::size_t>(y) * w + x;
      const std::size_t b = static_cast<std::size_t>(y) * w + (w - 1 - x);
      if (pixels[a] != pixels[b]) changed = true;
      std::swap(pixels[a], pixels[b]);
    }
  if (!changed) return JNI_FALSE;
  checkpoint();
  canvas->setPixels(pixels);
  storeActive();
  return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_art_velyntora_core_DrawingView_nativeFlipActiveVertical(JNIEnv*, jclass) {
  std::lock_guard<std::mutex> lock(guard);
  if (!canvas || !layers) return JNI_FALSE;
  const int w = canvas->width(), h = canvas->height();
  if (w <= 0 || h <= 1) return JNI_FALSE;
  auto pixels = canvas->pixels();
  bool changed = false;
  for (int y = 0; y < h / 2; ++y)
    for (int x = 0; x < w; ++x) {
      const std::size_t a = static_cast<std::size_t>(y) * w + x;
      const std::size_t b = static_cast<std::size_t>(h - 1 - y) * w + x;
      if (pixels[a] != pixels[b]) changed = true;
      std::swap(pixels[a], pixels[b]);
    }
  if (!changed) return JNI_FALSE;
  checkpoint();
  canvas->setPixels(pixels);
  storeActive();
  return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_art_velyntora_core_DrawingView_nativeRotateActive180(JNIEnv*, jclass) {
  std::lock_guard<std::mutex> lock(guard);
  if (!canvas || !layers) return JNI_FALSE;
  const int w = canvas->width(), h = canvas->height();
  if (w <= 0 || h <= 0) return JNI_FALSE;
  auto pixels = canvas->pixels();
  bool changed = false;
  for (std::size_t a = 0, b = pixels.size() - 1; a < b; ++a, --b) {
    if (pixels[a] != pixels[b]) changed = true;
    std::swap(pixels[a], pixels[b]);
  }
  if (!changed) return JNI_FALSE;
  checkpoint();
  canvas->setPixels(pixels);
  storeActive();
  return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_art_velyntora_core_DrawingView_nativeRotateActive90(JNIEnv*, jclass, jboolean clockwise) {
  std::lock_guard<std::mutex> lock(guard);
  if (!canvas || !layers) return JNI_FALSE;
  const int w = canvas->width(), h = canvas->height();
  if (w <= 0 || h <= 0 || w != h) return JNI_FALSE;
  const auto original = canvas->pixels();
  auto rotated = original;
  for (int y = 0; y < h; ++y)
    for (int x = 0; x < w; ++x) {
      const int nx = clockwise ? (w - 1 - y) : y;
      const int ny = clockwise ? x : (h - 1 - x);
      rotated[static_cast<std::size_t>(ny) * w + nx] =
          original[static_cast<std::size_t>(y) * w + x];
    }
  if (rotated == original) return JNI_FALSE;
  checkpoint();
  canvas->setPixels(rotated);
  storeActive();
  return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeCropActiveEllipse(
    JNIEnv*, jclass, jint left, jint top, jint right, jint bottom) {
  std::lock_guard<std::mutex> lock(guard);
  if (!canvas || !layers) return JNI_FALSE;
  const int w = canvas->width(), h = canvas->height();
  if (left < 0 || top < 0 || right > w || bottom > h || left >= right || top >= bottom)
    return JNI_FALSE;
  auto pixels = canvas->pixels();
  bool changed = false;
  const double rx = (static_cast<double>(right) - left) / 2.0,
               ry = (static_cast<double>(bottom) - top) / 2.0;
  const double cx = (static_cast<double>(left) + right) / 2.0,
               cy = (static_cast<double>(top) + bottom) / 2.0;
  for (int y = 0; y < h; ++y)
    for (int x = 0; x < w; ++x) {
      const double dx = (x + 0.5 - cx) / rx, dy = (y + 0.5 - cy) / ry;
      if (dx * dx + dy * dy <= 1.0) continue;
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

extern "C" JNIEXPORT jboolean JNICALL
Java_art_velyntora_core_DrawingView_nativeInvertActiveColors(JNIEnv*, jclass) {
  std::lock_guard<std::mutex> lock(guard);
  if (!canvas || !layers) return JNI_FALSE;
  auto pixels = canvas->pixels();
  bool changed = false;
  for (auto& pixel : pixels) {
    if ((pixel >> 24) == 0u) continue;
    const std::uint32_t inverted = (pixel & 0xFF000000u) | ((~pixel) & 0x00FFFFFFu);
    if (inverted != pixel) {
      pixel = inverted;
      changed = true;
    }
  }
  if (!changed) return JNI_FALSE;
  checkpoint();
  canvas->setPixels(pixels);
  storeActive();
  return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_art_velyntora_core_DrawingView_nativeGrayscaleActive(JNIEnv*, jclass) {
  std::lock_guard<std::mutex> lock(guard);
  if (!canvas || !layers) return JNI_FALSE;
  auto pixels = canvas->pixels();
  bool changed = false;
  for (auto& pixel : pixels) {
    const std::uint32_t a = pixel & 0xFF000000u;
    if (!a) continue;
    const std::uint32_t r = (pixel >> 16) & 255u, g = (pixel >> 8) & 255u, b = pixel & 255u;
    const std::uint32_t gray = (299u * r + 587u * g + 114u * b + 500u) / 1000u;
    const std::uint32_t converted = a | (gray << 16) | (gray << 8) | gray;
    if (converted != pixel) {
      pixel = converted;
      changed = true;
    }
  }
  if (!changed) return JNI_FALSE;
  checkpoint();
  canvas->setPixels(pixels);
  storeActive();
  return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_art_velyntora_core_DrawingView_nativeSepiaActive(JNIEnv*, jclass) {
  std::lock_guard<std::mutex> lock(guard);
  if (!canvas || !layers) return JNI_FALSE;
  auto pixels = canvas->pixels();
  bool changed = false;
  for (auto& pixel : pixels) {
    const std::uint32_t alpha = pixel & 0xFF000000u;
    if (!alpha) continue;
    const int r = static_cast<int>((pixel >> 16) & 255u), g = static_cast<int>((pixel >> 8) & 255u),
              b = static_cast<int>(pixel & 255u);
    const auto clamp = [](int v) -> std::uint32_t {
      return static_cast<std::uint32_t>(std::min(255, std::max(0, v)));
    };
    const std::uint32_t nr = clamp((393 * r + 769 * g + 189 * b + 500) / 1000);
    const std::uint32_t ng = clamp((349 * r + 686 * g + 168 * b + 500) / 1000);
    const std::uint32_t nb = clamp((272 * r + 534 * g + 131 * b + 500) / 1000);
    const std::uint32_t result = alpha | (nr << 16) | (ng << 8) | nb;
    if (result != pixel) {
      pixel = result;
      changed = true;
    }
  }
  if (!changed) return JNI_FALSE;
  checkpoint();
  canvas->setPixels(pixels);
  storeActive();
  return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_art_velyntora_core_DrawingView_nativeBrightnessActive(JNIEnv*, jclass, jint adjustment) {
  std::lock_guard<std::mutex> lock(guard);
  if (!canvas || !layers || adjustment == 0 || adjustment < -255 || adjustment > 255)
    return JNI_FALSE;
  auto pixels = canvas->pixels();
  bool changed = false;
  const auto clamp = [](int v) -> std::uint32_t {
    return static_cast<std::uint32_t>(std::min(255, std::max(0, v)));
  };
  for (auto& pixel : pixels) {
    const std::uint32_t alpha = pixel & 0xFF000000u;
    if (!alpha) continue;
    const std::uint32_t r = clamp(static_cast<int>((pixel >> 16) & 255u) + adjustment);
    const std::uint32_t g = clamp(static_cast<int>((pixel >> 8) & 255u) + adjustment);
    const std::uint32_t b = clamp(static_cast<int>(pixel & 255u) + adjustment);
    const std::uint32_t result = alpha | (r << 16) | (g << 8) | b;
    if (result != pixel) {
      pixel = result;
      changed = true;
    }
  }
  if (!changed) return JNI_FALSE;
  checkpoint();
  canvas->setPixels(pixels);
  storeActive();
  return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_art_velyntora_core_DrawingView_nativeContrastActive(JNIEnv*, jclass, jint adjustment) {
  std::lock_guard<std::mutex> lock(guard);
  if (!canvas || !layers || adjustment == 0 || adjustment < -100 || adjustment > 100)
    return JNI_FALSE;
  auto pixels = canvas->pixels();
  bool changed = false;
  const double factor = 1.0 + adjustment / 100.0;
  const auto clamp = [](double v) -> std::uint32_t {
    return static_cast<std::uint32_t>(std::min(255.0, std::max(0.0, std::floor(v + 0.5))));
  };
  for (auto& pixel : pixels) {
    const std::uint32_t alpha = pixel & 0xFF000000u;
    if (!alpha) continue;
    const auto transform = [&](std::uint32_t channel) {
      return clamp((static_cast<double>(channel) - 127.5) * factor + 127.5);
    };
    const std::uint32_t r = transform((pixel >> 16) & 255u), g = transform((pixel >> 8) & 255u),
                        b = transform(pixel & 255u);
    const std::uint32_t result = alpha | (r << 16) | (g << 8) | b;
    if (result != pixel) {
      pixel = result;
      changed = true;
    }
  }
  if (!changed) return JNI_FALSE;
  checkpoint();
  canvas->setPixels(pixels);
  storeActive();
  return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_art_velyntora_core_DrawingView_nativeThresholdActive(JNIEnv*, jclass, jint threshold) {
  std::lock_guard<std::mutex> lock(guard);
  if (!canvas || !layers || threshold < 0 || threshold > 255) return JNI_FALSE;
  auto pixels = canvas->pixels();
  bool changed = false;
  for (auto& pixel : pixels) {
    const std::uint32_t alpha = pixel & 0xFF000000u;
    if (!alpha) continue;
    const std::uint32_t r = (pixel >> 16) & 255u, g = (pixel >> 8) & 255u, b = pixel & 255u;
    const std::uint32_t luminance = (299u * r + 587u * g + 114u * b + 500u) / 1000u;
    const std::uint32_t monochrome =
        luminance >= static_cast<std::uint32_t>(threshold) ? 0x00FFFFFFu : 0u;
    const std::uint32_t result = alpha | monochrome;
    if (result != pixel) {
      pixel = result;
      changed = true;
    }
  }
  if (!changed) return JNI_FALSE;
  checkpoint();
  canvas->setPixels(pixels);
  storeActive();
  return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_art_velyntora_core_DrawingView_nativePosterizeActive(JNIEnv*, jclass, jint levels) {
  std::lock_guard<std::mutex> lock(guard);
  if (!canvas || !layers || levels < 2 || levels > 256) return JNI_FALSE;
  auto pixels = canvas->pixels();
  bool changed = false;
  for (auto& pixel : pixels) {
    const std::uint32_t alpha = pixel & 0xFF000000u;
    if (!alpha) continue;
    const auto quantize = [levels](std::uint32_t channel) -> std::uint32_t {
      const int step =
          static_cast<int>((channel * static_cast<std::uint32_t>(levels - 1) + 127u) / 255u);
      return static_cast<std::uint32_t>((step * 255 + (levels - 1) / 2) / (levels - 1));
    };
    const std::uint32_t r = quantize((pixel >> 16) & 255u), g = quantize((pixel >> 8) & 255u),
                        b = quantize(pixel & 255u);
    const std::uint32_t result = alpha | (r << 16) | (g << 8) | b;
    if (result != pixel) {
      pixel = result;
      changed = true;
    }
  }
  if (!changed) return JNI_FALSE;
  checkpoint();
  canvas->setPixels(pixels);
  storeActive();
  return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_art_velyntora_core_DrawingView_nativeSolarizeActive(JNIEnv*, jclass, jint threshold) {
  std::lock_guard<std::mutex> lock(guard);
  if (!canvas || !layers || threshold < 0 || threshold > 255) return JNI_FALSE;
  auto pixels = canvas->pixels();
  bool changed = false;
  for (auto& pixel : pixels) {
    const std::uint32_t alpha = pixel & 0xFF000000u;
    if (!alpha) continue;
    const auto solarize = [threshold](std::uint32_t channel) -> std::uint32_t {
      return channel >= static_cast<std::uint32_t>(threshold) ? 255u - channel : channel;
    };
    const std::uint32_t r = solarize((pixel >> 16) & 255u), g = solarize((pixel >> 8) & 255u),
                        b = solarize(pixel & 255u);
    const std::uint32_t result = alpha | (r << 16) | (g << 8) | b;
    if (result != pixel) {
      pixel = result;
      changed = true;
    }
  }
  if (!changed) return JNI_FALSE;
  checkpoint();
  canvas->setPixels(pixels);
  storeActive();
  return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_art_velyntora_core_DrawingView_nativeSaturationActive(JNIEnv*, jclass, jint adjustment) {
  std::lock_guard<std::mutex> lock(guard);
  if (!canvas || !layers || adjustment == 0 || adjustment < -100 || adjustment > 100)
    return JNI_FALSE;
  auto pixels = canvas->pixels();
  bool changed = false;
  const double factor = 1.0 + adjustment / 100.0;
  const auto clamp = [](double v) -> std::uint32_t {
    return static_cast<std::uint32_t>(std::min(255.0, std::max(0.0, std::floor(v + 0.5))));
  };
  for (auto& pixel : pixels) {
    const std::uint32_t alpha = pixel & 0xFF000000u;
    if (!alpha) continue;
    const double r = (pixel >> 16) & 255u, g = (pixel >> 8) & 255u, b = pixel & 255u;
    const double gray = 0.299 * r + 0.587 * g + 0.114 * b;
    const std::uint32_t nr = clamp(gray + (r - gray) * factor),
                        ng = clamp(gray + (g - gray) * factor),
                        nb = clamp(gray + (b - gray) * factor);
    const std::uint32_t result = alpha | (nr << 16) | (ng << 8) | nb;
    if (result != pixel) {
      pixel = result;
      changed = true;
    }
  }
  if (!changed) return JNI_FALSE;
  checkpoint();
  canvas->setPixels(pixels);
  storeActive();
  return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_art_velyntora_core_DrawingView_nativeGammaActive(JNIEnv*, jclass, jint percent) {
  std::lock_guard<std::mutex> lock(guard);
  if (!canvas || !layers || percent < 10 || percent > 300 || percent == 100) return JNI_FALSE;
  auto pixels = canvas->pixels();
  bool changed = false;
  const double gamma = static_cast<double>(percent) / 100.0;
  const auto correct = [gamma](std::uint32_t channel) -> std::uint32_t {
    const double value = 255.0 * std::pow(static_cast<double>(channel) / 255.0, 1.0 / gamma);
    return static_cast<std::uint32_t>(std::min(255.0, std::max(0.0, std::floor(value + 0.5))));
  };
  for (auto& pixel : pixels) {
    const std::uint32_t alpha = pixel & 0xFF000000u;
    if (!alpha) continue;
    const std::uint32_t r = correct((pixel >> 16) & 255u), g = correct((pixel >> 8) & 255u),
                        b = correct(pixel & 255u);
    const std::uint32_t result = alpha | (r << 16) | (g << 8) | b;
    if (result != pixel) {
      pixel = result;
      changed = true;
    }
  }
  if (!changed) return JNI_FALSE;
  checkpoint();
  canvas->setPixels(pixels);
  storeActive();
  return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeTintActive(
    JNIEnv*, jclass, jint redAdjustment, jint greenAdjustment, jint blueAdjustment) {
  std::lock_guard<std::mutex> lock(guard);
  if (!canvas || !layers || redAdjustment < -255 || redAdjustment > 255 || greenAdjustment < -255 ||
      greenAdjustment > 255 || blueAdjustment < -255 || blueAdjustment > 255)
    return JNI_FALSE;
  if (redAdjustment == 0 && greenAdjustment == 0 && blueAdjustment == 0) return JNI_FALSE;
  auto pixels = canvas->pixels();
  bool changed = false;
  const auto clamp = [](int value) -> std::uint32_t {
    return static_cast<std::uint32_t>(std::min(255, std::max(0, value)));
  };
  for (auto& pixel : pixels) {
    const std::uint32_t alpha = pixel & 0xFF000000u;
    if (!alpha) continue;
    const std::uint32_t r = clamp(static_cast<int>((pixel >> 16) & 255u) + redAdjustment);
    const std::uint32_t g = clamp(static_cast<int>((pixel >> 8) & 255u) + greenAdjustment);
    const std::uint32_t b = clamp(static_cast<int>(pixel & 255u) + blueAdjustment);
    const std::uint32_t result = alpha | (r << 16) | (g << 8) | b;
    if (result != pixel) {
      pixel = result;
      changed = true;
    }
  }
  if (!changed) return JNI_FALSE;
  checkpoint();
  canvas->setPixels(pixels);
  storeActive();
  return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_art_velyntora_core_DrawingView_nativeSwapChannelsActive(JNIEnv*, jclass, jint mode) {
  std::lock_guard<std::mutex> lock(guard);
  if (!canvas || !layers || (mode != 0 && mode != 1 && mode != 2)) return JNI_FALSE;
  auto pixels = canvas->pixels();
  bool changed = false;
  for (auto& pixel : pixels) {
    const std::uint32_t alpha = pixel & 0xFF000000u;
    if (!alpha) continue;
    const std::uint32_t r = (pixel >> 16) & 255u, g = (pixel >> 8) & 255u, b = pixel & 255u;
    const std::uint32_t nr = mode == 0 ? g : (mode == 1 ? b : r);
    const std::uint32_t ng = mode == 0 ? r : (mode == 1 ? g : b);
    const std::uint32_t nb = mode == 0 ? b : (mode == 1 ? r : g);
    const std::uint32_t result = alpha | (nr << 16) | (ng << 8) | nb;
    if (result != pixel) {
      pixel = result;
      changed = true;
    }
  }
  if (!changed) return JNI_FALSE;
  checkpoint();
  canvas->setPixels(pixels);
  storeActive();
  return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeColorBalanceActive(
    JNIEnv*, jclass, jint redPercent, jint greenPercent, jint bluePercent) {
  std::lock_guard<std::mutex> lock(guard);
  if (!canvas || !layers || redPercent < 0 || redPercent > 200 || greenPercent < 0 ||
      greenPercent > 200 || bluePercent < 0 || bluePercent > 200)
    return JNI_FALSE;
  if (redPercent == 100 && greenPercent == 100 && bluePercent == 100) return JNI_FALSE;
  auto pixels = canvas->pixels();
  bool changed = false;
  const auto scale = [](std::uint32_t channel, int percent) -> std::uint32_t {
    return std::min(255u, (channel * static_cast<std::uint32_t>(percent) + 50u) / 100u);
  };
  for (auto& pixel : pixels) {
    const std::uint32_t alpha = pixel & 0xFF000000u;
    if (!alpha) continue;
    const std::uint32_t r = scale((pixel >> 16) & 255u, redPercent);
    const std::uint32_t g = scale((pixel >> 8) & 255u, greenPercent);
    const std::uint32_t b = scale(pixel & 255u, bluePercent);
    const std::uint32_t result = alpha | (r << 16) | (g << 8) | b;
    if (result != pixel) {
      pixel = result;
      changed = true;
    }
  }
  if (!changed) return JNI_FALSE;
  checkpoint();
  canvas->setPixels(pixels);
  storeActive();
  return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_art_velyntora_core_DrawingView_nativeHueRotateActive(JNIEnv*, jclass, jint degrees) {
  std::lock_guard<std::mutex> lock(guard);
  if (!canvas || !layers || degrees < -180 || degrees > 180 || degrees == 0) return JNI_FALSE;
  auto pixels = canvas->pixels();
  bool changed = false;
  const double angle = static_cast<double>(degrees) * 3.14159265358979323846 / 180.0;
  const double cosine = std::cos(angle), sine = std::sin(angle);
  const auto clamp = [](double value) -> std::uint32_t {
    return static_cast<std::uint32_t>(std::min(255.0, std::max(0.0, std::floor(value + 0.5))));
  };
  for (auto& pixel : pixels) {
    const std::uint32_t alpha = pixel & 0xFF000000u;
    if (!alpha) continue;
    const double r = (pixel >> 16) & 255u, g = (pixel >> 8) & 255u, b = pixel & 255u;
    const double y = 0.299 * r + 0.587 * g + 0.114 * b;
    const double u = b - y, v = r - y;
    const double rotatedU = u * cosine - v * sine, rotatedV = u * sine + v * cosine;
    const std::uint32_t nr = clamp(y + rotatedV), nb = clamp(y + rotatedU),
                        ng = clamp((y - 0.299 * (y + rotatedV) - 0.114 * (y + rotatedU)) / 0.587);
    const std::uint32_t result = alpha | (nr << 16) | (ng << 8) | nb;
    if (result != pixel) {
      pixel = result;
      changed = true;
    }
  }
  if (!changed) return JNI_FALSE;
  checkpoint();
  canvas->setPixels(pixels);
  storeActive();
  return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeLevelsActive(
    JNIEnv*, jclass, jint blackPoint, jint whitePoint) {
  std::lock_guard<std::mutex> lock(guard);
  if (!canvas || !layers || blackPoint < 0 || whitePoint > 255 || blackPoint >= whitePoint ||
      (blackPoint == 0 && whitePoint == 255))
    return JNI_FALSE;
  auto pixels = canvas->pixels();
  bool changed = false;
  const auto remap = [blackPoint, whitePoint](std::uint32_t channel) -> std::uint32_t {
    const int value = static_cast<int>(channel);
    if (value <= blackPoint) return 0u;
    if (value >= whitePoint) return 255u;
    const int range = whitePoint - blackPoint;
    return static_cast<std::uint32_t>(((value - blackPoint) * 255 + range / 2) / range);
  };
  for (auto& pixel : pixels) {
    const std::uint32_t alpha = pixel & 0xFF000000u;
    if (!alpha) continue;
    const std::uint32_t r = remap((pixel >> 16) & 255u), g = remap((pixel >> 8) & 255u),
                        b = remap(pixel & 255u);
    const std::uint32_t result = alpha | (r << 16) | (g << 8) | b;
    if (result != pixel) {
      pixel = result;
      changed = true;
    }
  }
  if (!changed) return JNI_FALSE;
  checkpoint();
  canvas->setPixels(pixels);
  storeActive();
  return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_art_velyntora_core_DrawingView_nativeExposureActive(JNIEnv*, jclass, jint percent) {
  std::lock_guard<std::mutex> lock(guard);
  if (!canvas || !layers || percent < 10 || percent > 300 || percent == 100) return JNI_FALSE;
  auto pixels = canvas->pixels();
  bool changed = false;
  const auto scale = [percent](std::uint32_t channel) -> std::uint32_t {
    return std::min(255u, (channel * static_cast<std::uint32_t>(percent) + 50u) / 100u);
  };
  for (auto& pixel : pixels) {
    const std::uint32_t alpha = pixel & 0xFF000000u;
    if (!alpha) continue;
    const std::uint32_t r = scale((pixel >> 16) & 255u), g = scale((pixel >> 8) & 255u),
                        b = scale(pixel & 255u);
    const std::uint32_t result = alpha | (r << 16) | (g << 8) | b;
    if (result != pixel) {
      pixel = result;
      changed = true;
    }
  }
  if (!changed) return JNI_FALSE;
  checkpoint();
  canvas->setPixels(pixels);
  storeActive();
  return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_art_velyntora_core_DrawingView_nativeDesaturateChannelActive(JNIEnv*, jclass, jint channel) {
  std::lock_guard<std::mutex> lock(guard);
  if (!canvas || !layers || channel < 0 || channel > 2) return JNI_FALSE;
  auto pixels = canvas->pixels();
  bool changed = false;
  for (auto& pixel : pixels) {
    const std::uint32_t alpha = pixel & 0xFF000000u;
    if (!alpha) continue;
    const std::uint32_t r = (pixel >> 16) & 255u, g = (pixel >> 8) & 255u, b = pixel & 255u;
    const std::uint32_t gray = channel == 0 ? r : (channel == 1 ? g : b);
    const std::uint32_t result = alpha | (gray << 16) | (gray << 8) | gray;
    if (result != pixel) {
      pixel = result;
      changed = true;
    }
  }
  if (!changed) return JNI_FALSE;
  checkpoint();
  canvas->setPixels(pixels);
  storeActive();
  return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_art_velyntora_core_DrawingView_nativeAdjustAlphaActive(JNIEnv*, jclass, jint percent) {
  std::lock_guard<std::mutex> lock(guard);
  if (!canvas || !layers || percent < 0 || percent > 200 || percent == 100) return JNI_FALSE;
  auto pixels = canvas->pixels();
  bool changed = false;
  for (auto& pixel : pixels) {
    const std::uint32_t oldAlpha = (pixel >> 24) & 255u;
    const std::uint32_t newAlpha =
        std::min(255u, (oldAlpha * static_cast<std::uint32_t>(percent) + 50u) / 100u);
    const std::uint32_t result = (pixel & 0x00FFFFFFu) | (newAlpha << 24);
    if (result != pixel) {
      pixel = result;
      changed = true;
    }
  }
  if (!changed) return JNI_FALSE;
  checkpoint();
  canvas->setPixels(pixels);
  storeActive();
  return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_art_velyntora_core_DrawingView_nativeRemoveChannelActive(JNIEnv*, jclass, jint channel) {
  std::lock_guard<std::mutex> lock(guard);
  if (!canvas || !layers || channel < 0 || channel > 2) return JNI_FALSE;
  auto pixels = canvas->pixels();
  bool changed = false;
  const std::uint32_t mask =
      channel == 0 ? 0xFF00FFFFu : (channel == 1 ? 0xFFFF00FFu : 0xFFFFFF00u);
  for (auto& pixel : pixels) {
    const std::uint32_t result = pixel & mask;
    if (result != pixel) {
      pixel = result;
      changed = true;
    }
  }
  if (!changed) return JNI_FALSE;
  checkpoint();
  canvas->setPixels(pixels);
  storeActive();
  return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_art_velyntora_core_DrawingView_nativeNormalizeActive(JNIEnv*, jclass) {
  std::lock_guard<std::mutex> lock(guard);
  if (!canvas || !layers) return JNI_FALSE;
  auto pixels = canvas->pixels();
  std::uint32_t minimum = 255u, maximum = 0u;
  bool hasVisible = false;
  for (const auto pixel : pixels) {
    if ((pixel & 0xFF000000u) == 0u) continue;
    const std::uint32_t r = (pixel >> 16) & 255u, g = (pixel >> 8) & 255u, b = pixel & 255u;
    minimum = std::min(minimum, std::min(r, std::min(g, b)));
    maximum = std::max(maximum, std::max(r, std::max(g, b)));
    hasVisible = true;
  }
  if (!hasVisible || minimum >= maximum || (minimum == 0u && maximum == 255u)) return JNI_FALSE;
  bool changed = false;
  const std::uint32_t range = maximum - minimum;
  const auto normalize = [minimum, range](std::uint32_t channel) -> std::uint32_t {
    return ((channel - minimum) * 255u + range / 2u) / range;
  };
  for (auto& pixel : pixels) {
    const std::uint32_t alpha = pixel & 0xFF000000u;
    if (!alpha) continue;
    const std::uint32_t r = normalize((pixel >> 16) & 255u), g = normalize((pixel >> 8) & 255u),
                        b = normalize(pixel & 255u);
    const std::uint32_t result = alpha | (r << 16) | (g << 8) | b;
    if (result != pixel) {
      pixel = result;
      changed = true;
    }
  }
  if (!changed) return JNI_FALSE;
  checkpoint();
  canvas->setPixels(pixels);
  storeActive();
  return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_art_velyntora_core_DrawingView_nativeQuantizeActive(JNIEnv*, jclass, jint step) {
  std::lock_guard<std::mutex> lock(guard);
  if (!canvas || !layers || step < 2 || step > 128) return JNI_FALSE;
  auto pixels = canvas->pixels();
  bool changed = false;
  const auto quantize = [step](std::uint32_t channel) -> std::uint32_t {
    const std::uint32_t s = static_cast<std::uint32_t>(step);
    return std::min(255u, ((channel + s / 2u) / s) * s);
  };
  for (auto& pixel : pixels) {
    const std::uint32_t alpha = pixel & 0xFF000000u;
    if (!alpha) continue;
    const std::uint32_t r = quantize((pixel >> 16) & 255u), g = quantize((pixel >> 8) & 255u),
                        b = quantize(pixel & 255u);
    const std::uint32_t result = alpha | (r << 16) | (g << 8) | b;
    if (result != pixel) {
      pixel = result;
      changed = true;
    }
  }
  if (!changed) return JNI_FALSE;
  checkpoint();
  canvas->setPixels(pixels);
  storeActive();
  return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_art_velyntora_core_DrawingView_nativeClampHighlightsActive(JNIEnv*, jclass, jint ceiling) {
  std::lock_guard<std::mutex> lock(guard);
  if (!canvas || !layers || ceiling < 0 || ceiling >= 255) return JNI_FALSE;
  auto pixels = canvas->pixels();
  bool changed = false;
  const auto clamp = [ceiling](std::uint32_t channel) -> std::uint32_t {
    return std::min(channel, static_cast<std::uint32_t>(ceiling));
  };
  for (auto& pixel : pixels) {
    const std::uint32_t alpha = pixel & 0xFF000000u;
    if (!alpha) continue;
    const std::uint32_t r = clamp((pixel >> 16) & 255u), g = clamp((pixel >> 8) & 255u),
                        b = clamp(pixel & 255u);
    const std::uint32_t result = alpha | (r << 16) | (g << 8) | b;
    if (result != pixel) {
      pixel = result;
      changed = true;
    }
  }
  if (!changed) return JNI_FALSE;
  checkpoint();
  canvas->setPixels(pixels);
  storeActive();
  return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_art_velyntora_core_DrawingView_nativeLiftShadowsActive(JNIEnv*, jclass, jint floor) {
  std::lock_guard<std::mutex> lock(guard);
  if (!canvas || !layers || floor <= 0 || floor > 255) return JNI_FALSE;
  auto pixels = canvas->pixels();
  bool changed = false;
  const auto lift = [floor](std::uint32_t channel) -> std::uint32_t {
    return std::max(channel, static_cast<std::uint32_t>(floor));
  };
  for (auto& pixel : pixels) {
    const std::uint32_t alpha = pixel & 0xFF000000u;
    if (!alpha) continue;
    const std::uint32_t r = lift((pixel >> 16) & 255u), g = lift((pixel >> 8) & 255u),
                        b = lift(pixel & 255u);
    const std::uint32_t result = alpha | (r << 16) | (g << 8) | b;
    if (result != pixel) {
      pixel = result;
      changed = true;
    }
  }
  if (!changed) return JNI_FALSE;
  checkpoint();
  canvas->setPixels(pixels);
  storeActive();
  return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeAdjustChannelActive(
    JNIEnv*, jclass, jint channel, jint adjustment) {
  std::lock_guard<std::mutex> lock(guard);
  if (!canvas || !layers || channel < 0 || channel > 2 || adjustment < -255 || adjustment > 255 ||
      adjustment == 0)
    return JNI_FALSE;
  auto pixels = canvas->pixels();
  bool changed = false;
  const int shift = channel == 0 ? 16 : (channel == 1 ? 8 : 0);
  const std::uint32_t mask = 255u << shift;
  for (auto& pixel : pixels) {
    const std::uint32_t alpha = pixel & 0xFF000000u;
    if (!alpha) continue;
    const int value = static_cast<int>((pixel >> shift) & 255u);
    const std::uint32_t updated = static_cast<std::uint32_t>(
        std::max(0, std::min(255, value + static_cast<int>(adjustment))));
    const std::uint32_t result = (pixel & ~mask) | (updated << shift);
    if (result != pixel) {
      pixel = result;
      changed = true;
    }
  }
  if (!changed) return JNI_FALSE;
  checkpoint();
  canvas->setPixels(pixels);
  storeActive();
  return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_art_velyntora_core_DrawingView_nativeEffect(JNIEnv*, jclass, jint kind, jint amount) {
  std::lock_guard<std::mutex> lock(guard);
  if (!canvas || !layers) return JNI_FALSE;
  try {
    auto pixels =
        velyntora::pixelEffect(canvas->pixels(), canvas->width(), canvas->height(), kind, amount);
    applyEffectSelection(pixels);
    if (pixels == canvas->pixels()) return JNI_FALSE;
    checkpoint();
    canvas->setPixels(pixels);
    storeActive();
    return JNI_TRUE;
  } catch (...) {
    return JNI_FALSE;
  }
}

extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeColorAdjustment(
    JNIEnv* env, jclass, jint kind, jintArray values) {
  std::lock_guard<std::mutex> lock(guard);
  if (!canvas || !values) return JNI_FALSE;
  try {
    int n = env->GetArrayLength(values);
    if (n < 1 || n > 257) return JNI_FALSE;
    std::vector<int> v(n);
    env->GetIntArrayRegion(values, 0, n, v.data());
    if (env->ExceptionCheck()) return JNI_FALSE;
    velyntora::tiles::LayerPixels pixels;
    if (kind == 0 && n == 257) {
      std::array<int, 256> curve;
      std::copy_n(v.begin(), 256, curve.begin());
      pixels = velyntora::applyCurve(canvas->pixels(), curve, v[256]);
    } else if (kind == 1 && n == 15) {
      std::array<velyntora::ChannelLevels, 3> levels;
      for (int i = 0; i < 3; i++)
        levels[i] = {v[i * 5], v[i * 5 + 1], v[i * 5 + 2], v[i * 5 + 3], v[i * 5 + 4]};
      pixels = velyntora::applyLevels(canvas->pixels(), levels);
    } else if (kind == 2 && n == 1)
      pixels =
          velyntora::autoLevels(canvas->pixels(), v[0], effectMask.empty() ? nullptr : &effectMask);
    else if (kind == 3 && n == 3)
      pixels = velyntora::posterizeRgb(canvas->pixels(), {v[0], v[1], v[2]});
    else if (kind == 5 && n == 2)
      pixels = velyntora::brightnessContrast(canvas->pixels(), v[0], v[1]);
    else if (kind == 6 && n == 1)
      pixels = velyntora::basicColor(canvas->pixels(), v[0]);
    else if (kind == 4 && n == 3)
      pixels = velyntora::hueSaturation(canvas->pixels(), v[0], v[1], v[2]);
    else
      return JNI_FALSE;
    applyEffectSelection(pixels);
    if (pixels == canvas->pixels()) return JNI_FALSE;
    checkpoint();
    canvas->setPixels(pixels);
    storeActive();
    return JNI_TRUE;
  } catch (const std::exception&) {
    return JNI_FALSE;
  }
}

extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeBlur(
    JNIEnv*, jclass, jint kind, jint amount, jint angle, jint cx, jint cy) {
  std::lock_guard<std::mutex> lock(guard);
  if (!canvas) return JNI_FALSE;
  try {
    auto pixels = velyntora::blurEffect(canvas->pixels(), canvas->width(), canvas->height(), kind,
                                        amount, angle, cx, cy);
    applyEffectSelection(pixels);
    if (pixels == canvas->pixels()) return JNI_FALSE;
    checkpoint();
    canvas->setPixels(pixels);
    storeActive();
    return JNI_TRUE;
  } catch (const std::exception&) {
    return JNI_FALSE;
  }
}

extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeDistortion(
    JNIEnv*, jclass, jint kind, jint amount, jint size, jint angle, jint cx, jint cy) {
  std::lock_guard<std::mutex> lock(guard);
  if (!canvas) return JNI_FALSE;
  try {
    auto pixels = velyntora::distortionEffect(canvas->pixels(), canvas->width(), canvas->height(),
                                              kind, amount, size, angle, cx, cy);
    applyEffectSelection(pixels);
    if (pixels == canvas->pixels()) return JNI_FALSE;
    checkpoint();
    canvas->setPixels(pixels);
    storeActive();
    return JNI_TRUE;
  } catch (const std::exception&) {
    return JNI_FALSE;
  }
}

extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeArtistic(
    JNIEnv*, jclass, jint kind, jint strength, jint radius, jint threshold) {
  std::lock_guard<std::mutex> lock(guard);
  if (!canvas) return JNI_FALSE;
  try {
    auto pixels = velyntora::artisticEffect(canvas->pixels(), canvas->width(), canvas->height(),
                                            kind, strength, radius, threshold);
    applyEffectSelection(pixels);
    if (pixels == canvas->pixels()) return JNI_FALSE;
    checkpoint();
    canvas->setPixels(pixels);
    storeActive();
    return JNI_TRUE;
  } catch (const std::exception&) {
    return JNI_FALSE;
  }
}

extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeRender(
    JNIEnv*, jclass, jint kind, jint scale, jint detail, jint seed, jint first, jint second) {
  std::lock_guard<std::mutex> lock(guard);
  if (!canvas) return JNI_FALSE;
  try {
    auto pixels = velyntora::renderEffect(
        canvas->width(), canvas->height(), kind, scale, detail, static_cast<std::uint32_t>(seed),
        static_cast<std::uint32_t>(first), static_cast<std::uint32_t>(second));
    applyEffectSelection(pixels);
    if (pixels == canvas->pixels()) return JNI_FALSE;
    checkpoint();
    canvas->setPixels(pixels);
    storeActive();
    return JNI_TRUE;
  } catch (const std::exception&) {
    return JNI_FALSE;
  }
}

extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeUtility(
    JNIEnv*, jclass, jint kind, jint amount, jint size, jint parameter) {
  std::lock_guard<std::mutex> lock(guard);
  if (!canvas) return JNI_FALSE;
  try {
    auto pixels = velyntora::utilityEffect(canvas->pixels(), canvas->width(), canvas->height(),
                                           kind, amount, size, parameter);
    applyEffectSelection(pixels);
    if (pixels == canvas->pixels()) return JNI_FALSE;
    checkpoint();
    canvas->setPixels(pixels);
    storeActive();
    return JNI_TRUE;
  } catch (const std::exception&) {
    return JNI_FALSE;
  }
}
extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeObject(
    JNIEnv*, jclass, jint kind, jint amount, jint tolerance, jint color, jboolean option) {
  std::lock_guard<std::mutex> lock(guard);
  if (!canvas) return JNI_FALSE;
  try {
    auto pixels = velyntora::objectEffect(canvas->pixels(), canvas->width(), canvas->height(), kind,
                                          amount, tolerance, static_cast<std::uint32_t>(color),
                                          option, effectMask.empty() ? nullptr : &effectMask);
    applyEffectSelection(pixels);
    if (pixels == canvas->pixels()) return JNI_FALSE;
    checkpoint();
    canvas->setPixels(pixels);
    storeActive();
    return JNI_TRUE;
  } catch (const std::exception&) {
    return JNI_FALSE;
  }
}

