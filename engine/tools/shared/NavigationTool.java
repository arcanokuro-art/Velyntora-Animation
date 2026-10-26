package art.velyntora.core;

/** One-pointer pan or vertical-drag zoom; taps zoom in and long taps zoom out. */
final class NavigationTool {
  private double startX, startY, lastX, lastY;
  private boolean dragged;
  private final double slop;

  NavigationTool(double tolerance) {
    slop = tolerance;
  }

  void begin(double x, double y) {
    startX = lastX = x;
    startY = lastY = y;
    dragged = false;
  }

  void move(Viewport viewport, double x, double y, boolean zoom) {
    if (!dragged && Math.hypot(x - startX, y - startY) > slop) dragged = true;
    if (dragged) {
      if (zoom) viewport.gesture(startX, startY, startX, startY, Math.pow(1.01, lastY - y), 0);
      else viewport.gesture(lastX, lastY, x, y, 1, 0);
    }
    lastX = x;
    lastY = y;
  }

  void finish(Viewport viewport, boolean zoom, boolean zoomOut) {
    if (zoom && !dragged) viewport.gesture(startX, startY, startX, startY, zoomOut ? .8 : 1.25, 0);
  }
}
