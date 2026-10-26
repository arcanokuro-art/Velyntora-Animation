package art.velyntora.core;

import android.graphics.*;
import android.view.MotionEvent;

/** Gesture controller for picker. Shared document state belongs to DrawingView. */
final class PickerTool implements DrawingTool {
  public void down(DrawingView v, float x, float y, MotionEvent event) {
    v.color = DrawingView.nativePickColor((int) x, (int) y);
    v.drawing = false;
    if (v.pickedColorListener != null) v.pickedColorListener.accept(v.color);
  }
}
