package art.velyntora.core;

import android.graphics.Bitmap;
import android.graphics.Rect;

/** Bounded CPU sampling; never asks Skia to filter the document-sized bitmap. */
final class BitmapSampler {
  private int[] input = new int[0], output = new int[0];
  long pixelsRead;

  void update(Bitmap source, Bitmap destination, Rect bounds, int sample, Rect dirty) {
    int left = bounds.left + dirty.left * sample;
    int top = bounds.top + dirty.top * sample;
    int width = Math.min(source.getWidth() - left, dirty.width() * sample);
    int outputWidth = dirty.width(), outputHeight = dirty.height();
    if (output.length < outputWidth * outputHeight) output = new int[outputWidth * outputHeight];
    if (sample == 1) {
      int count = width * outputHeight;
      if (input.length < count) input = new int[count];
      source.getPixels(input, 0, width, left, top, width, outputHeight);
      pixelsRead += count;
      destination.setPixels(input, 0, width, dirty.left, dirty.top, width, outputHeight);
      return;
    }
    for (int row = 0; row < outputHeight; row++) {
      int y = top + row * sample;
      int height = Math.min(sample, source.getHeight() - y);
      int count = width * height;
      if (input.length < count) input = new int[count];
      source.getPixels(input, 0, width, left, y, width, height);
      pixelsRead += count;
      for (int col = 0; col < outputWidth; col++) {
        int x = col * sample, columns = Math.min(sample, width - x);
        int alpha = 0, red = 0, green = 0, blue = 0;
        for (int sy = 0; sy < height; sy++) {
          int offset = sy * width + x;
          for (int sx = 0; sx < columns; sx++) {
            int color = input[offset + sx], a = color >>> 24;
            alpha += a;
            red += ((color >>> 16) & 255) * a;
            green += ((color >>> 8) & 255) * a;
            blue += (color & 255) * a;
          }
        }
        int n = height * columns;
        output[row * outputWidth + col] = alpha == 0 ? 0 :
            (((alpha + n / 2) / n) << 24) |
            (((red + alpha / 2) / alpha) << 16) |
            (((green + alpha / 2) / alpha) << 8) |
            ((blue + alpha / 2) / alpha);
      }
    }
    destination.setPixels(output, 0, outputWidth, dirty.left, dirty.top, outputWidth, outputHeight);
  }
}
