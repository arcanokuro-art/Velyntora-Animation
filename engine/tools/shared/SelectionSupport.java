package art.velyntora.core;

import android.graphics.*;

final class SelectionSupport {
  static void updateSelection(DrawingView v, float x, float y) {
    float a = Math.max(0f, Math.min(v.canvasWidth, v.startX)),
        b = Math.max(0f, Math.min(v.canvasHeight, v.startY));
    float c = Math.max(0f, Math.min(v.canvasWidth, x)),
        d = Math.max(0f, Math.min(v.canvasHeight, y));
    v.selectionLeft = Math.min(a, c);
    v.selectionRight = Math.max(a, c);
    v.selectionTop = Math.min(b, d);
    v.selectionBottom = Math.max(b, d);
    v.invalidate();
  }

  static boolean selectionContains(DrawingView v, int px, int py) {
    if (!v.hasSelection()
        || px < 0
        || py < 0
        || px >= v.canvasWidth
        || py >= v.canvasHeight
        || px < v.selectionLeft
        || py < v.selectionTop
        || px >= v.selectionRight
        || py >= v.selectionBottom) return false;
    if (v.selectionTool == DrawingView.SELECT_RECTANGLE) return true;
    if (v.selectionTool == DrawingView.SELECT_FREE)
      return v.freeSelectionReady && v.freeRegion.contains(px, py);
    if (v.selectionTool == DrawingView.MAGIC_WAND) return v.freeRegion.contains(px, py);
    final float rx = (v.selectionRight - v.selectionLeft) / 2f,
        ry = (v.selectionBottom - v.selectionTop) / 2f;
    if (rx <= 0f || ry <= 0f) return false;
    final float cx = (v.selectionRight + v.selectionLeft) / 2f,
        cy = (v.selectionBottom + v.selectionTop) / 2f;
    final float dx = (px + 0.5f - cx) / rx, dy = (py + 0.5f - cy) / ry;
    return dx * dx + dy * dy <= 1f;
  }

  static byte[] selectionMask(DrawingView v) {
    if (!v.hasSelection()) return null;
    byte[] mask = new byte[v.canvasWidth * v.canvasHeight];
    int left = Math.max(0, (int) Math.floor(v.selectionLeft)),
        right = Math.min(v.canvasWidth, (int) Math.ceil(v.selectionRight)),
        top = Math.max(0, (int) Math.floor(v.selectionTop)),
        bottom = Math.min(v.canvasHeight, (int) Math.ceil(v.selectionBottom));
    for (int y = top; y < bottom; y++)
      for (int x = left; x < right; x++)
        if (v.selectionContains(x, y)) mask[y * v.canvasWidth + x] = 1;
    return mask;
  }

  static boolean shrinkSelectionOnePixel(DrawingView v) {
    if (!v.hasSelection()) return false;
    android.graphics.Region original = new android.graphics.Region();
    if (v.selectionTool == DrawingView.SELECT_FREE || v.selectionTool == DrawingView.MAGIC_WAND)
      original.set(v.freeRegion);
    else if (v.selectionTool == DrawingView.SELECT_RECTANGLE)
      original.set(
          (int) Math.floor(v.selectionLeft),
          (int) Math.floor(v.selectionTop),
          (int) Math.ceil(v.selectionRight),
          (int) Math.ceil(v.selectionBottom));
    else if (v.selectionTool == DrawingView.SELECT_ELLIPSE) {
      android.graphics.Path ellipse = new android.graphics.Path();
      ellipse.addOval(
          new RectF(v.selectionLeft, v.selectionTop, v.selectionRight, v.selectionBottom),
          android.graphics.Path.Direction.CW);
      original.setPath(ellipse, new android.graphics.Region(0, 0, v.canvasWidth, v.canvasHeight));
    } else return false;
    original.op(0, 0, v.canvasWidth, v.canvasHeight, android.graphics.Region.Op.INTERSECT);
    android.graphics.Region eroded = new android.graphics.Region(original);
    android.graphics.Region shifted = new android.graphics.Region();
    for (int dx = -1; dx <= 1; dx++)
      for (int dy = -1; dy <= 1; dy++) {
        if (dx == 0 && dy == 0) continue;
        original.translate(dx, dy, shifted);
        eroded.op(shifted, android.graphics.Region.Op.INTERSECT);
      }
    // A fully eroded selection is a valid result: clear it.
    if (eroded.isEmpty()) {
      v.deselect();
      return true;
    }
    v.freeRegion.set(eroded);
    v.freePath.reset();
    v.wandBoundary.set(v.freeRegion.getBoundaryPath());
    android.graphics.Rect bounds = v.freeRegion.getBounds();
    v.selectionLeft = bounds.left;
    v.selectionTop = bounds.top;
    v.selectionRight = bounds.right;
    v.selectionBottom = bounds.bottom;
    v.selectionTool = DrawingView.MAGIC_WAND;
    v.hasSelection = true;
    v.invalidate();
    return true;
  }

  static boolean expandSelectionOnePixel(DrawingView v) {
    if (!v.hasSelection()) return false;
    android.graphics.Region region = new android.graphics.Region();
    if (v.selectionTool == DrawingView.SELECT_FREE || v.selectionTool == DrawingView.MAGIC_WAND)
      region.set(v.freeRegion);
    else if (v.selectionTool == DrawingView.SELECT_RECTANGLE)
      region.set(
          (int) Math.floor(v.selectionLeft),
          (int) Math.floor(v.selectionTop),
          (int) Math.ceil(v.selectionRight),
          (int) Math.ceil(v.selectionBottom));
    else if (v.selectionTool == DrawingView.SELECT_ELLIPSE) {
      android.graphics.Path ellipse = new android.graphics.Path();
      ellipse.addOval(
          new RectF(v.selectionLeft, v.selectionTop, v.selectionRight, v.selectionBottom),
          android.graphics.Path.Direction.CW);
      region.setPath(ellipse, new android.graphics.Region(0, 0, v.canvasWidth, v.canvasHeight));
    } else return false;
    region.op(0, 0, v.canvasWidth, v.canvasHeight, android.graphics.Region.Op.INTERSECT);
    android.graphics.Region grown = new android.graphics.Region(region);
    android.graphics.Region offset = new android.graphics.Region();
    for (int dx = -1; dx <= 1; dx++)
      for (int dy = -1; dy <= 1; dy++) {
        if (dx == 0 && dy == 0) continue;
        region.translate(dx, dy, offset);
        grown.op(offset, android.graphics.Region.Op.UNION);
      }
    grown.op(0, 0, v.canvasWidth, v.canvasHeight, android.graphics.Region.Op.INTERSECT);
    if (grown.isEmpty()) return false;
    v.freeRegion.set(grown);
    v.freePath.reset();
    v.wandBoundary.set(v.freeRegion.getBoundaryPath());
    android.graphics.Rect bounds = v.freeRegion.getBounds();
    v.selectionLeft = bounds.left;
    v.selectionTop = bounds.top;
    v.selectionRight = bounds.right;
    v.selectionBottom = bounds.bottom;
    v.selectionTool = DrawingView.MAGIC_WAND;
    v.hasSelection = true;
    v.invalidate();
    return true;
  }

  static boolean invertSelection(DrawingView v) {
    if (!v.hasSelection()) return false;
    android.graphics.Region selected = new android.graphics.Region();
    if (v.selectionTool == DrawingView.SELECT_FREE || v.selectionTool == DrawingView.MAGIC_WAND) {
      selected.set(v.freeRegion);
    } else if (v.selectionTool == DrawingView.SELECT_RECTANGLE) {
      selected.set(
          (int) Math.floor(v.selectionLeft),
          (int) Math.floor(v.selectionTop),
          (int) Math.ceil(v.selectionRight),
          (int) Math.ceil(v.selectionBottom));
    } else if (v.selectionTool == DrawingView.SELECT_ELLIPSE) {
      android.graphics.Path ellipse = new android.graphics.Path();
      ellipse.addOval(
          new RectF(v.selectionLeft, v.selectionTop, v.selectionRight, v.selectionBottom),
          android.graphics.Path.Direction.CW);
      selected.setPath(ellipse, new android.graphics.Region(0, 0, v.canvasWidth, v.canvasHeight));
    } else return false;
    selected.op(0, 0, v.canvasWidth, v.canvasHeight, android.graphics.Region.Op.INTERSECT);
    android.graphics.Region inverted =
        new android.graphics.Region(0, 0, v.canvasWidth, v.canvasHeight);
    inverted.op(selected, android.graphics.Region.Op.DIFFERENCE);
    if (inverted.isEmpty()) {
      v.deselect();
      return true;
    }
    v.freeRegion.set(inverted);
    v.freePath.reset();
    v.wandBoundary.set(v.freeRegion.getBoundaryPath());
    android.graphics.Rect bounds = v.freeRegion.getBounds();
    v.selectionLeft = bounds.left;
    v.selectionTop = bounds.top;
    v.selectionRight = bounds.right;
    v.selectionBottom = bounds.bottom;
    v.selectionTool = DrawingView.MAGIC_WAND;
    v.hasSelection = true;
    v.invalidate();
    return true;
  }

  static Bitmap copySelection(DrawingView v) {
    if (!v.hasSelection()) return null;
    int leftBound = (int) Math.floor(v.selectionLeft), topBound = (int) Math.floor(v.selectionTop);
    int rightBound = (int) Math.ceil(v.selectionRight),
        bottomBound = (int) Math.ceil(v.selectionBottom);
    if (leftBound < 0
        || topBound < 0
        || rightBound > v.canvasWidth
        || bottomBound > v.canvasHeight
        || rightBound <= leftBound
        || bottomBound <= topBound) return null;
    int[] data =
        DrawingView.nativeCopySelection(
            (v.selectionTool == DrawingView.SELECT_FREE
                    || v.selectionTool == DrawingView.MAGIC_WAND)
                ? DrawingView.SELECT_RECTANGLE
                : v.selectionTool,
            (int) Math.floor(v.selectionLeft),
            (int) Math.floor(v.selectionTop),
            (int) Math.ceil(v.selectionRight),
            (int) Math.ceil(v.selectionBottom));
    if (data == null || data.length < 3) return null;
    int w = data[0], h = data[1];
    if (w <= 0
        || h <= 0
        || w > v.canvasWidth
        || h > v.canvasHeight
        || w != rightBound - leftBound
        || h != bottomBound - topBound
        || ((long) w * h) != data.length - 2) return null;
    if (v.selectionTool == DrawingView.SELECT_FREE || v.selectionTool == DrawingView.MAGIC_WAND) {
      int left = (int) Math.floor(v.selectionLeft), top = (int) Math.floor(v.selectionTop);
      for (int row = 0; row < h; ++row)
        for (int col = 0; col < w; ++col) {
          if (!v.freeRegion.contains(left + col, top + row)) data[2 + row * w + col] = 0;
        }
    }
    return Bitmap.createBitmap(data, 2, w, w, h, Bitmap.Config.ARGB_8888);
  }

  static boolean eraseSelection(DrawingView v) {
    if (!v.hasSelection()) return false;
    boolean ok;
    if (v.selectionTool == DrawingView.SELECT_FREE || v.selectionTool == DrawingView.MAGIC_WAND) {
      int left = (int) Math.floor(v.selectionLeft), top = (int) Math.floor(v.selectionTop);
      int w = (int) Math.ceil(v.selectionRight) - left,
          h = (int) Math.ceil(v.selectionBottom) - top;
      if (w <= 0
          || h <= 0
          || left < 0
          || top < 0
          || left + w > v.canvasWidth
          || top + h > v.canvasHeight
          || ((long) w * h) > v.canvasWidth * v.canvasHeight) return false;
      byte[] mask = new byte[w * h];
      for (int row = 0; row < h; ++row)
        for (int col = 0; col < w; ++col)
          if (v.freeRegion.contains(left + col, top + row)) mask[row * w + col] = 1;
      ok = DrawingView.nativeEraseMaskedSelection(left, top, w, h, mask);
    } else
      ok =
          DrawingView.nativeEraseSelection(
              v.selectionTool,
              (int) Math.floor(v.selectionLeft),
              (int) Math.floor(v.selectionTop),
              (int) Math.ceil(v.selectionRight),
              (int) Math.ceil(v.selectionBottom));
    if (ok) {
      v.deselect();
      v.refresh();
    }
    return ok;
  }

  static void selectAll(DrawingView v) {
    v.movingSelection = false;
    v.movingPixels = false;
    v.drawing = false;
    v.selectionTool = DrawingView.SELECT_RECTANGLE;
    v.selectionLeft = 0f;
    v.selectionTop = 0f;
    v.selectionRight = v.canvasWidth;
    v.selectionBottom = v.canvasHeight;
    v.hasSelection = true;
    v.invalidate();
  }

  static void deselect(DrawingView v) {
    v.hasSelection = false;
    v.movingSelection = false;
    v.movingPixels = false;
    v.freeSelectionReady = false;
    v.freeRegion.setEmpty();
    v.freePath.reset();
    v.wandBoundary.reset();
    v.invalidate();
  }

  static boolean trimActiveToFreeSelection(DrawingView v) {
    if (!v.hasSelection()
        || (v.selectionTool != DrawingView.SELECT_FREE
            && v.selectionTool != DrawingView.MAGIC_WAND)) return false;
    int left = (int) Math.floor(v.selectionLeft), top = (int) Math.floor(v.selectionTop);
    int right = (int) Math.ceil(v.selectionRight), bottom = (int) Math.ceil(v.selectionBottom);
    if (left < 0
        || top < 0
        || right > v.canvasWidth
        || bottom > v.canvasHeight
        || left >= right
        || top >= bottom) return false;
    int width = right - left, height = bottom - top;
    byte[] mask = new byte[width * height];
    for (int y = 0; y < height; ++y)
      for (int x = 0; x < width; ++x)
        if (v.freeRegion.contains(left + x, top + y)) mask[y * width + x] = 1;
    if (!DrawingView.nativeTrimActiveMasked(left, top, width, height, mask)) return false;
    v.deselect();
    v.refresh();
    return true;
  }

  static boolean trimActiveToEllipseSelection(DrawingView v) {
    if (!v.hasSelection() || v.selectionTool != DrawingView.SELECT_ELLIPSE) return false;
    int left = (int) Math.floor(v.selectionLeft), top = (int) Math.floor(v.selectionTop);
    int right = (int) Math.ceil(v.selectionRight), bottom = (int) Math.ceil(v.selectionBottom);
    if (left < 0
        || top < 0
        || right > v.canvasWidth
        || bottom > v.canvasHeight
        || left >= right
        || top >= bottom) return false;
    if (!DrawingView.nativeCropActiveEllipse(left, top, right, bottom)) return false;
    v.deselect();
    v.refresh();
    return true;
  }

  static boolean trimActiveToRectSelection(DrawingView v) {
    if (!v.hasSelection() || v.selectionTool != DrawingView.SELECT_RECTANGLE) return false;
    int left = (int) Math.floor(v.selectionLeft), top = (int) Math.floor(v.selectionTop);
    int right = (int) Math.ceil(v.selectionRight), bottom = (int) Math.ceil(v.selectionBottom);
    if (left < 0
        || top < 0
        || right > v.canvasWidth
        || bottom > v.canvasHeight
        || left >= right
        || top >= bottom) return false;
    if (!DrawingView.nativeCropActiveSelection(left, top, right, bottom)) return false;
    v.deselect();
    v.refresh();
    return true;
  }

  static void draw(DrawingView v, Canvas canvas, float scale) {
    if (!v.hasSelection()) return;
    v.selectionPaint.setColor(0xFF202020);
    v.selectionPaint.setStyle(Paint.Style.STROKE);
    v.selectionPaint.setStrokeWidth(Math.max(1f, 1f / scale));
    v.selectionPaint.setPathEffect(
        new android.graphics.DashPathEffect(new float[] {6f / scale, 4f / scale}, 0));
    RectF bounds = new RectF(v.selectionLeft, v.selectionTop, v.selectionRight, v.selectionBottom);
    if (v.selectionTool == DrawingView.SELECT_ELLIPSE) canvas.drawOval(bounds, v.selectionPaint);
    else if (v.selectionTool == DrawingView.SELECT_FREE) {
      canvas.save();
      canvas.drawPath(v.freePath, v.selectionPaint);
      canvas.restore();
    } else if (v.selectionTool == DrawingView.MAGIC_WAND) {
      canvas.save();
      canvas.drawPath(v.wandBoundary, v.selectionPaint);
      canvas.restore();
    } else canvas.drawRect(bounds, v.selectionPaint);
    v.selectionPaint.setPathEffect(null);
  }
}
