package art.velyntora.core;

import android.graphics.*;
import android.view.MotionEvent;

/** Gesture controller for bucket. Shared document state belongs to DrawingView. */
final class BucketTool implements DrawingTool {
  public void down(DrawingView v, float x, float y, MotionEvent event) {
    if (v.brushOpacity <= 0) { v.drawing = false; return; }
    DrawingView.nativeFillSelection((int) x, (int) y, v.color, v.brushOpacity, v.selectionMask());
    v.drawing = false;
    v.refresh();
  }
}
