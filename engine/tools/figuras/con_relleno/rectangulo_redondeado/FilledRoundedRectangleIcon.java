package art.velyntora.core;

import android.graphics.*;

/** Tool-local vector icon, rendered by the shared 24 dp drawable. */
final class FilledRoundedRectangleIcon extends ToolIcon {
  @Override
  void geometry(Canvas c) {
    c.drawRoundRect(3, 3, 21, 21, 5, 5, p);
    p.setStyle(Paint.Style.FILL);
    c.drawCircle(19, 20, 3, p);
    p.setStyle(Paint.Style.STROKE);
  }
}
