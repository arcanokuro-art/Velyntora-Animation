package art.velyntora.core;

import android.graphics.*;
import android.view.MotionEvent;

/** Gesture controller for clone. Shared document state belongs to DrawingView. */
final class CloneTool implements DrawingTool {
  public void down(DrawingView v, float x, float y, MotionEvent event) {
    if (!v.cloneOriginReady) {
      v.cloneX = (int) x;
      v.cloneY = (int) y;
      v.cloneOriginReady = true;
      v.drawing = false;
      v.announceForAccessibility("Origen de clonación fijado");
    } else {
      v.paintStroke(x, y, x, y, event);
      v.requestStrokeRefresh();
    }
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
    DrawingView.nativeSampledStroke(
        true,
        v.cloneX - (int) v.startX,
        v.cloneY - (int) v.startY,
        v.color,
        v.wandTolerance,
        x0,
        y0,
        x1,
        y1,
        v.brushRadius * pressure,
        v.brushOpacity,
        v.brushHardness);
  }
}
