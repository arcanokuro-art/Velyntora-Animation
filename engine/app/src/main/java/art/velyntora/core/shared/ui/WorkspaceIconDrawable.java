package art.velyntora.core;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Small vector command icons; hit targets belong to the surrounding 48 dp button. */
final class WorkspaceIconDrawable extends Drawable {
  private final String glyph;
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

  WorkspaceIconDrawable(String glyph) {
    this.glyph = glyph;
    paint.setColor(0xffeeeeee);
    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeWidth(1.6f);
    paint.setStrokeCap(Paint.Cap.ROUND);
    paint.setStrokeJoin(Paint.Join.ROUND);
  }

  private void line(Canvas c, float... points) {
    Path path = new Path();
    path.moveTo(points[0], points[1]);
    for (int i = 2; i < points.length; i += 2) path.lineTo(points[i], points[i + 1]);
    c.drawPath(path, paint);
  }

  @Override
  public void draw(Canvas c) {
    c.save();
    c.translate(getBounds().left, getBounds().top);
    c.scale(getBounds().width() / 24f, getBounds().height() / 24f);
    switch (glyph) {
      case "menu":
        line(c, 4, 6, 20, 6);
        line(c, 4, 12, 20, 12);
        line(c, 4, 18, 20, 18);
        break;
      case "new":
        line(c, 14, 21, 4, 21, 4, 3, 14, 3, 19, 8, 19, 12);
        line(c, 14, 3, 14, 8, 19, 8);
        line(c, 14, 17, 22, 17);
        line(c, 18, 13, 18, 21);
        break;
      case "open":
        line(c, 3, 19, 3, 5, 10, 5, 12, 8, 21, 8, 21, 19, 3, 19, 6, 11, 21, 11);
        break;
      case "save":
        line(c, 12, 3, 12, 16);
        line(c, 7, 11, 12, 16, 17, 11);
        line(c, 4, 18, 4, 21, 20, 21, 20, 18);
        break;
      case "undo":
        line(c, 8, 5, 3, 10, 8, 15);
        line(c, 3, 10, 14, 10);
        c.drawArc(10, 10, 21, 21, -90, 170, false, paint);
        break;
      case "redo":
        line(c, 16, 5, 21, 10, 16, 15);
        line(c, 21, 10, 10, 10);
        c.drawArc(3, 10, 14, 21, -90, -170, false, paint);
        break;
      case "confirm":
        line(c, 4, 12, 9, 17, 20, 6);
        break;
      case "cancel":
        line(c, 5, 5, 19, 19);
        line(c, 19, 5, 5, 19);
        break;
      case "duplicate":
        c.drawRect(4,4,15,15,paint);c.drawRect(9,9,20,20,paint);break;
      case "merge":
        c.drawRect(3,17,21,21,paint);line(c,12,3,12,14);line(c,8,10,12,14);line(c,16,10,12,14);break;
      case "delete":
        line(c, 4, 6, 20, 6);
        line(c, 8, 3, 16, 3);
        line(c, 6, 6, 7, 21, 17, 21, 18, 6);
        line(c, 10, 10, 10, 17);
        line(c, 14, 10, 14, 17);
        break;
      case "up":
        line(c, 6, 12, 12, 6, 18, 12);
        line(c, 12, 6, 12, 20);
        break;
      case "down":
        line(c, 6, 12, 12, 18, 18, 12);
        line(c, 12, 4, 12, 18);
        break;
      case "swap":
        line(c, 3, 8, 20, 8, 16, 4);
        line(c, 21, 16, 4, 16, 8, 20);
        break;
      case "eye":
        c.drawOval(3, 7, 21, 17, paint);
        c.drawCircle(12, 12, 3, paint);
        break;
      case "layers":
        line(c, 3, 8, 12, 3, 21, 8, 12, 13, 3, 8);
        line(c, 3, 12, 12, 17, 21, 12);
        line(c, 3, 16, 12, 21, 21, 16);
        break;
      case "tools":
        line(c, 4, 20, 17, 7);
        line(c, 14, 4, 20, 10);
        line(c, 4, 16, 8, 20);
        break;
      case "gradient":
        for (int i = 0; i < 5; i++) {
          paint.setAlpha(55 + i * 50);
          line(c, 4 + i * 4, 4, 4 + i * 4, 20);
        }
        paint.setAlpha(255);
        break;
      default:
        c.drawCircle(12, 12, 6, paint);
        line(c, 12, 2, 12, 5);
        line(c, 12, 19, 12, 22);
        line(c, 2, 12, 5, 12);
        line(c, 19, 12, 22, 12);
    }
    c.restore();
  }

  @Override
  public void setAlpha(int alpha) {
    paint.setAlpha(alpha);
    invalidateSelf();
  }

  @Override
  public void setColorFilter(ColorFilter filter) {
    paint.setColorFilter(filter);
    invalidateSelf();
  }

  @Override
  public int getOpacity() {
    return PixelFormat.TRANSLUCENT;
  }
}
