package art.velyntora.core;

import art.velyntora.core.document.CanvasLimits;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;

/** Bounded text readers for X bitmap (X10/X11) and X pixmap (XPM3). */
final class XRasterReader {
  static final class Image {
    final int width, height;
    final int[] pixels;

    Image(int w, int h) throws IOException {
      if (w < 1 || h < 1 || w > CanvasLimits.MAX_SIDE || h > CanvasLimits.MAX_SIDE || (long) w * h > CanvasLimits.MAX_PIXELS)
        throw new IOException("Imagen X demasiado grande");
      width = w;
      height = h;
      pixels = new int[w * h];
    }
  }

  private static final Map<String, Integer> names = new HashMap<>();

  static {
    try (InputStream stream = XRasterReader.class.getResourceAsStream("/x11-rgb.txt")) {
      if (stream != null) {
        BufferedReader reader =
            new BufferedReader(new InputStreamReader(stream, StandardCharsets.US_ASCII));
        String line;
        Pattern p = Pattern.compile("\\s*(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(.+)");
        while ((line = reader.readLine()) != null) {
          Matcher m = p.matcher(line);
          if (m.matches())
            names.put(
                normalize(m.group(4)),
                0xff000000
                    | Integer.parseInt(m.group(1)) << 16
                    | Integer.parseInt(m.group(2)) << 8
                    | Integer.parseInt(m.group(3)));
        }
      }
    } catch (IOException ignored) {
    }
  }

  private static String normalize(String name) {
    return name.replace(" ", "").toLowerCase(Locale.ROOT);
  }

  private static String text(InputStream input) throws IOException {
    return new String(OpenRasterArchive.bounded(input, 4194304), StandardCharsets.ISO_8859_1);
  }

  static Image xbm(InputStream input) throws IOException {
    String s = text(input);
    Matcher width = Pattern.compile("#define\\s+(\\w+)_width\\s+(\\d+)").matcher(s);
    if (!width.find()) throw new IOException("XBM sin ancho");
    String name = width.group(1);
    int w = number(width.group(2));
    Matcher height =
        Pattern.compile("#define\\s+" + Pattern.quote(name) + "_height\\s+(\\d+)").matcher(s);
    if (!height.find()) throw new IOException("XBM sin alto");
    Image out = new Image(w, number(height.group(1)));
    Matcher data =
        Pattern.compile(
                "(?:unsigned\\s+)?(char|short)\\s+"
                    + Pattern.quote(name)
                    + "_bits\\s*\\[\\s*\\]\\s*=\\s*\\{([^}]+)\\}",
                Pattern.DOTALL)
            .matcher(s);
    if (!data.find()) throw new IOException("XBM sin datos");
    boolean shortWords = data.group(1).equals("short");
    int unit = shortWords ? 16 : 8, stride = (w + unit - 1) / unit;
    String body = data.group(2).replaceAll("/\\*.*?\\*/", "");
    String[] values = body.trim().split("\\s*,\\s*");
    if (values.length != stride * out.height) throw new IOException("Datos XBM incompletos");
    for (int y = 0; y < out.height; y++)
      for (int column = 0; column < stride; column++) {
        String value = values[y * stride + column].trim();
        int word;
        try {
          word = Integer.decode(value);
        } catch (NumberFormatException e) {
          throw new IOException("Muestra XBM inválida");
        }
        if (word < 0 || word >= (1 << unit)) throw new IOException("Muestra XBM fuera de rango");
        for (int bit = 0; bit < unit && column * unit + bit < w; bit++)
          out.pixels[y * w + column * unit + bit] =
              (word & (1 << bit)) != 0 ? 0xff000000 : 0xffffffff;
      }
    return out;
  }

  static Image xpm(InputStream input) throws IOException {
    String s = text(input);
    List<String> strings = new ArrayList<>();
    Matcher quoted = Pattern.compile("\"((?:\\\\.|[^\"\\\\])*)\"", Pattern.DOTALL).matcher(s);
    while (quoted.find()) {
      String raw = quoted.group(1);
      StringBuilder value = new StringBuilder();
      for (int i = 0; i < raw.length(); i++) {
        char c = raw.charAt(i);
        if (c == '\\') {
          if (++i >= raw.length()) throw new IOException("Escape XPM inválido");
          c = raw.charAt(i);
          if (c != '\\' && c != '\"') throw new IOException("Escape XPM incompatible");
        }
        value.append(c);
      }
      strings.add(value.toString());
    }
    int start = 0;
    while (start < strings.size()
        && !strings.get(start).matches("\\d+\\s+\\d+\\s+\\d+\\s+\\d+(?:\\s+.*)?")) start++;
    if (start == strings.size()) throw new IOException("XPM sin cabecera");
    String[] header = strings.get(start++).split("\\s+");
    int colors = number(header[2]), cpp = number(header[3]);
    if (colors < 1 || colors > 65536 || cpp < 1 || cpp > 8)
      throw new IOException("Paleta XPM demasiado grande");
    Image out = new Image(number(header[0]), number(header[1]));
    if (strings.size() - start < colors + out.height) throw new IOException("XPM incompleto");
    Map<String, Integer> palette = new HashMap<>();
    for (int i = 0; i < colors; i++) {
      String line = strings.get(start++);
      if (line.length() < cpp) throw new IOException("Clave XPM incompleta");
      String key = line.substring(0, cpp), spec = line.substring(cpp);
      Matcher properties =
          Pattern.compile("(?:^|\\s)(c|g|g4|m)\\s+(.+?)(?=\\s+(?:c|g|g4|m|s)\\s+|$)").matcher(spec);
      String color = null;
      while (properties.find()) {
        if (color == null || properties.group(1).equals("c")) color = properties.group(2).trim();
        if (properties.group(1).equals("c")) break;
      }
      if (color == null || palette.put(key, color(color)) != null)
        throw new IOException("Paleta XPM inválida");
    }
    for (int y = 0; y < out.height; y++) {
      String row = strings.get(start++);
      if (row.length() != out.width * cpp) throw new IOException("Fila XPM inválida");
      for (int x = 0; x < out.width; x++) {
        Integer value = palette.get(row.substring(x * cpp, (x + 1) * cpp));
        if (value == null) throw new IOException("Índice XPM inválido");
        out.pixels[y * out.width + x] = value;
      }
    }
    return out;
  }

  private static int number(String s) throws IOException {
    try {
      return Integer.parseInt(s);
    } catch (NumberFormatException e) {
      throw new IOException("Dimensión X inválida");
    }
  }

  private static int color(String s) throws IOException {
    if (s.equalsIgnoreCase("None")) return 0;
    if (s.startsWith("#")) {
      int length = s.length() - 1;
      if (length != 3 && length != 6 && length != 9 && length != 12)
        throw new IOException("Color XPM inválido");
      int digits = length / 3, result = 0xff000000, max = (1 << (digits * 4)) - 1;
      for (int i = 0; i < 3; i++) {
        int channel;
        try {
          channel = Integer.parseInt(s.substring(1 + i * digits, 1 + (i + 1) * digits), 16);
        } catch (NumberFormatException e) {
          throw new IOException("Color XPM inválido");
        }
        result |= ((channel * 255 + max / 2) / max) << (16 - 8 * i);
      }
      return result;
    }
    Integer result = names.get(normalize(s));
    if (result == null) throw new IOException("Nombre de color XPM desconocido");
    return result;
  }
}
