package art.velyntora.core;

import android.graphics.*;

/** Tool-local vector icon, rendered by the shared 24 dp drawable. */
final class PanIcon extends ToolIcon {
  @Override
  void geometry(Canvas c) {
    line(
        c, 4, 14, 7, 17, 7, 7, 10, 7, 10, 4, 13, 4, 13, 7, 16, 7, 16, 10, 19, 10, 19, 18, 16, 21,
        10, 21, 4, 14);
  }
}
