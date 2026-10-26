package art.velyntora.core;

import art.velyntora.core.document.CanvasLimits;

import java.io.IOException;
import java.io.OutputStream;

/** Baseline little-endian, uncompressed RGB TIFF with unassociated alpha. */
final class TiffWriter {
  private static void number(OutputStream out, int value, int bytes) throws IOException {
    for (int i = 0; i < bytes; i++) out.write((value >>> (8 * i)) & 255);
  }

  private static void tag(OutputStream out, int id, int type, int count, int value)
      throws IOException {
    number(out, id, 2);
    number(out, type, 2);
    number(out, count, 4);
    number(out, value, 4);
  }

  static void write(OutputStream output, int width, int height, RasterFileWriter.RowSource source)
      throws IOException {
    if (output == null
        || source == null
        || width < 1
        || height < 1
        || width > CanvasLimits.MAX_SIDE
        || height > CanvasLimits.MAX_SIDE
        || (long) width * height > CanvasLimits.MAX_PIXELS)
      throw new IllegalArgumentException("Invalid TIFF dimensions");
    final int fields = 12, bitsOffset = 8 + 2 + fields * 12 + 4, pixelsOffset = bitsOffset + 8;
    output.write('I');
    output.write('I');
    number(output, 42, 2);
    number(output, 8, 4);
    number(output, fields, 2);
    tag(output, 256, 4, 1, width);
    tag(output, 257, 4, 1, height);
    tag(output, 258, 3, 4, bitsOffset);
    tag(output, 259, 3, 1, 1);
    tag(output, 262, 3, 1, 2);
    tag(output, 273, 4, 1, pixelsOffset);
    tag(output, 274, 3, 1, 1);
    tag(output, 277, 3, 1, 4);
    tag(output, 278, 4, 1, height);
    tag(output, 279, 4, 1, width * height * 4);
    tag(output, 284, 3, 1, 1);
    tag(output, 338, 3, 1, 2);
    number(output, 0, 4);
    for (int i = 0; i < 4; i++) number(output, 8, 2);
    int[] pixels = new int[width];
    byte[] row = new byte[width * 4];
    for (int y = 0; y < height; y++) {
      source.read(y, pixels);
      int offset = 0;
      for (int p : pixels) {
        row[offset++] = (byte) (p >>> 16);
        row[offset++] = (byte) (p >>> 8);
        row[offset++] = (byte) p;
        row[offset++] = (byte) (p >>> 24);
      }
      output.write(row);
    }
  }
}
