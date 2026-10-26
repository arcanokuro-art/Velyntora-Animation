package art.velyntora.core;

import art.velyntora.core.document.CanvasLimits;

import java.io.*;

/** PCX RLE reader: 8-bit RGB/indexed and planar 1-bit EGA/monochrome. */
final class PcxReader {
  static final class Image {
    final int width, height;
    final int[] pixels;

    Image(int w, int h) {
      width = w;
      height = h;
      pixels = new int[w * h];
    }
  }

  private static int u16(byte[] b, int p) {
    return (b[p] & 255) | (b[p + 1] & 255) << 8;
  }

  static Image read(InputStream input) throws IOException {
    byte[] b = OpenRasterArchive.bounded(input, 67108864);
    if (b.length < 128 || (b[0] & 255) != 10 || b[2] != 1)
      throw new IOException("PCX incompatible");
    int bits = b[3] & 255,
        planes = b[65] & 255,
        w = u16(b, 8) - u16(b, 4) + 1,
        h = u16(b, 10) - u16(b, 6) + 1,
        stride = u16(b, 66);
    if (w < 1
        || h < 1
        || w > CanvasLimits.MAX_SIDE
        || h > CanvasLimits.MAX_SIDE
        || (long) w * h > CanvasLimits.MAX_PIXELS
        || stride < ((long) w * bits + 7) / 8
        || stride == 0
        || (bits == 8 ? planes != 1 && planes != 3 : bits != 1 || planes < 1 || planes > 4))
      throw new IOException("PCX no compatible o demasiado grande");
    int end = b.length;
    int[] palette = new int[256];
    if (bits == 8 && planes == 1) {
      if (end < 897 || (b[end - 769] & 255) != 12) throw new IOException("Paleta PCX ausente");
      end -= 769;
      for (int i = 0; i < 256; i++) {
        int p = end + 1 + i * 3;
        palette[i] = 0xff000000 | (b[p] & 255) << 16 | (b[p + 1] & 255) << 8 | (b[p + 2] & 255);
      }
    } else
      for (int i = 0; i < 16; i++) {
        int p = 16 + i * 3;
        palette[i] = 0xff000000 | (b[p] & 255) << 16 | (b[p + 1] & 255) << 8 | (b[p + 2] & 255);
      }
    // Old monochrome PCX convention when no explicit two-color palette was written.
    if (bits == 1 && planes == 1 && palette[0] == palette[1]) {
      palette[0] = 0xff000000;
      palette[1] = 0xffffffff;
    }
    Image image = new Image(w, h);
    byte[] row = new byte[stride * planes];
    int pos = 128;
    for (int y = 0; y < h; y++) {
      int dest = 0;
      while (dest < row.length) {
        if (pos >= end) throw new IOException("PCX incompleto");
        int value = b[pos++] & 255, count = 1;
        if ((value & 192) == 192) {
          count = value & 63;
          if (count == 0 || pos >= end) throw new IOException("RLE PCX inválido");
          value = b[pos++] & 255;
        }
        if (count > row.length - dest) throw new IOException("RLE PCX fuera de fila");
        java.util.Arrays.fill(row, dest, dest + count, (byte) value);
        dest += count;
      }
      for (int x = 0; x < w; x++) {
        int pixel;
        if (bits == 8 && planes == 3)
          pixel =
              0xff000000
                  | (row[x] & 255) << 16
                  | (row[stride + x] & 255) << 8
                  | (row[2 * stride + x] & 255);
        else {
          int index = 0;
          if (bits == 8) index = row[x] & 255;
          else
            for (int plane = 0; plane < planes; plane++)
              index |= ((row[plane * stride + x / 8] >> (7 - x % 8)) & 1) << plane;
          pixel = palette[index];
        }
        image.pixels[y * w + x] = pixel;
      }
    }
    return image;
  }
}
