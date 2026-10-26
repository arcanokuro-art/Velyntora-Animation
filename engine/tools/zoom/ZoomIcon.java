package art.velyntora.core;

import android.graphics.*;

/** Tool-local vector icon, rendered by the shared 24 dp drawable. */
final class ZoomIcon extends ToolIcon {
  @Override
  void geometry(Canvas c) {
    c.drawCircle(10, 10, 6, p);
    line(c, 15, 15, 21, 21);
  }
}
