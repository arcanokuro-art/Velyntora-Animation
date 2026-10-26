package art.velyntora.core;

import android.graphics.*;

/** Tool-local vector icon, rendered by the shared 24 dp drawable. */
final class BrushIcon extends ToolIcon {
  @Override
  void geometry(Canvas c) {
    line(c, 8, 15, 18, 3, 21, 6, 11, 17);
    c.drawOval(3, 15, 11, 21, p);
  }
}
