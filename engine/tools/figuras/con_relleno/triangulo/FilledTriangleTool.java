package art.velyntora.core;

import android.graphics.*;
import android.view.MotionEvent;

/** Gesture controller for filled_triangle. Shared document state belongs to DrawingView. */
final class FilledTriangleTool extends ShapeTool {
  public void up(DrawingView v, float x, float y, MotionEvent event) {
    v.additionalShape(v.tool, v.startX, v.startY, x, y);
  }
}
