package art.velyntora.core;

import android.graphics.*;
import android.view.MotionEvent;

/** Gesture controller for brush. Shared document state belongs to DrawingView. */
final class BrushTool implements DrawingTool {
  public void down(DrawingView v, float x, float y, MotionEvent event) {
    v.brushes().begin();
    v.paintStroke(x, y, x, y, event);
    v.requestStrokeRefresh();
  }

  public void move(DrawingView v, float x, float y, MotionEvent event) {
    if (x != v.previousX || y != v.previousY) {
      v.paintStroke(v.previousX, v.previousY, x, y, event);
      v.requestStrokeRefresh();
    }
  }

  public void cancel(DrawingView v) {v.brushes().stop();}

  public void up(DrawingView v, float x, float y, MotionEvent event) {
    if (!v.strokeEditing || x != v.previousX || y != v.previousY)
      v.paintStroke(v.previousX, v.previousY, x, y, event);
    v.brushes().stop();
  }

  public void stroke(DrawingView v, float x0, float y0, float x1, float y1, float pressure) {
    if (v.brushes().isVelyntoraPreset())v.brushes().runtime.stroke(x0,y0,x1,y1,pressure);
    else if (v.brushes().isAntialiased())
      AntialiasedBrush.stroke(v,x0,y0,x1,y1,pressure);
    else ClassicBrush.stroke(v,x0,y0,x1,y1,pressure);
  }
}
