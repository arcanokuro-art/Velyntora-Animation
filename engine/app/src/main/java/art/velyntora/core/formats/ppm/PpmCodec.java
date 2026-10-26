package art.velyntora.core;

import art.velyntora.core.document.CanvasLimits;

import java.io.*;
import java.nio.charset.StandardCharsets;

/** Netpbm P1–P6 bitmap/grayscale/RGB interchange, bounded and normalized to opaque ARGB8. */
final class PpmCodec {
  static final class Image {
    final int width, height;
    final int[] pixels;

    Image(int w, int h) {
      width = w;
      height = h;
      pixels = new int[w * h];
    }
  }

  private final byte[] bytes;
  private int position, delimiter;

  private PpmCodec(byte[] data) {
    bytes = data;
  }

  private static boolean whitespace(int b) {
    return b == 32 || b == 9 || b == 10 || b == 13 || b == 11 || b == 12;
  }

  private void skip() {
    while (position < bytes.length) {
      int b = bytes[position] & 255;
      if (whitespace(b)) position++;
      else if (b == '#') {
        while (position < bytes.length && bytes[position] != 10 && bytes[position] != 13)
          position++;
      } else break;
    }
  }

  private String token() throws IOException {
    skip();
    int start = position;
    while (position < bytes.length
        && !whitespace(bytes[position] & 255)
        && bytes[position] != '#') {
      if (position - start >= 10) throw new IOException("Token PPM demasiado largo");
      position++;
    }
    if (start == position) throw new IOException("PPM incompleto");
    String value = new String(bytes, start, position - start, StandardCharsets.US_ASCII);
    delimiter = position < bytes.length ? bytes[position] & 255 : -1;
    if (whitespace(delimiter)) position++;
    return value;
  }

  private int integer() throws IOException {
    String value = token();
    long number = 0;
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      if (c < '0' || c > '9') throw new IOException("Número PPM inválido");
      number = number * 10 + c - '0';
      if (number > Integer.MAX_VALUE) throw new IOException("Número PPM demasiado grande");
    }
    return (int) number;
  }

  static Image read(InputStream input) throws IOException {
    PpmCodec reader = new PpmCodec(OpenRasterArchive.bounded(input, 67108864));
    String magic = reader.token();
    if (magic.length() != 2
        || magic.charAt(0) != 'P'
        || magic.charAt(1) < '1'
        || magic.charAt(1) > '6') throw new IOException("Netpbm incompatible");
    int kind = magic.charAt(1) - '0';
    boolean binary = kind >= 4, bitmap = kind == 1 || kind == 4, rgb = kind == 3 || kind == 6;
    int width = reader.integer(),
        height = reader.integer(),
        maximum = bitmap ? 1 : reader.integer();
    if (width < 1
        || height < 1
        || width > CanvasLimits.MAX_SIDE
        || height > CanvasLimits.MAX_SIDE
        || (long) width * height > CanvasLimits.MAX_PIXELS
        || maximum < 1
        || maximum > 65535) throw new IOException("Netpbm demasiado grande o inválido");
    if (binary) {
      if (!whitespace(reader.delimiter)) throw new IOException("Cabecera Netpbm incompleta");
      if (reader.delimiter == 13
          && reader.position < reader.bytes.length
          && reader.bytes[reader.position] == 10) reader.position++;
      long expected =
          bitmap
              ? (long) ((width + 7) / 8) * height
              : (long) width * height * (rgb ? 3 : 1) * (maximum > 255 ? 2 : 1);
      if (expected != reader.bytes.length - reader.position)
        throw new IOException("Datos Netpbm incompletos o adicionales");
    }
    Image image = new Image(width, height);
    int rowBytes = (width + 7) / 8, bitmapStart = reader.position;
    for (int i = 0; i < image.pixels.length; i++) {
      int pixel = 0xff000000;
      for (int channel = 0; channel < (rgb ? 3 : 1); channel++) {
        int sample;
        if (binary && bitmap) {
          int col = i % width, row = i / width;
          sample = (reader.bytes[bitmapStart + row * rowBytes + col / 8] >> (7 - col % 8)) & 1;
        } else if (binary) {
          sample = reader.bytes[reader.position++] & 255;
          if (maximum > 255) sample = sample << 8 | (reader.bytes[reader.position++] & 255);
        } else sample = reader.integer();
        if (sample > maximum) throw new IOException("Muestra Netpbm fuera de rango");
        int normalized = bitmap ? (1 - sample) * 255 : (sample * 255 + maximum / 2) / maximum;
        pixel |= rgb ? normalized << (16 - channel * 8) : normalized * 0x010101;
      }
      image.pixels[i] = pixel;
    }
    if (!binary) {
      reader.skip();
      if (reader.position != reader.bytes.length)
        throw new IOException("Muestras Netpbm adicionales");
    }
    return image;
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
      throw new IllegalArgumentException("Invalid PPM dimensions");
    out.write(("P6\n" + width + " " + height + "\n255\n").getBytes(StandardCharsets.US_ASCII));
    int[] pixels = new int[width];
    byte[] row = new byte[width * 3];
    for (int y = 0; y < height; y++) {
      source.read(y, pixels);
      int offset = 0;
      for (int pixel : pixels) {
        int alpha = pixel >>> 24;
        for (int shift = 16; shift >= 0; shift -= 8)
          row[offset++] =
              (byte) ((((pixel >>> shift) & 255) * alpha + 255 * (255 - alpha) + 127) / 255);
      }
      out.write(row);
    }
  }
}
