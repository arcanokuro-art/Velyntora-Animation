package art.velyntora.core;

import android.graphics.*;

/** Tool-local vector icon, rendered by the shared 24 dp drawable. */
final class FreeformIcon extends ToolIcon {
  @Override
  void geometry(Canvas c) {
    line(c, 3, 21, 10, 13, 14, 4, 21, 4, 21, 21, 3, 21);
  }
}
