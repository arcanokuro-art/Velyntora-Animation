package art.velyntora.core;

import android.graphics.*;
import android.view.MotionEvent;

/** Gesture controller for ellipse_selection. Shared document state belongs to DrawingView. */
final class EllipseSelectionTool implements DrawingTool {
  public void down(DrawingView v, float x, float y, MotionEvent event) {
    v.selectionTool = v.tool;
    v.hasSelection = true;
    v.selectionLeft = v.selectionRight = x;
    v.selectionTop = v.selectionBottom = y;
    v.invalidate();
  }

  public void move(DrawingView v, float x, float y, MotionEvent event) {
    v.updateSelection(x, y);
  }

  public void up(DrawingView v, float x, float y, MotionEvent event) {
    v.updateSelection(x, y);
    v.hasSelection = v.hasSelection();
  }

  public void cancel(DrawingView v) {
    if (v.drawing) v.deselect();
  }
}
