package art.velyntora.core;

import android.graphics.*;

/** Tool-local vector icon, rendered by the shared 24 dp drawable. */
final class EllipseIcon extends ToolIcon {
  @Override
  void geometry(Canvas c) {
    c.drawOval(3, 3, 21, 21, p);
  }
}
