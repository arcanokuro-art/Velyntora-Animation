package art.velyntora.core;

import art.velyntora.core.document.CanvasLimits;

import java.io.*;
import java.util.*;
import java.util.zip.InflaterInputStream;

/** Bounded strip-based TIFF decoder: 8-bit RGB/gray/palette, chunky samples. */
final class TiffReader {
  static final class Image {
    final int width, height;
    final int[] pixels;

    Image(int w, int h) {
      width = w;
      height = h;
      pixels = new int[w * h];
    }
  }

  final byte[] data;
  final boolean little;
  final Map<Integer, int[]> tags = new HashMap<>();

  private TiffReader(byte[] bytes) throws IOException {
    data = bytes;
    if (data.length < 8) throw new IOException("TIFF incompleto");
    little = data[0] == 'I' && data[1] == 'I';
    if (!little && !(data[0] == 'M' && data[1] == 'M')) throw new IOException("TIFF incompatible");
    if (word(2) != 42) throw new IOException("TIFF incompatible");
    int offset = integer(4), count = word(offset);
    if (count > 256) throw new IOException("Demasiadas etiquetas TIFF");
    range(offset, 2L + count * 12L + 4);
    for (int i = 0; i < count; i++) {
      int p = offset + 2 + i * 12, id = word(p), type = word(p + 2), n = integer(p + 4);
      if (!wanted(id)) continue;
      if (tags.containsKey(id) || n < 1 || n > 16384 || (type != 3 && type != 4))
        throw new IOException("Etiqueta TIFF incompatible");
      int size = type == 3 ? 2 : 4, start = (long) n * size <= 4 ? p + 8 : integer(p + 8);
      range(start, (long) n * size);
      int[] values = new int[n];
      for (int j = 0; j < n; j++)
        values[j] = type == 3 ? word(start + j * 2) : integer(start + j * 4);
      tags.put(id, values);
    }
  }

  private static boolean wanted(int id) {
    return id == 256 || id == 257 || id == 258 || id == 259 || id == 262 || id == 273 || id == 274
        || id == 277 || id == 278 || id == 279 || id == 284 || id == 317 || id == 320 || id == 338
        || id == 339;
  }

  private void range(int offset, long size) throws IOException {
    if (offset < 0 || size < 0 || offset + size > data.length)
      throw new IOException("Offset TIFF fuera de límites");
  }

  private int word(int p) throws IOException {
    range(p, 2);
    return little
        ? (data[p] & 255) | (data[p + 1] & 255) << 8
        : (data[p] & 255) << 8 | (data[p + 1] & 255);
  }

  private int integer(int p) throws IOException {
    range(p, 4);
    long value = 0;
    for (int i = 0; i < 4; i++)
      value |= (long) (data[p + i] & 255) << (little ? i * 8 : (3 - i) * 8);
    if (value > Integer.MAX_VALUE) throw new IOException("Valor TIFF demasiado grande");
    return (int) value;
  }

  private int scalar(int id, int fallback) throws IOException {
    int[] values = tags.get(id);
    if (values == null) return fallback;
    if (values.length != 1) throw new IOException("Etiqueta escalar TIFF inválida");
    return values[0];
  }

  static Image read(InputStream input) throws IOException {
    if (input == null) throw new IOException("Sin archivo");
    return new TiffReader(OpenRasterArchive.bounded(input, 67108864)).decode();
  }

  private Image decode() throws IOException {
    int w = scalar(256, 0),
        h = scalar(257, 0),
        samples = scalar(277, 1),
        photo = scalar(262, -1),
        compression = scalar(259, 1),
        orientation = scalar(274, 1),
        rows = scalar(278, h),
        predictor = scalar(317, 1);
    if (w < 1
        || h < 1
        || w > CanvasLimits.MAX_SIDE
        || h > CanvasLimits.MAX_SIDE
        || (long) w * h > CanvasLimits.MAX_PIXELS
        || samples < 1
        || samples > 4
        || orientation < 1
        || orientation > 8
        || rows < 1
        || scalar(284, 1) != 1
        || (predictor != 1 && predictor != 2)
        || (photo == 2
            ? samples < 3
            : photo == 0 || photo == 1 ? samples > 2 : photo == 3 ? samples != 1 : true))
      throw new IOException("TIFF no compatible o demasiado grande");
    int[] bits = tags.get(258);
    if (bits == null || bits.length != samples)
      throw new IOException("Profundidad TIFF incompatible");
    for (int b : bits) if (b != 8) throw new IOException("Sólo TIFF de 8 bits por canal");
    int[] format = tags.get(339);
    if (format != null) {
      if (format.length != samples) throw new IOException("Muestras inválidas");
      for (int value : format) if (value != 1) throw new IOException("Muestras TIFF no enteras");
    }
    if (compression != 1
        && compression != 5
        && compression != 8
        && compression != 32946
        && compression != 32773) throw new IOException("Compresión TIFF no compatible");
    int[] offsets = tags.get(273),
        counts = tags.get(279),
        palette = tags.get(320),
        extras = tags.get(338);
    int strips = (int) (((long) h + rows - 1) / rows);
    if (offsets == null
        || counts == null
        || offsets.length != strips
        || counts.length != strips
        || (photo == 3 && (palette == null || palette.length != 768)))
      throw new IOException("Tiras o paleta TIFF inválidas");
    int alphaMode = 0;
    if (extras != null) {
      if (extras.length != 1
          || extras[0] > 2
          || (photo == 2 ? samples != 4 : photo == 0 || photo == 1 ? samples != 2 : true))
        throw new IOException("Alfa TIFF no compatible");
      alphaMode = extras[0];
    }
    Image image = new Image(orientation >= 5 ? h : w, orientation >= 5 ? w : h);
    int stride = w * samples;
    for (int strip = 0; strip < strips; strip++) {
      int top = (int) ((long) strip * rows),
          height = Math.min(rows, h - top),
          expected = stride * height;
      range(offsets[strip], counts[strip]);
      byte[] bytes = unpack(offsets[strip], counts[strip], expected, compression);
      if (predictor == 2)
        for (int y = 0; y < height; y++)
          for (int x = samples; x < stride; x++)
            bytes[y * stride + x] =
                (byte) ((bytes[y * stride + x] & 255) + (bytes[y * stride + x - samples] & 255));
      for (int yy = 0; yy < height; yy++)
        for (int x = 0; x < w; x++) {
          int p = yy * stride + x * samples, r, g, b, a = 255;
          if (photo == 2) {
            r = bytes[p] & 255;
            g = bytes[p + 1] & 255;
            b = bytes[p + 2] & 255;
          } else if (photo == 3) {
            int index = bytes[p] & 255;
            r = (palette[index] + 128) / 257;
            g = (palette[index + 256] + 128) / 257;
            b = (palette[index + 512] + 128) / 257;
          } else {
            r = g = b = bytes[p] & 255;
            if (photo == 0) r = g = b = 255 - r;
          }
          if (alphaMode != 0) {
            a = bytes[p + samples - 1] & 255;
            if (alphaMode == 1) {
              if (a == 0) r = g = b = 0;
              else {
                r = Math.min(255, (r * 255 + a / 2) / a);
                g = Math.min(255, (g * 255 + a / 2) / a);
                b = Math.min(255, (b * 255 + a / 2) / a);
              }
            }
          }
          int y = top + yy, dx = x, dy = y;
          switch (orientation) {
            case 2:
              dx = w - 1 - x;
              break;
            case 3:
              dx = w - 1 - x;
              dy = h - 1 - y;
              break;
            case 4:
              dy = h - 1 - y;
              break;
            case 5:
              dx = y;
              dy = x;
              break;
            case 6:
              dx = h - 1 - y;
              dy = x;
              break;
            case 7:
              dx = h - 1 - y;
              dy = w - 1 - x;
              break;
            case 8:
              dx = y;
              dy = w - 1 - x;
              break;
          }
          image.pixels[dy * image.width + dx] = a << 24 | r << 16 | g << 8 | b;
        }
    }
    return image;
  }

  private byte[] unpack(int offset, int count, int expected, int compression) throws IOException {
    if (compression == 1) {
      if (count != expected) throw new IOException("Tira TIFF incompleta");
      return Arrays.copyOfRange(data, offset, offset + count);
    }
    if (compression == 8 || compression == 32946) {
      try (InputStream input =
          new InflaterInputStream(new ByteArrayInputStream(data, offset, count))) {
        byte[] out = new byte[expected];
        new DataInputStream(input).readFully(out);
        if (input.read() != -1) throw new IOException("Tira TIFF demasiado larga");
        return out;
      }
    }
    if (compression == 5) return lzw(offset, count, expected);
    byte[] out = new byte[expected];
    int p = offset, end = offset + count, dest = 0;
    while (dest < expected && p < end) {
      int n = data[p++];
      if (n == -128) continue;
      if (n >= 0) {
        int length = n + 1;
        if (length > end - p || length > expected - dest)
          throw new IOException("PackBits fuera de límites");
        System.arraycopy(data, p, out, dest, length);
        p += length;
        dest += length;
      } else {
        int length = 1 - n;
        if (p >= end || length > expected - dest)
          throw new IOException("PackBits fuera de límites");
        Arrays.fill(out, dest, dest + length, data[p++]);
        dest += length;
      }
    }
    if (dest != expected) throw new IOException("PackBits incompleto");
    return out;
  }

  private byte[] lzw(int offset, int count, int expected) throws IOException {
    byte[] out = new byte[expected], suffix = new byte[4096], stack = new byte[4096];
    int[] prefix = new int[4096];
    int bit = 0, size = 9, next = 258, previous = -1, first = 0, dest = 0;
    boolean ended = false;
    while (bit + size <= (long) count * 8) {
      int code = 0;
      for (int i = 0; i < size; i++) {
        int position = bit++;
        code = code << 1 | ((data[offset + position / 8] >> (7 - position % 8)) & 1);
      }
      if (code == 256) {
        size = 9;
        next = 258;
        previous = -1;
        continue;
      }
      if (code == 257) {
        ended = true;
        break;
      }
      if (previous < 0) {
        if (code > 255 || dest >= expected) throw new IOException("LZW inválido");
        out[dest++] = (byte) code;
        first = code;
        previous = code;
        continue;
      }
      int original = code, depth = 0;
      if (code >= next) {
        if (code != next) throw new IOException("Código LZW inválido");
        stack[depth++] = (byte) first;
        code = previous;
      }
      while (code >= 256) {
        if (code >= next || depth >= 4095) throw new IOException("Diccionario LZW inválido");
        stack[depth++] = suffix[code];
        code = prefix[code];
      }
      first = code;
      stack[depth++] = (byte) first;
      if (depth > expected - dest) throw new IOException("LZW demasiado largo");
      while (depth > 0) out[dest++] = stack[--depth];
      if (next < 4096) {
        prefix[next] = previous;
        suffix[next++] = (byte) first;
        if (size < 12 && next == (1 << size) - 1) size++;
      }
      previous = original;
    }
    if (!ended || dest != expected) throw new IOException("LZW incompleto");
    return out;
  }
}
