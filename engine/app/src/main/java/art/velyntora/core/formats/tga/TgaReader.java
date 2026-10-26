package art.velyntora.core;

import art.velyntora.core.document.CanvasLimits;

import java.io.IOException;
import java.io.InputStream;

/** Bounded indexed, grayscale and true-color TGA, raw/RLE, with both axis origins. */
final class TgaReader {
  static final class Image {
    final int width, height;
    final int[] pixels;

    Image(int w, int h) {
      width = w;
      height = h;
      pixels = new int[w * h];
    }
  }

  private final InputStream input;
  private int consumed;

  private TgaReader(InputStream input) {
    this.input = input;
  }

  private int octet() throws IOException {
    if (++consumed > 67108864) throw new IOException("TGA demasiado grande");
    int value = input.read();
    if (value < 0) throw new IOException("TGA incompleto");
    return value;
  }

  private int word() throws IOException {
    return octet() | (octet() << 8);
  }

  private int color(int depth, int alphaBits) throws IOException {
    if (depth == 15 || depth == 16) {
      int value = word(), r = (value >> 10) & 31, g = (value >> 5) & 31, b = value & 31;
      return (alphaBits == 1 && (value & 32768) == 0 ? 0 : 255) << 24
          | ((r * 255 + 15) / 31) << 16
          | ((g * 255 + 15) / 31) << 8
          | (b * 255 + 15) / 31;
    }
    int b = octet(), g = octet(), r = octet(), a = depth == 32 ? octet() : 255;
    return (alphaBits == 8 ? a : 255) << 24 | r << 16 | g << 8 | b;
  }

  private int pixel(int type, int depth, int attributes, int[] palette, int first)
      throws IOException {
    if (type == 1) {
      int index = depth == 8 ? octet() : word();
      if (index < first || index - first >= palette.length)
        throw new IOException("Índice TGA inválido");
      return palette[index - first];
    }
    if (type == 3) {
      int value = octet(), a = depth == 16 ? octet() : 255;
      return (attributes == 8 ? a : 255) << 24 | value << 16 | value << 8 | value;
    }
    return color(depth, attributes);
  }

  static Image read(InputStream input) throws IOException {
    if (input == null) throw new IOException("Sin archivo");
    return new TgaReader(input).decode();
  }

  private Image decode() throws IOException {
    int id = octet(),
        colorMap = octet(),
        rawType = octet(),
        first = word(),
        count = word(),
        mapDepth = octet();
    word();
    word();
    int w = word(),
        h = word(),
        depth = octet(),
        descriptor = octet(),
        attributes = descriptor & 15,
        type = rawType >= 9 ? rawType - 8 : rawType;
    boolean rle = rawType >= 9;
    if (type < 1
        || type > 3
        || rawType != type && rawType != type + 8
        || (descriptor & 0xc0) != 0
        || w < 1
        || h < 1
        || w > CanvasLimits.MAX_SIDE
        || h > CanvasLimits.MAX_SIDE
        || (long) w * h > CanvasLimits.MAX_PIXELS) throw new IOException("TGA no compatible o demasiado grande");
    if (colorMap != 0 && colorMap != 1
        || type == 1
            && (colorMap != 1 || count == 0 || first + count > 65536 || depth != 8 && depth != 16)
        || type == 2 && (depth != 15 && depth != 16 && depth != 24 && depth != 32)
        || type == 3 && (depth != 8 && depth != 16)) throw new IOException("Tipo TGA incompatible");
    if (type == 2
            && !(attributes == 0
                || attributes == 1 && depth == 16
                || attributes == 8 && depth == 32)
        || type == 3 && !(attributes == 0 || attributes == 8 && depth == 16)
        || type == 1 && attributes != 0 && attributes != 1 && attributes != 8)
      throw new IOException("Alfa TGA incompatible");
    for (int i = 0; i < id; i++) octet();
    int[] palette = null;
    if (colorMap == 1) {
      if (count == 0
          || first + count > 65536
          || (mapDepth != 15 && mapDepth != 16 && mapDepth != 24 && mapDepth != 32))
        throw new IOException("Paleta TGA incompatible");
      palette = new int[count];
      for (int i = 0; i < count; i++)
        palette[i] = color(mapDepth, mapDepth == 32 ? 8 : mapDepth == 16 ? 1 : 0);
    }
    Image image = new Image(w, h);
    int index = 0;
    while (index < image.pixels.length) {
      int packet = rle ? octet() : 0, amount = rle ? (packet & 127) + 1 : 1;
      if (amount > image.pixels.length - index)
        throw new IOException("Paquete TGA fuera de límites");
      boolean repeated = rle && (packet & 128) != 0;
      int value = repeated ? pixel(type, depth, attributes, palette, first) : 0;
      for (int i = 0; i < amount; i++) {
        int p = repeated ? value : pixel(type, depth, attributes, palette, first),
            x = index % w,
            y = index / w;
        if ((descriptor & 0x20) == 0) y = h - 1 - y;
        if ((descriptor & 0x10) != 0) x = w - 1 - x;
        image.pixels[y * w + x] = p;
        index++;
      }
    }
    return image;
  }
}
