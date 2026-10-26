package art.velyntora.core;

import android.graphics.*;

/** Tool-local vector icon, rendered by the shared 24 dp drawable. */
final class FilledTriangleIcon extends ToolIcon {
  @Override
  void geometry(Canvas c) {
    line(c, 12, 3, 22, 21, 2, 21, 12, 3);
    p.setStyle(Paint.Style.FILL);
    c.drawCircle(19, 20, 3, p);
    p.setStyle(Paint.Style.STROKE);
  }
}
