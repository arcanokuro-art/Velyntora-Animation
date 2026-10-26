package art.velyntora.core;

import java.io.*;
import java.util.*;

/** Bounded Windows icon interchange; PNG or uncompressed indexed/RGB DIB images. */
final class IcoCodec {
  static final class Image {
    final int width, height;
    final int[] pixels;

    Image(int w, int h, int[] p) {
      width = w;
      height = h;
      pixels = p;
    }
  }

  interface PngDecoder {
    Image decode(byte[] png) throws IOException;
  }

  private static int number(byte[] b, int p, int n) throws IOException {
    if (p < 0 || (long) p + n > b.length) throw new IOException("ICO incompleto");
    long value = 0;
    for (int i = 0; i < n; i++) value |= (long) (b[p + i] & 255) << (i * 8);
    if (value > Integer.MAX_VALUE) throw new IOException("Valor ICO demasiado grande");
    return (int) value;
  }

  private static void little(OutputStream out, int v, int n) throws IOException {
    for (int i = 0; i < n; i++) out.write(v >>> (i * 8) & 255);
  }

  static Image read(InputStream input, PngDecoder decoder) throws IOException {
    byte[] b = OpenRasterArchive.bounded(input, 16777216);
    if (number(b, 0, 2) != 0 || number(b, 2, 2) != 1) throw new IOException("No es un icono ICO");
    int count = number(b, 4, 2);
    if (count < 1 || count > 64 || (long) 6 + 16 * count > b.length)
      throw new IOException("Directorio ICO inválido");
    List<int[]> entries = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      int p = 6 + i * 16, w = b[p] & 255, h = b[p + 1] & 255;
      w = w == 0 ? 256 : w;
      h = h == 0 ? 256 : h;
      int length = number(b, p + 8, 4), offset = number(b, p + 12, 4), depth = number(b, p + 6, 2);
      if (length < 8 || offset < 6 + 16 * count || (long) offset + length > b.length)
        throw new IOException("Entrada ICO fuera de límites");
      entries.add(new int[] {w, h, offset, length, depth});
    }
    entries.sort(
        (a, c) -> {
          int area = Integer.compare(c[0] * c[1], a[0] * a[1]);
          return area != 0 ? area : Integer.compare(c[4], a[4]);
        });
    IOException failure = new IOException("ICO sin imagen compatible");
    for (int[] entry : entries)
      try {
        return decode(
            Arrays.copyOfRange(b, entry[2], entry[2] + entry[3]), entry[0], entry[1], decoder);
      } catch (IOException e) {
        failure = e;
      }
    throw failure;
  }

  private static Image decode(byte[] b, int w, int h, PngDecoder decoder) throws IOException {
    if (b.length >= 24 && b[0] == (byte) 137 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
      byte[] signature = {(byte) 137, 80, 78, 71, 13, 10, 26, 10};
      for (int i = 0; i < 8; i++) if (b[i] != signature[i]) throw new IOException("PNG inválido");
      int pw = 0, ph = 0;
      for (int i = 0; i < 4; i++) {
        pw = pw << 8 | (b[16 + i] & 255);
        ph = ph << 8 | (b[20 + i] & 255);
      }
      if (pw != w || ph != h || decoder == null)
        throw new IOException("Dimensiones PNG/ICO incompatibles");
      Image image = decoder.decode(b);
      if (image == null
          || image.width != w
          || image.height != h
          || image.pixels == null
          || image.pixels.length != w * h) throw new IOException("PNG ICO inválido");
      return image;
    }
    int header = number(b, 0, 4),
        dw = number(b, 4, 4),
        dh = number(b, 8, 4),
        bits = number(b, 14, 2);
    if (header < 40
        || header > b.length
        || dw != w
        || dh != h * 2
        || number(b, 12, 2) != 1
        || number(b, 16, 4) != 0
        || (bits != 1 && bits != 4 && bits != 8 && bits != 24 && bits != 32))
      throw new IOException("DIB ICO incompatible");
    int colors = bits <= 8 ? number(b, 32, 4) : 0;
    if (bits <= 8 && colors == 0) colors = 1 << bits;
    if (colors > (1 << Math.min(bits, 8))) throw new IOException("Paleta ICO inválida");
    int stride = ((w * bits + 31) / 32) * 4,
        maskStride = ((w + 31) / 32) * 4,
        pixelsStart = header + colors * 4,
        maskStart = pixelsStart + stride * h;
    if ((long) maskStart + maskStride * h > b.length)
      throw new IOException("Píxeles o máscara ICO incompletos");
    int[] palette = new int[colors];
    for (int i = 0; i < colors; i++) {
      int p = header + i * 4;
      palette[i] = 0xff000000 | (b[p + 2] & 255) << 16 | (b[p + 1] & 255) << 8 | (b[p] & 255);
    }
    int[] pixels = new int[w * h];
    boolean hasAlpha = false;
    for (int y = 0; y < h; y++)
      for (int x = 0; x < w; x++) {
        int row = pixelsStart + (h - 1 - y) * stride, pixel;
        if (bits <= 8) {
          int packed = b[row + x * bits / 8] & 255,
              index = packed >>> (8 - bits - x * bits % 8) & ((1 << bits) - 1);
          if (index >= colors) throw new IOException("Índice de paleta ICO inválido");
          pixel = palette[index];
        } else {
          int p = row + x * (bits / 8), alpha = bits == 32 ? b[p + 3] & 255 : 255;
          hasAlpha |= bits == 32 && alpha != 0;
          pixel = alpha << 24 | (b[p + 2] & 255) << 16 | (b[p + 1] & 255) << 8 | (b[p] & 255);
        }
        pixels[y * w + x] = pixel;
      }
    for (int y = 0; y < h; y++)
      for (int x = 0; x < w; x++) {
        int p = y * w + x,
            mask = b[maskStart + (h - 1 - y) * maskStride + x / 8] >>> (7 - x % 8) & 1;
        if (bits == 32 && !hasAlpha) pixels[p] |= 0xff000000;
        if (mask == 1) pixels[p] &= 0xffffff;
      }
    return new Image(w, h, pixels);
  }

  static void write(OutputStream out, int width, int height, RasterFileWriter.RowSource source)
      throws IOException {
    if (out == null || source == null || width < 1 || height < 1 || width > 256 || height > 256)
      throw new IllegalArgumentException("ICO dimensions must be 1–256");
    int maskStride = ((width + 31) / 32) * 4, size = 40 + width * height * 4 + maskStride * height;
    little(out, 0, 2);
    little(out, 1, 2);
    little(out, 1, 2);
    out.write(width & 255);
    out.write(height & 255);
    out.write(0);
    out.write(0);
    little(out, 1, 2);
    little(out, 32, 2);
    little(out, size, 4);
    little(out, 22, 4);
    little(out, 40, 4);
    little(out, width, 4);
    little(out, height * 2, 4);
    little(out, 1, 2);
    little(out, 32, 2);
    little(out, 0, 4);
    little(out, width * height * 4, 4);
    for (int i = 0; i < 4; i++) little(out, 0, 4);
    int[] row = new int[width];
    for (int y = height - 1; y >= 0; y--) {
      source.read(y, row);
      for (int pixel : row) little(out, pixel, 4);
    }
    byte[] mask = new byte[maskStride];
    for (int y = height - 1; y >= 0; y--) {
      source.read(y, row);
      Arrays.fill(mask, (byte) 0);
      for (int x = 0; x < width; x++) if ((row[x] >>> 24) == 0) mask[x / 8] |= 1 << (7 - x % 8);
      out.write(mask);
    }
  }
}
