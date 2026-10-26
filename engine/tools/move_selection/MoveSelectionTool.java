package art.velyntora.core;

import android.graphics.*;
import android.view.MotionEvent;

/** Gesture controller for move_selection. Shared document state belongs to DrawingView. */
final class MoveSelectionTool implements DrawingTool {
  public void down(DrawingView v, float x, float y, MotionEvent event) {
    v.movingPixels = v.tool == DrawingView.MOVE_PIXELS;
    v.movingSelection = v.hasSelection() && v.selectionContains((int) x, (int) y);
    if (v.movingSelection) {
      v.moveStartX = x;
      v.moveStartY = y;
      v.moveOriginalLeft = v.selectionLeft;
      v.moveOriginalTop = v.selectionTop;
      v.moveOriginalRight = v.selectionRight;
      v.moveOriginalBottom = v.selectionBottom;
    } else v.drawing = false;
  }

  public void move(DrawingView v, float x, float y, MotionEvent event) {
    if (v.movingSelection) v.updateMovedSelection(x, y);
  }

  public void up(DrawingView v, float x, float y, MotionEvent event) {
    if (v.movingSelection) {
      if (v.movingPixels) {
        final int dx = Math.round(x - v.moveStartX), dy = Math.round(y - v.moveStartY);
        v.selectionLeft = v.moveOriginalLeft;
        v.selectionTop = v.moveOriginalTop;
        v.selectionRight = v.moveOriginalRight;
        v.selectionBottom = v.moveOriginalBottom;
        if (!v.moveSelectedPixels(dx, dy)) v.invalidate();
      } else v.updateMovedSelection(x, y);
    }
    v.movingSelection = false;
    v.movingPixels = false;
  }

  static void updateMovedSelection(DrawingView v, float x, float y) {
    float dx = Math.round(x - v.moveStartX), dy = Math.round(y - v.moveStartY);
    dx = Math.max(-v.moveOriginalLeft, Math.min(v.canvasWidth - v.moveOriginalRight, dx));
    dy = Math.max(-v.moveOriginalTop, Math.min(v.canvasHeight - v.moveOriginalBottom, dy));
    if (!v.movingPixels)
      v.translateSelectionMask(
          Math.round(v.moveOriginalLeft + dx - v.selectionLeft),
          Math.round(v.moveOriginalTop + dy - v.selectionTop));
    v.selectionLeft = v.moveOriginalLeft + dx;
    v.selectionRight = v.moveOriginalRight + dx;
    v.selectionTop = v.moveOriginalTop + dy;
    v.selectionBottom = v.moveOriginalBottom + dy;
    v.invalidate();
  }

  static void restoreMovedSelection(DrawingView v) {
    if (!v.movingPixels)
      v.translateSelectionMask(
          Math.round(v.moveOriginalLeft - v.selectionLeft),
          Math.round(v.moveOriginalTop - v.selectionTop));
    v.selectionLeft = v.moveOriginalLeft;
    v.selectionTop = v.moveOriginalTop;
    v.selectionRight = v.moveOriginalRight;
    v.selectionBottom = v.moveOriginalBottom;
  }

  static void translateSelectionMask(DrawingView v, int dx, int dy) {
    if (v.selectionTool != DrawingView.SELECT_FREE && v.selectionTool != DrawingView.MAGIC_WAND)
      return;
    android.graphics.Matrix translation = new android.graphics.Matrix();
    translation.setTranslate(dx, dy);
    v.freePath.transform(translation);
    v.freeRegion.translate(dx, dy);
    v.wandBoundary.transform(translation);
  }

  public void cancel(DrawingView v) {
    if (v.movingSelection) MoveSelectionTool.restoreMovedSelection(v);
  }
}
