package art.velyntora.core;

import android.graphics.*;

/** Tool-local vector icon, rendered by the shared 24 dp drawable. */
final class LassoSelectionIcon extends ToolIcon {
  @Override
  void geometry(Canvas c) {
    c.drawOval(3, 4, 21, 14, p);
    line(c, 15, 12, 20, 18, 15, 21, 5, 21);
  }
}
