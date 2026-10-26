package art.velyntora.core;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Shows RGBA over a checkerboard so partial transparency remains visible. */
final class ColorSwatchDrawable extends Drawable {
  private final int color;
  private final Paint paint = new Paint();

  ColorSwatchDrawable(int value) {
    color = value;
  }

  @Override
  public void draw(Canvas canvas) {
    Rect bounds = getBounds();
    int cell = Math.max(4, Math.min(bounds.width(), bounds.height()) / 6);
    paint.setStyle(Paint.Style.FILL);
    for (int y = bounds.top; y < bounds.bottom; y += cell)
      for (int x = bounds.left; x < bounds.right; x += cell) {
        paint.setColor(
            (((x - bounds.left) / cell + (y - bounds.top) / cell) & 1) == 0
                ? 0xffffffff
                : 0xffcccccc);
        canvas.drawRect(
            x, y, Math.min(x + cell, bounds.right), Math.min(y + cell, bounds.bottom), paint);
      }
    paint.setColor(color);
    canvas.drawRect(bounds, paint);
    paint.setColor(0xff333333);
    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeWidth(2);
    canvas.drawRect(bounds.left + 1, bounds.top + 1, bounds.right - 1, bounds.bottom - 1, paint);
  }

  @Override
  public void setAlpha(int alpha) {}

  @Override
  public void setColorFilter(ColorFilter filter) {}

  @Override
  public int getOpacity() {
    return PixelFormat.OPAQUE;
  }
}
