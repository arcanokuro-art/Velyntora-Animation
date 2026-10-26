package art.velyntora.core;

import android.graphics.*;

/** Tool-local vector icon, rendered by the shared 24 dp drawable. */
final class MagicWandIcon extends ToolIcon {
  @Override
  void geometry(Canvas c) {
    line(c, 3, 21, 16, 8, 19, 11, 6, 24);
    line(c, 5, 3, 5, 8);
    line(c, 2, 5, 8, 5);
    line(c, 19, 2, 19, 6);
    line(c, 17, 4, 21, 4);
  }
}
