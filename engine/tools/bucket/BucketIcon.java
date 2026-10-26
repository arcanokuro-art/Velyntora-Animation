package art.velyntora.core;

import android.graphics.*;

/** Tool-local vector icon, rendered by the shared 24 dp drawable. */
final class BucketIcon extends ToolIcon {
  @Override
  void geometry(Canvas c) {
    line(c, 3, 10, 11, 2, 20, 11, 12, 19, 3, 10);
    line(c, 3, 11, 20, 11);
    line(c, 7, 2, 15, 10);
    line(c, 3, 22, 21, 22);
    c.drawCircle(21, 16, 1, p);
  }
}
