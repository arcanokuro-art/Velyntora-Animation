package art.velyntora.core;

import android.graphics.*;

final class ShapeRaster {
  static void additionalShape(DrawingView v, int kind, float x0, float y0, float x1, float y1) {
    if (v.brushOpacity <= 0) return;
    float left = Math.min(x0, x1),
        top = Math.min(y0, y1),
        right = Math.max(x0, x1),
        bottom = Math.max(y0, y1);
    int x = Math.max(0, (int) Math.floor(left - v.brushRadius)),
        y = Math.max(0, (int) Math.floor(top - v.brushRadius));
    int w = Math.min(v.canvasWidth - x, (int) Math.ceil(right + v.brushRadius) - x + 1),
        h = Math.min(v.canvasHeight - y, (int) Math.ceil(bottom + v.brushRadius) - y + 1);
    if (w <= 0 || h <= 0) return;
    Bitmap shape = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
    try {
      Canvas target = new Canvas(shape);
      target.translate(-x, -y);
      drawShape(v,target,kind,x0,y0,x1,y1);
      int[] data = new int[2 + w * h];
      data[0] = w;
      data[1] = h;
      shape.getPixels(data, 2, w, 0, 0, w, h);
      if (v.hasSelection())
        for (int row = 0; row < h; row++)
          for (int col = 0; col < w; col++)
            if (!v.selectionContains(x + col, y + row)) data[2 + row * w + col] = 0;
      if (DrawingView.nativePasteSelection(data, x, y)) v.refresh();
    } finally {
      shape.recycle();
    }
  }
  static void drawShape(DrawingView v, Canvas target, int kind,
      float x0, float y0, float x1, float y1) {
    float left=Math.min(x0,x1), top=Math.min(y0,y1),
        right=Math.max(x0,x1), bottom=Math.max(y0,y1);
      Paint style = new Paint(Paint.ANTI_ALIAS_FLAG);
      style.setColor(v.color);
      style.setAlpha(Math.round((v.color >>> 24) * v.brushOpacity));
      style.setStrokeWidth(v.brushRadius * 2);
      style.setStrokeJoin(Paint.Join.ROUND);
      style.setStyle(
          kind == DrawingView.FILLED_RECTANGLE
                  || kind == DrawingView.FILLED_ELLIPSE
                  || kind == DrawingView.FILLED_ROUNDED_RECTANGLE
                  || kind == DrawingView.FILLED_TRIANGLE
              ? Paint.Style.FILL
              : Paint.Style.STROKE);
      if (kind == DrawingView.RECTANGLE || kind == DrawingView.FILLED_RECTANGLE)
        target.drawRect(left, top, right, bottom, style);
      else if (kind == DrawingView.ELLIPSE || kind == DrawingView.FILLED_ELLIPSE)
        target.drawOval(left, top, right, bottom, style);
      else if (kind == DrawingView.ROUNDED_RECTANGLE
          || kind == DrawingView.FILLED_ROUNDED_RECTANGLE) {
        float corner = Math.min(right - left, bottom - top) / 5f;
        target.drawRoundRect(left, top, right, bottom, corner, corner, style);
      } else {
        android.graphics.Path path = new android.graphics.Path();
        path.moveTo((left + right) / 2, top);
        path.lineTo(right, bottom);
        path.lineTo(left, bottom);
        path.close();
        target.drawPath(path, style);
      }
  }

}
