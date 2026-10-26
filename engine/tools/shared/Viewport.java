package art.velyntora.core;

/** Invertible document-to-screen transform, independent of Android. */
final class Viewport {
  static final double MIN_SCALE = 0.01, MAX_SCALE = 70;
  double scale = 1, angle = 0, centerX, centerY;
  private double width, height;

  Viewport(double width, double height) {
    this.width = width;
    this.height = height;
  }

  void documentSize(double w, double h) {
    width = w;
    height = h;
  }

  void fit(double w, double h) {
    if (w <= 0 || h <= 0) return;
    scale = clamp(Math.min(w / width, h / height));
    angle = 0;
    centerX = w / 2;
    centerY = h / 2;
  }

  double documentX(double x, double y) {
    double r = Math.toRadians(angle);
    return width / 2 + ((x - centerX) * Math.cos(r) + (y - centerY) * Math.sin(r)) / scale;
  }

  double documentY(double x, double y) {
    double r = Math.toRadians(angle);
    return height / 2 + (-(x - centerX) * Math.sin(r) + (y - centerY) * Math.cos(r)) / scale;
  }

  double screenX(double x, double y) {
    double r = Math.toRadians(angle);
    return centerX + scale * ((x - width / 2) * Math.cos(r) - (y - height / 2) * Math.sin(r));
  }

  double screenY(double x, double y) {
    double r = Math.toRadians(angle);
    return centerY + scale * ((x - width / 2) * Math.sin(r) + (y - height / 2) * Math.cos(r));
  }

  void gesture(double oldX, double oldY, double newX, double newY, double factor, double degrees) {
    if (!Double.isFinite(factor) || factor <= 0 || !Double.isFinite(degrees)) return;
    double x = documentX(oldX, oldY), y = documentY(oldX, oldY);
    scale = clamp(scale * factor);
    angle = ((angle + degrees + 180) % 360 + 360) % 360 - 180;
    centerX += newX - screenX(x, y);
    centerY += newY - screenY(x, y);
  }

  void zoom(double value, double x, double y) {
    gesture(x, y, x, y, value / scale, 0);
  }

  void rotate(double degrees, double x, double y) {
    gesture(x, y, x, y, 1, degrees);
  }

  private static double clamp(double value) {
    return Math.max(MIN_SCALE, Math.min(MAX_SCALE, value));
  }
}
