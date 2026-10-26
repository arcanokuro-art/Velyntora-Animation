package art.velyntora.core;

import android.graphics.*;
import android.view.MotionEvent;

/** Gesture controller for pencil. Shared document state belongs to DrawingView. */
final class PencilTool implements DrawingTool {
  public void down(DrawingView v, float x, float y, MotionEvent event) {
    v.paintStroke(x, y, x, y, event);
    v.requestStrokeRefresh();
  }

  public void move(DrawingView v, float x, float y, MotionEvent event) {
    if (x != v.previousX || y != v.previousY) {
      v.paintStroke(v.previousX, v.previousY, x, y, event);
      v.requestStrokeRefresh();
    }
  }

  public void up(DrawingView v, float x, float y, MotionEvent event) {
    if (!v.strokeEditing || x != v.previousX || y != v.previousY)
      v.paintStroke(v.previousX, v.previousY, x, y, event);
  }

  public void stroke(DrawingView v, float x0, float y0, float x1, float y1, float pressure) {
    DrawingView.nativeStyledStroke(
        (float) Math.floor(x0) + .5f,
        (float) Math.floor(y0) + .5f,
        (float) Math.floor(x1) + .5f,
        (float) Math.floor(y1) + .5f,
        v.brushRadius,
        v.color,
        v.brushOpacity,
        1,
        true,
        false);
  }
}
