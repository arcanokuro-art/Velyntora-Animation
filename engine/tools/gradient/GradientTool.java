package art.velyntora.core;

import android.graphics.*;
import android.view.MotionEvent;

/** Gesture controller for gradient. Shared document state belongs to DrawingView. */
final class GradientTool implements DrawingTool {
  public void move(DrawingView v, float x, float y, MotionEvent event) {
    v.previousX = x;
    v.previousY = y;
    v.invalidate();
  }

  public void up(DrawingView v, float x, float y, MotionEvent event) {
    if (v.brushOpacity <= 0) return;
    int first = (v.color & 0xffffff) | (Math.round((v.color >>> 24) * v.brushOpacity) << 24),
        second =
            v.gradientTransparent
                ? 0
                : (v.secondaryColor & 0xffffff)
                    | (Math.round((v.secondaryColor >>> 24) * v.brushOpacity) << 24);
    int[] data =
        GradientRaster.create(
            v.canvasWidth, v.canvasHeight, v.startX, v.startY, x, y, first, second, v.gradientMode);
    if (v.hasSelection())
      for (int row = 0; row < v.canvasHeight; row++)
        for (int col = 0; col < v.canvasWidth; col++)
          if (!v.selectionContains(col, row)) data[2 + row * v.canvasWidth + col] = 0;
    DrawingView.nativePasteSelection(data, 0, 0);
  }

  public void preview(DrawingView v, Canvas canvas, float scale) {
    if (!v.drawing) return;
    Paint guide = new Paint(Paint.ANTI_ALIAS_FLAG);
    guide.setColor(0xff7040b0);
    guide.setStrokeWidth(2 / scale);
    canvas.drawLine(v.startX, v.startY, v.previousX, v.previousY, guide);
    canvas.drawCircle(v.startX, v.startY, 4 / scale, guide);
    canvas.drawCircle(v.previousX, v.previousY, 4 / scale, guide);
  }
}
