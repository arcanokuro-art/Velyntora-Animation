package art.velyntora.core;

import android.graphics.*;
import android.view.MotionEvent;

/** Gesture controller for freeform. Shared document state belongs to DrawingView. */
final class FreeformTool implements DrawingTool {
  public void down(DrawingView v, float x, float y, MotionEvent event) {
    v.shapePath.reset();
    v.shapePath.moveTo(x, y);
    v.invalidate();
  }

  public void move(DrawingView v, float x, float y, MotionEvent event) {
    v.shapePath.lineTo(
        Math.max(0, Math.min(v.canvasWidth, x)), Math.max(0, Math.min(v.canvasHeight, y)));
    v.invalidate();
  }

  public void up(DrawingView v, float x, float y, MotionEvent event) {
    v.shapePath.lineTo(
        Math.max(0, Math.min(v.canvasWidth, x)), Math.max(0, Math.min(v.canvasHeight, y)));
    v.commitFreeform();
  }

  static void commitFreeform(DrawingView v) {
    if (v.brushOpacity <= 0) { v.shapePath.reset(); return; }
    Bitmap shape = Bitmap.createBitmap(v.canvasWidth, v.canvasHeight, Bitmap.Config.ARGB_8888);
    try {
      Paint style = new Paint(Paint.ANTI_ALIAS_FLAG);
      style.setColor(v.color);
      style.setAlpha(Math.round((v.color >>> 24) * v.brushOpacity));
      style.setStrokeWidth(v.brushRadius * 2);
      style.setStrokeJoin(Paint.Join.ROUND);
      style.setStyle(Paint.Style.STROKE);
      v.shapePath.close();
      new Canvas(shape).drawPath(v.shapePath, style);
      int[] data = new int[2 + v.canvasWidth * v.canvasHeight];
      data[0] = v.canvasWidth;
      data[1] = v.canvasHeight;
      shape.getPixels(data, 2, v.canvasWidth, 0, 0, v.canvasWidth, v.canvasHeight);
      if (v.hasSelection())
        for (int row = 0; row < v.canvasHeight; row++)
          for (int col = 0; col < v.canvasWidth; col++)
            if (!v.selectionContains(col, row)) data[2 + row * v.canvasWidth + col] = 0;
      DrawingView.nativePasteSelection(data, 0, 0);
    } finally {
      shape.recycle();
      v.shapePath.reset();
    }
  }

  public void cancel(DrawingView v) {
    v.shapePath.reset();
    v.invalidate();
  }

  public void preview(DrawingView v, Canvas canvas, float scale) {
    if (!v.drawing) return;
    Paint preview = new Paint(Paint.ANTI_ALIAS_FLAG);
    preview.setColor(v.color);
    preview.setAlpha(Math.round((v.color >>> 24) * v.brushOpacity));
    preview.setStyle(Paint.Style.STROKE);
    preview.setStrokeWidth(v.brushRadius * 2);
    preview.setStrokeJoin(Paint.Join.ROUND);
    canvas.drawPath(v.shapePath, preview);
  }
}
