package art.velyntora.core;

import android.graphics.Canvas;
import android.view.MotionEvent;

/** Immediate overlay while dragging; a single native edit when released. */
abstract class ShapeTool implements DrawingTool {
  public void down(DrawingView v, float x, float y, MotionEvent event) {
    v.invalidate();
  }

  public void move(DrawingView v, float x, float y, MotionEvent event) {
    v.postInvalidateOnAnimation();
  }

  public void preview(DrawingView v, Canvas canvas, float scale) {
    if (!v.drawing) return;
    float x = v.previousX, y = v.previousY;
    int kind = v.tool;
    if (kind == DrawingView.CIRCLE) {
      float size = Math.min(Math.abs(x-v.startX),Math.abs(y-v.startY));
      x = v.startX + Math.copySign(size,x-v.startX);
      y = v.startY + Math.copySign(size,y-v.startY);
      kind = DrawingView.ELLIPSE;
    }
    ShapeRaster.drawShape(v,canvas,kind,v.startX,v.startY,x,y);
  }
}
