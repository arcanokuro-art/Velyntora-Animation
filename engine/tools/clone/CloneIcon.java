package art.velyntora.core;

import android.graphics.*;

/** Tool-local vector icon, rendered by the shared 24 dp drawable. */
final class CloneIcon extends ToolIcon {
  @Override
  void geometry(Canvas c) {
    c.drawCircle(12, 7, 4, p);
    line(c, 9, 11, 7, 17, 17, 17, 15, 11);
    c.drawRect(4, 18, 20, 22, p);
  }
}
