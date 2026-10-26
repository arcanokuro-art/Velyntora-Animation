package art.velyntora.core;

import android.graphics.*;
import android.view.MotionEvent;

final class StrokeSupport {
  static void paintStroke(
      DrawingView v, float x0, float y0, float x1, float y1, MotionEvent event) {
    if (v.brushOpacity <= 0) return;
    if (!v.strokeEditing) {
      byte[] mask = null;
      if (v.hasSelection()) {
        mask = new byte[v.canvasWidth * v.canvasHeight];
        int left = Math.max(0, (int) Math.floor(v.selectionLeft)),
            right = Math.min(v.canvasWidth, (int) Math.ceil(v.selectionRight));
        int top = Math.max(0, (int) Math.floor(v.selectionTop)),
            bottom = Math.min(v.canvasHeight, (int) Math.ceil(v.selectionBottom));
        for (int y = top; y < bottom; ++y)
          for (int x = left; x < right; ++x)
            if (v.selectionContains(x, y)) mask[y * v.canvasWidth + x] = 1;
      }
      DrawingView.nativeSetBrushSelection(mask);
      if (!DrawingView.nativeBeginOpacityStroke(
          (v.tool == DrawingView.BRUSH && v.brushes().isAntialiased()) || v.brushOpacity < 1 || v.brushHardness < 1 || (v.color >>> 24) < 255 || v.tool == DrawingView.CLONE)) return;
      DrawingView.nativeBeginEdit();
      if (v.tool == DrawingView.CLONE || v.tool == DrawingView.RECOLOR)
        DrawingView.nativeBeginSampled((int) v.startX, (int) v.startY);
      v.strokeEditing = true;
    }
    float pressure =
        v.pressureBrush && event.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS
            ? Math.max(.1f, Math.min(1f, event.getPressure()))
            : 1f;
    Tools.controller(v.tool).stroke(v, x0, y0, x1, y1, pressure);
    v.noteStrokeBounds(x0,y0,x1,y1,v.brushRadius*((v.tool == DrawingView.BRUSH && v.brushes().isVelyntoraPreset()) || v.tool == DrawingView.PENCIL ? 1.415f : pressure));
  }
}
