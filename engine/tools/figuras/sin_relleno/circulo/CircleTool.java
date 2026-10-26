package art.velyntora.core;

import android.graphics.*;
import android.view.MotionEvent;

/** Gesture controller for circle. Shared document state belongs to DrawingView. */
final class CircleTool extends ShapeTool {
  public void up(DrawingView v, float x, float y, MotionEvent event) {
    float size = Math.min(Math.abs(x - v.startX), Math.abs(y - v.startY));
    v.additionalShape(
        DrawingView.ELLIPSE,
        v.startX,
        v.startY,
        v.startX + Math.copySign(size, x - v.startX),
        v.startY + Math.copySign(size, y - v.startY));
  }
}
