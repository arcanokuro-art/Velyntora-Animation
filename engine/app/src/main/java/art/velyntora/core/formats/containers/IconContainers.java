package art.velyntora.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Static-image import from ANI, ICNS and QuickTime image containers. */
final class IconContainers {
  static final class Image {
    final int width, height;
    final int[] pixels;

    Image(int w, int h, int[] p) {
      width = w;
      height = h;
      pixels = p;
    }
  }

  interface Decoder {
    Image decode(byte[] encoded) throws IOException;
  }

  private static long number(byte[] b, int p, boolean little) throws IOException {
    if (p < 0 || p > b.length - 4) throw new IOException("Contenedor incompleto");
    long n = 0;
    for (int i = 0; i < 4; i++) n |= (long) (b[p + i] & 255) << (little ? 8 * i : 8 * (3 - i));
    return n;
  }

  private static String tag(byte[] b, int p) throws IOException {
    if (p < 0 || p > b.length - 4) throw new IOException("Etiqueta incompleta");
    return new String(b, p, 4, StandardCharsets.US_ASCII);
  }

  static Image read(InputStream input, Decoder decoder) throws IOException {
    byte[] b = OpenRasterArchive.bounded(input, 67108864);
    String magic = tag(b, 0);
    if (magic.equals("icns")) return icns(b, decoder);
    if (magic.equals("RIFF")) {
      if (!tag(b, 8).equals("ACON") || number(b, 4, true) + 8 > b.length)
        throw new IOException("ANI inválido");
      List<byte[]> frames = new ArrayList<>();
      List<Integer> sequence = new ArrayList<>();
      chunks(b, 12, (int) number(b, 4, true) + 8, 0, frames, sequence);
      int first = sequence.isEmpty() ? 0 : sequence.get(0);
      if (first < 0 || first >= frames.size()) throw new IOException("ANI sin imagen");
      return decoder.decode(frames.get(first));
    }
    int pos = 0;
    for (int atoms = 0; pos < b.length && atoms < 256; atoms++) {
      long length = number(b, pos, false);
      String type = tag(b, pos + 4);
      if (length < 8 || length > b.length - pos) throw new IOException("Átomo QuickTime inválido");
      if (type.equals("idat"))
        return decoder.decode(Arrays.copyOfRange(b, pos + 8, pos + (int) length));
      pos += (int) length;
    }
    throw new IOException("Contenedor sin imagen compatible");
  }

  private static void chunks(
      byte[] b, int pos, int end, int depth, List<byte[]> frames, List<Integer> sequence)
      throws IOException {
    if (depth > 4) throw new IOException("ANI demasiado anidado");
    int count = 0;
    while (pos < end) {
      if (++count > 256 || end - pos < 8)
        throw new IOException("ANI demasiado complejo o incompleto");
      long length = number(b, pos + 4, true);
      if (length > end - pos - 8) throw new IOException("Bloque ANI fuera de límites");
      String type = tag(b, pos);
      int start = pos + 8, stop = start + (int) length;
      if (type.equals("LIST")) {
        if (length < 4) throw new IOException("Lista ANI incompleta");
        chunks(b, start + 4, stop, depth + 1, frames, sequence);
      } else if (type.equals("icon")) {
        if (frames.size() >= 256) throw new IOException("Demasiadas imágenes ANI");
        frames.add(Arrays.copyOfRange(b, start, stop));
      } else if (type.equals("seq ")) {
        if (length % 4 != 0 || length > 4096) throw new IOException("Secuencia ANI inválida");
        for (int i = start; i < stop; i += 4) sequence.add((int) number(b, i, true));
      }
      pos = stop + (int) (length & 1);
      if (pos > end) throw new IOException("Relleno ANI incompleto");
    }
  }

  private static Image icns(byte[] b, Decoder decoder) throws IOException {
    long total = number(b, 4, false);
    if (total != b.length) throw new IOException("Tamaño ICNS inválido");
    Map<String, byte[]> blocks = new HashMap<>();
    for (int p = 8, count = 0; p < b.length; count++) {
      if (count > 256) throw new IOException("Demasiados bloques ICNS");
      String id = tag(b, p);
      long length = number(b, p + 4, false);
      if (length < 8 || length > b.length - p) throw new IOException("Bloque ICNS inválido");
      blocks.put(id, Arrays.copyOfRange(b, p + 8, p + (int) length));
      p += (int) length;
    }
    Image best = null;
    for (String id :
        new String[] {
          "icp4", "icp5", "icp6", "ic07", "ic08", "ic09", "ic10", "ic11", "ic12", "ic13", "ic14"
        }) {
      byte[] encoded = blocks.get(id);
      if (encoded == null) continue;
      try {
        Image candidate = decoder.decode(encoded);
        if (best == null
            || (long) candidate.width * candidate.height > (long) best.width * best.height)
          best = candidate;
      } catch (IOException ignored) {
      }
    }
    if (best != null) return best;
    String[] ids = {"it32", "ih32", "il32", "is32"}, masks = {"t8mk", "h8mk", "l8mk", "s8mk"};
    int[] sizes = {128, 48, 32, 16};
    for (int i = 0; i < ids.length; i++) {
      byte[] rgb = blocks.get(ids[i]), alpha = blocks.get(masks[i]);
      int size = sizes[i], n = size * size;
      if (rgb == null || alpha == null || alpha.length != n) continue;
      int[] pixels = new int[n];
      for (int p = 0; p < n; p++) pixels[p] = (alpha[p] & 255) << 24;
      if (rgb.length == n * 4) {
        for (int p = 0; p < n; p++)
          pixels[p] |=
              (rgb[p * 4 + 1] & 255) << 16 | (rgb[p * 4 + 2] & 255) << 8 | (rgb[p * 4 + 3] & 255);
      } else {
        int pos =
            ids[i].equals("it32")
                    && rgb.length >= 4
                    && rgb[0] == 0
                    && rgb[1] == 0
                    && rgb[2] == 0
                    && rgb[3] == 0
                ? 4
                : 0;
        for (int channel = 0; channel < 3; channel++) {
          int dest = 0;
          while (dest < n) {
            if (pos >= rgb.length) throw new IOException("ICNS RLE incompleto");
            int control = rgb[pos++] & 255, count = control >= 128 ? control - 125 : control + 1;
            if (count > n - dest) throw new IOException("ICNS RLE fuera de límites");
            if (control >= 128) {
              if (pos >= rgb.length) throw new IOException("ICNS RLE incompleto");
              int value = rgb[pos++] & 255;
              for (int j = 0; j < count; j++) pixels[dest++] |= value << (16 - channel * 8);
            } else {
              if (count > rgb.length - pos) throw new IOException("ICNS RLE incompleto");
              for (int j = 0; j < count; j++)
                pixels[dest++] |= (rgb[pos++] & 255) << (16 - channel * 8);
            }
          }
        }
        if (pos != rgb.length) throw new IOException("ICNS RLE con datos sobrantes");
      }
      return new Image(size, size, pixels);
    }
    throw new IOException("ICNS sin imagen compatible; JPEG 2000 no admitido");
  }
}
