package art.velyntora.core;

import android.graphics.*;

/** Neighbor proxies follow native canvas pan, zoom and rotation, without modifying saved pixels. */
final class OnionSkin implements DrawingView.AnimationOverlay {
  private Bitmap previous, next;

  void set(Bitmap previous, Bitmap next) {
    this.previous = previous;
    this.next = next;
  }

  public void draw(Canvas canvas, Viewport v, int width, int height) {
    canvas.save();
    canvas.translate((float) v.centerX, (float) v.centerY);
    canvas.rotate((float) v.angle);
    canvas.scale((float) v.scale, (float) v.scale);
    canvas.translate(-width / 2f, -height / 2f);
    canvas.clipRect(0, 0, width, height);
    draw(canvas, previous, width, height, 1, .25f, .25f);
    draw(canvas, next, width, height, .2f, .55f, 1);
    canvas.restore();
  }

  private void draw(Canvas c, Bitmap b, int w, int h, float r, float g, float blue) {
    if (b == null) return;
    Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    p.setColorFilter(
        new ColorMatrixColorFilter(
            new float[] {
              0, 0, 0, 0, 255 * r, 0, 0, 0, 0, 255 * g, 0, 0, 0, 0, 255 * blue, -.1f, -.1f, -.1f,
              .3f, 0
            }));
    c.drawBitmap(b, null, new Rect(0, 0, w, h), p);
  }
}
