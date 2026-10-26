package art.velyntora.core;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Monochrome tool geometry drawn at a consistent 24 dp optical size. */
abstract class ToolIcon extends Drawable {
  final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

  ToolIcon() {
    p.setColor(0xffeeeeee);
    p.setStyle(Paint.Style.STROKE);
    p.setStrokeWidth(1.8f);
    p.setStrokeCap(Paint.Cap.ROUND);
    p.setStrokeJoin(Paint.Join.ROUND);
  }

  final void line(Canvas c, float... xy) {
    Path path = new Path();
    path.moveTo(xy[0], xy[1]);
    for (int i = 2; i < xy.length; i += 2) path.lineTo(xy[i], xy[i + 1]);
    c.drawPath(path, p);
  }

  final void node(Canvas c, float x, float y) {
    c.drawRect(x - 2, y - 2, x + 2, y + 2, p);
  }

  abstract void geometry(Canvas c);

  @Override
  public final void draw(Canvas c) {
    c.save();
    c.translate(getBounds().left, getBounds().top);
    c.scale(getBounds().width() / 24f, getBounds().height() / 24f);
    geometry(c);
    c.restore();
  }

  final void move(Canvas c, float reach) {
    float a = 12 - reach, b = 12 + reach;
    line(c, a, 12, b, 12);
    line(c, 12, a, 12, b);
    line(c, a + 3, 9, a, 12, a + 3, 15);
    line(c, b - 3, 9, b, 12, b - 3, 15);
    line(c, 9, a + 3, 12, a, 15, a + 3);
    line(c, 9, b - 3, 12, b, 15, b - 3);
  }

  @Override
  public void setAlpha(int alpha) {
    p.setAlpha(alpha);
    invalidateSelf();
  }

  @Override
  public void setColorFilter(ColorFilter filter) {
    p.setColorFilter(filter);
    invalidateSelf();
  }

  @Override
  public int getOpacity() {
    return PixelFormat.TRANSLUCENT;
  }
}
