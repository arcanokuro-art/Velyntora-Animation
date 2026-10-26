package art.velyntora.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;


/** Owns components/canvas behavior; the host is the workspace composition facade. */
final class CanvasViewport {
  private final DrawingView host;
  CanvasViewport(DrawingView host) { this.host = host; }

  String cursorStatus() {
    return host.readCursorX() < 0 ? "" : "  |  X: " + host.readCursorX() + " Y: " + host.readCursorY();
  }

  float zoomPercent() {
    return (float) (host.readViewport().scale * 100);
  }

  float rotationDegrees() {
    return (float) host.readViewport().angle;
  }

  void viewportChanged() {
    host.invalidate();
    if (host.readViewportChangedListener() != null) host.readViewportChangedListener().run();
  }

  void fitCanvas() {
    host.readViewport().fit(host.getWidth(), host.getHeight());
    host.viewportChanged();
  }

  void zoomBy(float factor) {
    host.readViewport().zoom(host.readViewport().scale * factor, host.getWidth() / 2., host.getHeight() / 2.);
    host.viewportChanged();
  }

  void setZoomPercent(float percent) {
    host.readViewport().zoom(percent / 100., host.getWidth() / 2., host.getHeight() / 2.);
    host.viewportChanged();
  }

  void rotateView(float degrees) {
    host.readViewport().rotate(degrees, host.getWidth() / 2., host.getHeight() / 2.);
    host.viewportChanged();
  }
}
