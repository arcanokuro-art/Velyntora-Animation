package art.velyntora.core;

import art.velyntora.core.document.CanvasLimits;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/** Single-frame GIF89a. Exact palette up to 255 colors; bounded RGB cube otherwise. */
final class GifWriter {
  private static void little(OutputStream out, int value) throws IOException {
    out.write(value & 255);
    out.write(value >>> 8 & 255);
  }

  static void write(OutputStream out, int width, int height, RasterFileWriter.RowSource source)
      throws IOException {
    if (out == null
        || source == null
        || width < 1
        || height < 1
        || width > CanvasLimits.MAX_SIDE
        || height > CanvasLimits.MAX_SIDE
        || (long) width * height > CanvasLimits.MAX_PIXELS)
      throw new IllegalArgumentException("Invalid GIF dimensions");
    Map<Integer, Integer> palette = new LinkedHashMap<>();
    int[] row = new int[width];
    boolean quantized = false;
    scan:
    for (int y = 0; y < height; y++) {
      source.read(y, row);
      for (int pixel : row)
        if ((pixel >>> 24) >= 128) {
          int rgb = pixel & 0xffffff;
          if (!palette.containsKey(rgb)) {
            if (palette.size() == 255) {
              quantized = true;
              break scan;
            }
            palette.put(rgb, palette.size() + 1);
          }
        }
    }
    out.write(new byte[] {'G', 'I', 'F', '8', '9', 'a'});
    little(out, width);
    little(out, height);
    out.write(0xf7);
    out.write(0);
    out.write(0);
    int[] colors = new int[256];
    if (quantized)
      for (int r = 0; r < 6; r++)
        for (int g = 0; g < 6; g++)
          for (int b = 0; b < 6; b++)
            colors[1 + r * 36 + g * 6 + b] = r * 51 << 16 | g * 51 << 8 | b * 51;
    else
      for (Map.Entry<Integer, Integer> entry : palette.entrySet())
        colors[entry.getValue()] = entry.getKey();
    for (int color : colors) {
      out.write(color >>> 16 & 255);
      out.write(color >>> 8 & 255);
      out.write(color & 255);
    }
    // Transparency is binary in GIF: alpha below 128 uses palette index zero.
    out.write(new byte[] {0x21, (byte) 0xf9, 4, 1, 0, 0, 0, 0, 0x2c});
    little(out, 0);
    little(out, 0);
    little(out, width);
    little(out, height);
    out.write(0);
    out.write(8);
    Codes codes = new Codes(out);
    int[] keys = new int[8192], values = new int[8192];
    Arrays.fill(keys, -1);
    int next = 258, bits = 9, prefix = -1;
    codes.write(256, bits);
    for (int y = 0; y < height; y++) {
      source.read(y, row);
      for (int pixel : row) {
        int index = 0;
        if ((pixel >>> 24) >= 128) {
          if (quantized)
            index =
                1
                    + ((pixel >>> 16 & 255) + 25) / 51 * 36
                    + ((pixel >>> 8 & 255) + 25) / 51 * 6
                    + ((pixel & 255) + 25) / 51;
          else {
            Integer found = palette.get(pixel & 0xffffff);
            if (found == null) throw new IOException("Image changed during GIF export");
            index = found;
          }
        }
        if (prefix < 0) {
          prefix = index;
          continue;
        }
        int key = prefix << 8 | index, slot = (key * 0x9e3779b9 >>> 19) & 8191;
        while (keys[slot] != -1 && keys[slot] != key) slot = (slot + 1) & 8191;
        if (keys[slot] == key) {
          prefix = values[slot];
          continue;
        }
        codes.write(prefix, bits);
        if (next < 4096) {
          keys[slot] = key;
          values[slot] = next++;
          if (bits < 12 && next > (1 << bits)) bits++;
        } else {
          codes.write(256, bits);
          Arrays.fill(keys, -1);
          next = 258;
          bits = 9;
        }
        prefix = index;
      }
    }
    if (prefix >= 0) codes.write(prefix, bits);
    // The decoder adds its last dictionary entry after the final prefix.
    if (bits < 12 && next == (1 << bits)) bits++;
    codes.write(257, bits);
    codes.finish();
    out.write(0x3b);
  }

  private static final class Codes {
    final OutputStream out;
    final byte[] block = new byte[255];
    int count, buffer, pending;

    Codes(OutputStream output) {
      out = output;
    }

    void write(int code, int bits) throws IOException {
      buffer |= code << pending;
      pending += bits;
      while (pending >= 8) {
        byteValue(buffer & 255);
        buffer >>>= 8;
        pending -= 8;
      }
    }

    void byteValue(int value) throws IOException {
      block[count++] = (byte) value;
      if (count == 255) flush();
    }

    void flush() throws IOException {
      if (count > 0) {
        out.write(count);
        out.write(block, 0, count);
        count = 0;
      }
    }

    void finish() throws IOException {
      if (pending > 0) byteValue(buffer & 255);
      flush();
      out.write(0);
    }
  }
}
