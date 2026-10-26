package art.velyntora.core;

import android.graphics.*;

/** Tool-local vector icon, rendered by the shared 24 dp drawable. */
final class RecolorIcon extends ToolIcon {
  @Override
  void geometry(Canvas c) {
    c.drawCircle(8, 15, 5, p);
    c.drawCircle(17, 6, 3, p);
    line(c, 15, 12, 20, 17, 15, 17);
  }
}
