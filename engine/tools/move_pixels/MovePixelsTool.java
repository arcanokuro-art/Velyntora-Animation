package art.velyntora.core;

import android.graphics.*;
import android.view.MotionEvent;

/** Gesture controller for move_pixels. Shared document state belongs to DrawingView. */
final class MovePixelsTool implements DrawingTool {
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

  static boolean transformSelection(DrawingView v, float degrees, float sx, float sy) {
    if (!v.hasSelection()) return false;
    int left = (int) Math.floor(v.selectionLeft),
        top = (int) Math.floor(v.selectionTop),
        right = (int) Math.ceil(v.selectionRight),
        bottom = (int) Math.ceil(v.selectionBottom);
    int w = right - left, h = bottom - top;
    if (w <= 0 || h <= 0 || left < 0 || top < 0 || right > v.canvasWidth || bottom > v.canvasHeight)
      return false;
    byte[] mask = new byte[w * h];
    android.graphics.Region selected = new android.graphics.Region();
    for (int y = 0; y < h; ++y) {
      int run = -1;
      for (int x = 0; x <= w; ++x) {
        boolean inside = x < w && v.selectionContains(left + x, top + y);
        if (inside) {
          mask[y * w + x] = 1;
          if (run < 0) run = x;
        } else if (run >= 0) {
          selected.op(left + run, top + y, left + x, top + y + 1, android.graphics.Region.Op.UNION);
          run = -1;
        }
      }
    }
    android.graphics.Path transformed = selected.getBoundaryPath();
    float cx = left + w / 2f, cy = top + h / 2f;
    android.graphics.Matrix matrix = new android.graphics.Matrix();
    matrix.setTranslate(-cx, -cy);
    matrix.postScale(sx, sy);
    matrix.postRotate(degrees);
    matrix.postTranslate(cx, cy);
    transformed.transform(matrix);
    android.graphics.Region region = new android.graphics.Region();
    region.setPath(transformed, new android.graphics.Region(0, 0, v.canvasWidth, v.canvasHeight));
    if (!DrawingView.nativeTransformSelection(left, top, w, h, mask, degrees, sx, sy)) return false;
    v.deselect();
    if (!region.isEmpty()) {
      v.freeRegion.set(region);
      v.wandBoundary.set(region.getBoundaryPath());
      android.graphics.Rect bounds = region.getBounds();
      v.selectionLeft = bounds.left;
      v.selectionTop = bounds.top;
      v.selectionRight = bounds.right;
      v.selectionBottom = bounds.bottom;
      v.selectionTool = DrawingView.MAGIC_WAND;
      v.hasSelection = true;
    }
    v.refresh();
    return true;
  }

  static boolean moveSelectedPixels(DrawingView v, int dx, int dy) {
    if (!v.hasSelection() || (dx == 0 && dy == 0)) return false;
    int left = (int) Math.floor(v.selectionLeft), top = (int) Math.floor(v.selectionTop);
    int right = (int) Math.ceil(v.selectionRight), bottom = (int) Math.ceil(v.selectionBottom);
    if (left < 0
        || top < 0
        || right > v.canvasWidth
        || bottom > v.canvasHeight
        || right <= left
        || bottom <= top) return false;
    dx = Math.max(-left, Math.min(v.canvasWidth - right, dx));
    dy = Math.max(-top, Math.min(v.canvasHeight - bottom, dy));
    if (dx == 0 && dy == 0) return false;
    boolean ok;
    if (v.selectionTool == DrawingView.SELECT_FREE || v.selectionTool == DrawingView.MAGIC_WAND) {
      int w = right - left, h = bottom - top;
      if (w <= 0 || h <= 0 || ((long) w * h) > v.canvasWidth * v.canvasHeight) return false;
      byte[] mask = new byte[w * h];
      for (int row = 0; row < h; ++row)
        for (int col = 0; col < w; ++col)
          if (v.freeRegion.contains(left + col, top + row)) mask[row * w + col] = 1;
      ok = DrawingView.nativeMoveMaskedSelection(left, top, w, h, mask, dx, dy);
      if (ok) {
        android.graphics.Matrix matrix = new android.graphics.Matrix();
        matrix.setTranslate(dx, dy);
        v.freePath.transform(matrix);
        v.freeRegion.translate(dx, dy);
        v.wandBoundary.transform(matrix);
      }
    } else ok = DrawingView.nativeMovePixels(v.selectionTool, left, top, right, bottom, dx, dy);
    if (ok) {
      v.selectionLeft += dx;
      v.selectionRight += dx;
      v.selectionTop += dy;
      v.selectionBottom += dy;
      v.refresh();
    }
    return ok;
  }

  public void cancel(DrawingView v) {
    if (v.movingSelection) MoveSelectionTool.restoreMovedSelection(v);
  }
}
