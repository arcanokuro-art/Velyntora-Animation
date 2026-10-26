package art.velyntora.core;

import java.util.Locale;

/** Signature dispatch; only TGA needs the name because it has no reliable magic. */
final class RasterFormat {
  static final int ANDROID = 0,
      BMP = 1,
      TIFF = 2,
      ICO = 3,
      NETPBM = 4,
      TGA = 5,
      PROJECT = 6,
      ORA = 7,
      PCX = 8,
      XBM = 9,
      XPM = 10,
      SVG = 11,
      CONTAINER = 12;

  static int detect(byte[] b, String name) {
    if (b.length >= 8
        && b[0] == 'V'
        && b[1] == 'L'
        && b[2] == 'Y'
        && b[3] == 'C'
        && b[4] == 'O'
        && b[5] == 'R'
        && b[6] == 'E') return PROJECT;
    if (b.length >= 2 && b[0] == 'B' && b[1] == 'M') return BMP;
    if (b.length >= 4
        && ((b[0] == 'I' && b[1] == 'I' && b[2] == 42 && b[3] == 0)
            || (b[0] == 'M' && b[1] == 'M' && b[2] == 0 && b[3] == 42))) return TIFF;
    if (b.length >= 4 && b[0] == 0 && b[1] == 0 && b[2] == 1 && b[3] == 0) return ICO;
    if (b.length >= 3
        && b[0] == 'P'
        && b[1] >= '1'
        && b[1] <= '6'
        && (b[2] == ' ' || b[2] == '\t' || b[2] == '\r' || b[2] == '\n' || b[2] == '#'))
      return NETPBM;
    if (b.length >= 4 && b[0] == 'P' && b[1] == 'K' && b[2] == 3 && b[3] == 4) return ORA;
    if (b.length >= 4 && b[0] == 'i' && b[1] == 'c' && b[2] == 'n' && b[3] == 's') return CONTAINER;
    if (b.length >= 12
        && b[0] == 'R'
        && b[1] == 'I'
        && b[2] == 'F'
        && b[3] == 'F'
        && b[8] == 'A'
        && b[9] == 'C'
        && b[10] == 'O'
        && b[11] == 'N') return CONTAINER;
    if (b.length >= 8
        && b[4] == 'i'
        && b[5] == 'd'
        && (b[6] == 'a' && b[7] == 't' || b[6] == 's' && b[7] == 'c')) return CONTAINER;
    if (b.length >= 4 && b[0] == 10 && b[2] == 1 && (b[3] == 1 || b[3] == 8)) return PCX;
    // Recognized native Android formats win over an incorrect TGA extension.
    if (b.length >= 3 && ((b[0] & 255) == 255 && (b[1] & 255) == 216 && (b[2] & 255) == 255)
        || b.length >= 4
            && ((b[0] & 255) == 137 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                || b[0] == 'G' && b[1] == 'I' && b[2] == 'F'
                || b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F')) return ANDROID;
    String extension = name == null ? "" : name.toLowerCase(Locale.ROOT);
    if (extension.endsWith(".tga")) return TGA;
    if (extension.endsWith(".xbm")) return XBM;
    if (extension.endsWith(".xpm")) return XPM;
    if (extension.endsWith(".svg")) return SVG;
    String prefix = new String(b, java.nio.charset.StandardCharsets.US_ASCII).trim();
    if (prefix.startsWith("#define")) return XBM;
    if (prefix.startsWith("/* XPM */")) return XPM;
    if (prefix.startsWith("<svg") || prefix.startsWith("<?xml")) return SVG;
    return ANDROID;
  }
}
