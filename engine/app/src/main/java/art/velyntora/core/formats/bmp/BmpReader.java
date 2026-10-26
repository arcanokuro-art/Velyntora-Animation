package art.velyntora.core;

import art.velyntora.core.document.CanvasLimits;

import java.io.*;

/** Bounded Windows/OS2 BMP reader: indexed RGB, bitfields, and RLE4/RLE8. */
final class BmpReader {
  static final class Image {
    final int width, height;
    final int[] pixels;

    Image(int w, int h) {
      width = w;
      height = h;
      pixels = new int[w * h];
    }
  }

  private final byte[] b;
  private int pos, limit;

  private BmpReader(byte[] b) {
    this.b = b;
  }

  private long n(int p, int bytes) throws IOException {
    if (p < 0 || (long) p + bytes > b.length) throw new IOException("BMP incompleto");
    long n = 0;
    for (int i = 0; i < bytes; i++) n |= (long) (b[p + i] & 255) << (8 * i);
    return n;
  }

  private int octet() throws IOException {
    if (pos >= limit) throw new IOException("BMP RLE incompleto");
    return b[pos++] & 255;
  }

  private static int component(long p, long mask) {
    if (mask == 0) return 255;
    int shift = Long.numberOfTrailingZeros(mask);
    long maximum = mask >>> shift;
    return (int) ((((p & mask) >>> shift) * 255 + maximum / 2) / maximum);
  }

  private static boolean contiguous(long mask) {
    if (mask == 0) return false;
    long m = mask >>> Long.numberOfTrailingZeros(mask);
    return (m & (m + 1)) == 0;
  }

  private static void checkMask(long mask, int depth) throws IOException {
    if (!contiguous(mask) || (mask >>> depth) != 0 && depth < 32)
      throw new IOException("Máscara BMP inválida");
  }

  static Image read(InputStream input) throws IOException {
    return new BmpReader(OpenRasterArchive.bounded(input, 67108864)).decode();
  }

  private Image decode() throws IOException {
    if (n(0, 2) != 0x4d42) throw new IOException("No es BMP");
    long length = n(2, 4), offset = n(10, 4), header = n(14, 4);
    if (length < 26 || length > b.length || offset > length)
      throw new IOException("Tamaño BMP inválido");
    limit = (int) length;
    boolean core = header == 12;
    if (!core && header != 40 && header != 52 && header != 56 && header != 108 && header != 124)
      throw new IOException("Cabecera BMP incompatible");
    if (14 + header > b.length) throw new IOException("Cabecera BMP truncada");
    int w = core ? (int) n(18, 2) : (int) n(18, 4),
        signedH = core ? (int) n(20, 2) : (int) n(22, 4),
        planes = (int) n(core ? 22 : 26, 2),
        depth = (int) n(core ? 24 : 28, 2);
    if (w < 1
        || signedH == Integer.MIN_VALUE
        || signedH == 0
        || w > CanvasLimits.MAX_SIDE
        || Math.abs(signedH) > 8192
        || (long) w * Math.abs(signedH) > CanvasLimits.MAX_PIXELS
        || planes != 1) throw new IOException("Dimensiones BMP inválidas");
    int h = Math.abs(signedH);
    boolean top = signedH < 0;
    long compression = core ? 0 : n(30, 4), colors = core ? 0 : n(46, 4);
    if (depth != 1 && depth != 4 && depth != 8 && depth != 16 && depth != 24 && depth != 32)
      throw new IOException("Profundidad BMP incompatible");
    if (compression != 0
            && compression != 1
            && compression != 2
            && compression != 3
            && compression != 6
        || compression == 1 && depth != 8
        || compression == 2 && depth != 4
        || (compression == 3 || compression == 6) && depth != 16 && depth != 32
        || top && (compression == 1 || compression == 2))
      throw new IOException("Compresión BMP incompatible");
    int table = 14 + (int) header;
    long red = depth == 16 ? 0x7c00 : 0xff0000,
        green = depth == 16 ? 0x3e0 : 0xff00,
        blue = depth == 16 ? 0x1f : 0xff,
        alpha = 0;
    if (compression == 3 || compression == 6) {
      int masks = header >= 52 ? 54 : table;
      red = n(masks, 4);
      green = n(masks + 4, 4);
      blue = n(masks + 8, 4);
      if (compression == 6 || header >= 56) alpha = n(masks + 12, 4);
      if (header == 40) table += compression == 6 ? 16 : 12;
      checkMask(red, depth);
      checkMask(green, depth);
      checkMask(blue, depth);
      if (alpha != 0) checkMask(alpha, depth);
      if ((red & green) != 0
          || (red & blue) != 0
          || (green & blue) != 0
          || ((red | green | blue) & alpha) != 0) throw new IOException("Máscaras BMP solapadas");
    }
    int[] palette = null;
    if (depth <= 8) {
      long count = colors == 0 ? 1 << depth : colors;
      if (count < 1 || count > (1 << depth)) throw new IOException("Paleta BMP inválida");
      palette = new int[(int) count];
      int size = core ? 3 : 4;
      for (int i = 0; i < palette.length; i++) {
        int p = table + i * size;
        palette[i] = 0xff000000 | (int) n(p + 2, 1) << 16 | (int) n(p + 1, 1) << 8 | (int) n(p, 1);
      }
      table += palette.length * size;
    }
    if (offset < table || offset > b.length) throw new IOException("Posición BMP inválida");
    Image out = new Image(w, h);
    pos = (int) offset;
    if (compression == 1 || compression == 2) {
      java.util.Arrays.fill(out.pixels, palette[0]);
      rle(out, palette, depth);
      return out;
    }
    long stride = (((long) w * depth + 31) / 32) * 4;
    if (offset + stride * h > length) throw new IOException("Píxeles BMP truncados");
    boolean anyAlpha = false;
    for (int row = 0; row < h; row++) {
      int base = (int) (offset + row * stride), y = top ? row : h - 1 - row;
      for (int x = 0; x < w; x++) {
        int pixel;
        if (depth <= 8) {
          int index =
              depth == 8
                  ? (b[base + x] & 255)
                  : depth == 4
                      ? (b[base + x / 2] >> ((1 - x % 2) * 4)) & 15
                      : (b[base + x / 8] >> (7 - x % 8)) & 1;
          if (index >= palette.length) throw new IOException("Índice BMP fuera de paleta");
          pixel = palette[index];
        } else if (depth == 24) {
          int p = base + x * 3;
          pixel = 0xff000000 | (int) n(p + 2, 1) << 16 | (int) n(p + 1, 1) << 8 | (int) n(p, 1);
        } else {
          long value = n(base + x * (depth / 8), depth / 8);
          int a =
              alpha != 0
                  ? component(value, alpha)
                  : depth == 32 && compression == 0 ? (int) (value >>> 24) : 255;
          anyAlpha |= a != 0;
          pixel =
              a << 24
                  | component(value, red) << 16
                  | component(value, green) << 8
                  | component(value, blue);
        }
        out.pixels[y * w + x] = pixel;
      }
    }
    if (depth == 32 && compression == 0 && !anyAlpha)
      for (int i = 0; i < out.pixels.length; i++) out.pixels[i] |= 0xff000000;
    return out;
  }

  private void rle(Image image, int[] palette, int depth) throws IOException {
    int x = 0, row = 0;
    while (true) {
      int count = octet(), value = octet();
      if (count != 0) {
        for (int i = 0; i < count; i++) {
          int index = depth == 8 ? value : (value >> ((1 - i % 2) * 4)) & 15;
          put(image, palette, x++, row, index);
        }
      } else if (value == 0) {
        x = 0;
        row++;
        if (row > image.height) throw new IOException("Filas BMP RLE fuera de límites");
      } else if (value == 1) return;
      else if (value == 2) {
        x += octet();
        row += octet();
        if (x > image.width || row >= image.height)
          throw new IOException("Desplazamiento BMP RLE inválido");
      } else {
        int bytes = depth == 8 ? value : (value + 1) / 2, packed = 0;
        for (int i = 0; i < value; i++) {
          if (depth == 8 || i % 2 == 0) packed = octet();
          int index = depth == 8 ? packed : (packed >> ((1 - i % 2) * 4)) & 15;
          put(image, palette, x++, row, index);
        }
        if ((bytes & 1) != 0) octet();
      }
    }
  }

  private void put(Image image, int[] palette, int x, int row, int index) throws IOException {
    if (x < 0 || x >= image.width || row < 0 || row >= image.height || index >= palette.length)
      throw new IOException("Paquete BMP RLE inválido");
    image.pixels[(image.height - 1 - row) * image.width + x] = palette[index];
  }
}
