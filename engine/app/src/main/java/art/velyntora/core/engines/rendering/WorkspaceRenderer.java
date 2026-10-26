package art.velyntora.core;

import android.graphics.*;

/** Viewport, checkerboard and sharp source pixels; tool overlays are delegated. */
final class WorkspaceRenderer {
  static void draw(DrawingView v, Canvas canvas) {

    if (v.getWidth() <= 0 || v.getHeight() <= 0) return;
    float scale = (float) v.viewport.scale;
    canvas.drawColor(0xFF2B2B2B);
    canvas.save();
    canvas.translate((float) v.viewport.centerX, (float) v.viewport.centerY);
    canvas.rotate((float) v.viewport.angle);
    canvas.scale(scale, scale);
    canvas.translate(-v.canvasWidth / 2f, -v.canvasHeight / 2f);
    canvas.clipRect(0, 0, v.canvasWidth, v.canvasHeight);
    Checkerboard.draw(canvas,v.canvasWidth,v.canvasHeight);
    // Interpolate only when reducing the image. Enlarged source pixels stay sharp.
    v.paint.setFilterBitmap(scale < 1f);
    if(!v.placedImage().drawBase(canvas) && !v.selectionDragPreview.draw(canvas))v.drawCanvasPixels(canvas);

    if(v.placedImage().active())v.placedImage().draw(canvas,scale);
    else {if (v.isEnabled()) Tools.controller(v.tool).preview(v, canvas, scale);
      v.selectionDragPreview.contour(canvas, scale);}
    canvas.restore();
  }
}
