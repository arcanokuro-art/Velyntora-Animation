package art.velyntora.core;

/** Five gradient geometries with premultiplied interpolation of two RGBA colors. */
final class GradientRaster {
  static int[] create(
      int width, int height, double x0, double y0, double x1, double y1, int color) {
    return create(width, height, x0, y0, x1, y1, color, false);
  }

  static int[] create(
      int width,
      int height,
      double x0,
      double y0,
      double x1,
      double y1,
      int color,
      boolean radial) {
    return create(width, height, x0, y0, x1, y1, color, 0, radial ? 1 : 0);
  }

  static int[] create(
      int width,
      int height,
      double x0,
      double y0,
      double x1,
      double y1,
      int first,
      int second,
      int mode) {
    if (width <= 0
        || height <= 0
        || (long) width * height > 4000000
        || mode < 0
        || mode > 4
        || !Double.isFinite(x0)
        || !Double.isFinite(y0)
        || !Double.isFinite(x1)
        || !Double.isFinite(y1)) throw new IllegalArgumentException("Invalid gradient");
    int[] data = new int[2 + width * height];
    data[0] = width;
    data[1] = height;
    double dx = x1 - x0, dy = y1 - y0, length = dx * dx + dy * dy;
    if (!Double.isFinite(length)) throw new IllegalArgumentException("Invalid gradient extent");
    if (length == 0) return data;
    double radius = Math.sqrt(length),
        direction = Math.atan2(dy, dx),
        a0 = first >>> 24,
        a1 = second >>> 24;
    for (int y = 0; y < height; y++)
      for (int x = 0; x < width; x++) {
        double qx = x + .5 - x0, qy = y + .5 - y0, t = (qx * dx + qy * dy) / length;
        if (mode == 1) t = Math.hypot(qx, qy) / radius;
        else if (mode == 2) t = Math.abs(t);
        else if (mode == 3)
          t = (Math.abs(qx * dx + qy * dy) + Math.abs(-qx * dy + qy * dx)) / length;
        else if (mode == 4) {
          double turn = (Math.atan2(qy, qx) - direction) / (2 * Math.PI);
          t = qx == 0 && qy == 0 ? 0 : turn - Math.floor(turn);
        }
        t = Math.max(0, Math.min(1, t));
        double alpha = a0 * (1 - t) + a1 * t;
        int a = (int) Math.round(alpha), pixel = a << 24;
        if (a > 0)
          for (int shift = 0; shift <= 16; shift += 8) {
            double channel =
                (((first >>> shift) & 255) * a0 * (1 - t) + ((second >>> shift) & 255) * a1 * t)
                    / alpha;
            pixel |= (int) Math.round(channel) << shift;
          }
        data[2 + y * width + x] = pixel;
      }
    return data;
  }
}
