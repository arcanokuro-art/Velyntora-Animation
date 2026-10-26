package art.velyntora.core;

import android.graphics.*;

/** Tinted neighboring proxies, aligned with the engine's pan/zoom/rotation. White is suppressed. */
final class OnionSkin implements DrawingView.AnimationOverlay {
  private Bitmap previous, next;
  private boolean enabled;

  void set(Bitmap previous, Bitmap next, boolean enabled) {
    clear();
    this.previous = previous;
    this.next = next;
    this.enabled = enabled;
  }

  void clear() {
    if (previous != null) previous.recycle();
    if (next != null) next.recycle();
    previous = next = null;
  }

  public void draw(Canvas canvas, Viewport v, int width, int height) {
    if (!enabled) return;
    canvas.save();
    canvas.translate((float) v.centerX, (float) v.centerY);
    canvas.rotate((float) v.angle);
    canvas.scale((float) v.scale, (float) v.scale);
    canvas.translate(-width / 2f, -height / 2f);
    canvas.clipRect(0, 0, width, height);
    draw(canvas, previous, width, height, 1, 0.25f, 0.25f);
    draw(canvas, next, width, height, 0.2f, 0.5f, 1);
    canvas.restore();
  }

  private void draw(Canvas c, Bitmap b, int w, int h, float r, float g, float blue) {
    if (b == null) return;
    Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    p.setColorFilter(
        new ColorMatrixColorFilter(
            new float[] {
              0, 0, 0, 0, 255 * r, 0, 0, 0, 0, 255 * g, 0, 0, 0, 0, 255 * blue, -0.10f, -0.10f,
              -0.10f, 0.3f, 0
            }));
    c.drawBitmap(b, null, new Rect(0, 0, w, h), p);
  }
}
