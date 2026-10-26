package art.velyntora.core;

import art.velyntora.core.document.CanvasLimits;

import java.io.IOException;
import java.io.OutputStream;

/** Streaming, dependency-free BMP (24-bit opaque) and TGA (32-bit alpha) export. */
final class RasterFileWriter {
  interface RowSource {
    void read(int y, int[] pixels);
  }

  private static void little(OutputStream out, int value, int bytes) throws IOException {
    for (int i = 0; i < bytes; i++) out.write(value >>> (8 * i) & 255);
  }

  static void write(OutputStream out, int width, int height, boolean tga, RowSource source)
      throws IOException {
    if (out == null
        || source == null
        || width < 1
        || height < 1
        || width > CanvasLimits.MAX_SIDE
        || height > CanvasLimits.MAX_SIDE
        || (long) width * height > CanvasLimits.MAX_PIXELS)
      throw new IllegalArgumentException("Invalid export dimensions");
    int stride = tga ? width * 4 : (width * 3 + 3) & ~3;
    if (tga) {
      out.write(new byte[] {0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0});
      little(out, width, 2);
      little(out, height, 2);
      out.write(32);
      out.write(0x28);
    } else {
      out.write('B');
      out.write('M');
      little(out, 54 + stride * height, 4);
      little(out, 0, 4);
      little(out, 54, 4);
      little(out, 40, 4);
      little(out, width, 4);
      little(out, height, 4);
      little(out, 1, 2);
      little(out, 24, 2);
      little(out, 0, 4);
      little(out, stride * height, 4);
      little(out, 2835, 4);
      little(out, 2835, 4);
      little(out, 0, 4);
      little(out, 0, 4);
    }
    int[] pixels = new int[width];
    byte[] row = new byte[stride];
    for (int index = 0; index < height; index++) {
      source.read(tga ? index : height - 1 - index, pixels);
      int offset = 0;
      for (int pixel : pixels) {
        int a = pixel >>> 24;
        for (int shift = 0; shift <= 16; shift += 8) {
          int component = (pixel >>> shift) & 255;
          if (!tga) component = (component * a + 255 * (255 - a) + 127) / 255;
          row[offset++] = (byte) component;
        }
        if (tga) row[offset++] = (byte) a;
      }
      out.write(row);
    }
  }
}
