package art.velyntora.core;

import android.graphics.*;
import android.view.MotionEvent;

/** Gesture controller for magic_wand. Shared document state belongs to DrawingView. */
final class MagicWandTool implements DrawingTool {
  public void down(DrawingView v, float x, float y, MotionEvent event) {
    v.selectMatchingRegion((int) x, (int) y);
    v.drawing = false;
  }

  static void selectMatchingRegion(DrawingView v, int sx, int sy) {
    if (sx < 0 || sy < 0 || sx >= v.canvasWidth || sy >= v.canvasHeight) return;
    int[] pixels = new int[v.canvasWidth * v.canvasHeight];
    v.bitmap.getPixels(pixels, 0, v.canvasWidth, 0, 0, v.canvasWidth, v.canvasHeight);
    int target = pixels[sy * v.canvasWidth + sx];
    byte[] visited = new byte[pixels.length];
    int[] queue = new int[pixels.length];
    int head = 0, tail = 0, start = sy * v.canvasWidth + sx;
    visited[start] = 1;
    queue[tail++] = start;
    int minX = sx, maxX = sx, minY = sy, maxY = sy;
    v.freeRegion.setEmpty();
    v.freePath.reset();
    v.wandBoundary.reset();
    while (head < tail) {
      int pos = queue[head++], px = pos % v.canvasWidth, py = pos / v.canvasWidth;
      visited[pos] = 2;
      minX = Math.min(minX, px);
      maxX = Math.max(maxX, px);
      minY = Math.min(minY, py);
      maxY = Math.max(maxY, py);
      if (px > 0) {
        int q = pos - 1;
        if (visited[q] == 0) {
          visited[q] = 1;
          if (withinTolerance(pixels[q], target, v.wandTolerance)) queue[tail++] = q;
        }
      }
      if (px < v.canvasWidth - 1) {
        int q = pos + 1;
        if (visited[q] == 0) {
          visited[q] = 1;
          if (withinTolerance(pixels[q], target, v.wandTolerance)) queue[tail++] = q;
        }
      }
      if (py > 0) {
        int q = pos - v.canvasWidth;
        if (visited[q] == 0) {
          visited[q] = 1;
          if (withinTolerance(pixels[q], target, v.wandTolerance)) queue[tail++] = q;
        }
      }
      if (py < v.canvasHeight - 1) {
        int q = pos + v.canvasWidth;
        if (visited[q] == 0) {
          visited[q] = 1;
          if (withinTolerance(pixels[q], target, v.wandTolerance)) queue[tail++] = q;
        }
      }
    }
    // Union horizontal runs, not individual pixels, to reduce Region operations.
    for (int row = minY; row <= maxY; ++row) {
      int col = minX;
      while (col <= maxX) {
        while (col <= maxX && visited[row * v.canvasWidth + col] != 2) ++col;
        int left = col;
        while (col <= maxX && visited[row * v.canvasWidth + col] == 2) ++col;
        if (left < col) v.freeRegion.op(left, row, col, row + 1, android.graphics.Region.Op.UNION);
      }
    }
    v.selectionTool = DrawingView.MAGIC_WAND;
    v.selectionLeft = minX;
    v.selectionTop = minY;
    v.selectionRight = maxX + 1;
    v.selectionBottom = maxY + 1;
    v.hasSelection = !v.freeRegion.isEmpty();
    if (v.hasSelection) v.wandBoundary.set(v.freeRegion.getBoundaryPath());
    v.invalidate();
  }

  private static boolean withinTolerance(int color, int target, int tolerance) {
    int da = Math.abs((color >>> 24) - (target >>> 24));
    int dr = Math.abs(((color >>> 16) & 255) - ((target >>> 16) & 255));
    int dg = Math.abs(((color >>> 8) & 255) - ((target >>> 8) & 255));
    int db = Math.abs((color & 255) - (target & 255));
    return Math.max(Math.max(da, dr), Math.max(dg, db)) <= tolerance;
  }
}
