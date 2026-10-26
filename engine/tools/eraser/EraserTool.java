package art.velyntora.core;

import android.graphics.*;
import android.view.MotionEvent;

/** Gesture controller for eraser. Shared document state belongs to DrawingView. */
final class EraserTool implements DrawingTool {
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
        x0,
        y0,
        x1,
        y1,
        v.brushRadius * pressure,
        v.color,
        v.brushOpacity,
        v.brushHardness,
        v.squareBrush,
        true);
  }
}
