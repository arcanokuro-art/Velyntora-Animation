package art.velyntora.core;

import android.graphics.*;

/** Tool-local vector icon, rendered by the shared 24 dp drawable. */
final class PencilIcon extends ToolIcon {
  @Override
  void geometry(Canvas c) {
    line(c, 3, 21, 5, 15, 17, 3, 21, 7, 9, 19, 3, 21);
    line(c, 15, 5, 19, 9);
  }
}
