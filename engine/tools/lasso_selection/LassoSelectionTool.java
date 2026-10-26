package art.velyntora.core;

import android.graphics.*;
import android.view.MotionEvent;

/** Gesture controller for lasso_selection. Shared document state belongs to DrawingView. */
final class LassoSelectionTool implements DrawingTool {
  public void down(DrawingView v, float x, float y, MotionEvent event) {
    v.selectionTool = DrawingView.SELECT_FREE;
    v.hasSelection = false;
    v.freeSelectionReady = false;
    v.freeRegion.setEmpty();
    v.freePath.reset();
    v.freePath.moveTo(x, y);
    v.selectionLeft = v.selectionRight = x;
    v.selectionTop = v.selectionBottom = y;
    v.invalidate();
  }

  public void move(DrawingView v, float x, float y, MotionEvent event) {
    float px = Math.max(0f, Math.min(v.canvasWidth, x)),
        py = Math.max(0f, Math.min(v.canvasHeight, y));
    v.freePath.lineTo(px, py);
    v.selectionLeft = Math.min(v.selectionLeft, px);
    v.selectionRight = Math.max(v.selectionRight, px);
    v.selectionTop = Math.min(v.selectionTop, py);
    v.selectionBottom = Math.max(v.selectionBottom, py);
    v.invalidate();
  }

  public void up(DrawingView v, float x, float y, MotionEvent event) {
    float px = Math.max(0f, Math.min(v.canvasWidth, x)),
        py = Math.max(0f, Math.min(v.canvasHeight, y));
    v.freePath.lineTo(px, py);
    v.freePath.close();
    v.selectionLeft = Math.min(v.selectionLeft, px);
    v.selectionRight = Math.max(v.selectionRight, px);
    v.selectionTop = Math.min(v.selectionTop, py);
    v.selectionBottom = Math.max(v.selectionBottom, py);
    v.hasSelection =
        (v.selectionRight - v.selectionLeft >= 1f && v.selectionBottom - v.selectionTop >= 1f);
    if (v.hasSelection) {
      v.freeSelectionReady =
          v.freeRegion.setPath(
              v.freePath, new android.graphics.Region(0, 0, v.canvasWidth, v.canvasHeight));
      v.hasSelection = v.freeSelectionReady && !v.freeRegion.isEmpty();
      if (v.hasSelection) {
        android.graphics.Rect actual = v.freeRegion.getBounds();
        v.selectionLeft = actual.left;
        v.selectionTop = actual.top;
        v.selectionRight = actual.right;
        v.selectionBottom = actual.bottom;
      }
    }
    v.freeSelectionReady = v.hasSelection;
    v.invalidate();
  }

  public void cancel(DrawingView v) {
    if (v.drawing) v.deselect();
  }
}
