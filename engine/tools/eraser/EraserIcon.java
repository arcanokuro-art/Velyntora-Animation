package art.velyntora.core;

import android.graphics.*;

/** Tool-local vector icon, rendered by the shared 24 dp drawable. */
final class EraserIcon extends ToolIcon {
  @Override
  void geometry(Canvas c) {
    line(c, 3, 14, 14, 3, 21, 10, 10, 21, 7, 21, 3, 17, 3, 14);
    line(c, 7, 10, 15, 18);
  }
}
