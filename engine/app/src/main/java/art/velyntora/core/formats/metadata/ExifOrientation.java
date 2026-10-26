package art.velyntora.core;

import java.io.IOException;
import java.io.InputStream;

/** Reads the JPEG IFD0 orientation without allocating the photo or trusting EXIF offsets. */
final class ExifOrientation {
  static int read(InputStream input) throws IOException {
    if (input.read() != 255 || input.read() != 216) return 1;
    int budget = 1024 * 1024;
    while (budget > 0) {
      int marker = input.read();
      if (marker != 255) return 1;
      do {
        marker = input.read();
        budget--;
      } while (marker == 255 && budget > 0);
      if (marker < 0 || marker == 217 || marker == 218) return 1;
      if (marker == 0 || marker == 216 || marker >= 208 && marker <= 215) continue;
      int hi = input.read(), lo = input.read();
      if (hi < 0 || lo < 0) return 1;
      int size = (hi << 8 | lo) - 2;
      if (size < 0 || size > budget) return 1;
      budget -= size + 3;
      byte[] segment = new byte[size];
      int position = 0;
      while (position < size) {
        int n = input.read(segment, position, size - position);
        if (n < 0) return 1;
        if (n == 0) {
          int b = input.read();
          if (b < 0) return 1;
          segment[position++] = (byte) b;
        } else position += n;
      }
      if (marker == 225
          && size >= 14
          && segment[0] == 'E'
          && segment[1] == 'x'
          && segment[2] == 'i'
          && segment[3] == 'f'
          && segment[4] == 0
          && segment[5] == 0) {
        int value = tiff(segment);
        if (value != 1) return value;
      }
    }
    return 1;
  }

  private static int tiff(byte[] b) {
    boolean le = b[6] == 'I' && b[7] == 'I';
    if (!le && !(b[6] == 'M' && b[7] == 'M')) return 1;
    if (u16(b, 8, le) != 42) return 1;
    long offset = u32(b, 10, le) + 6;
    if (offset < 14 || offset > b.length - 2) return 1;
    int p = (int) offset;
    int count = u16(b, p, le);
    p += 2;
    if (count > (b.length - p) / 12) return 1;
    for (int i = 0; i < count; i++, p += 12)
      if (u16(b, p, le) == 274) {
        if (u16(b, p + 2, le) != 3 || u32(b, p + 4, le) != 1) return 1;
        int v = u16(b, p + 8, le);
        return v >= 1 && v <= 8 ? v : 1;
      }
    return 1;
  }

  private static int u16(byte[] b, int p, boolean le) {
    return le ? (b[p] & 255) | ((b[p + 1] & 255) << 8) : ((b[p] & 255) << 8) | (b[p + 1] & 255);
  }

  private static long u32(byte[] b, int p, boolean le) {
    return le
        ? (long) u16(b, p, le) | (long) u16(b, p + 2, le) << 16
        : (long) u16(b, p, le) << 16 | u16(b, p + 2, le);
  }

  /** Source coordinate for each output pixel; mirrored orientations are included. */
  static int sourceIndex(int x, int y, int width, int height, int orientation) {
    int sx = x, sy = y;
    switch (orientation) {
      case 2:
        sx = width - 1 - x;
        break;
      case 3:
        sx = width - 1 - x;
        sy = height - 1 - y;
        break;
      case 4:
        sy = height - 1 - y;
        break;
      case 5:
        sx = y;
        sy = x;
        break;
      case 6:
        sx = y;
        sy = height - 1 - x;
        break;
      case 7:
        sx = width - 1 - y;
        sy = height - 1 - x;
        break;
      case 8:
        sx = width - 1 - y;
        sy = x;
        break;
      default:
        break;
    }
    return sy * width + sx;
  }
}
