package art.velyntora.core;

import android.graphics.*;

/** Tool-local vector icon, rendered by the shared 24 dp drawable. */
final class PickerIcon extends ToolIcon {
  @Override
  void geometry(Canvas c) {
    line(c, 4, 20, 6, 14, 15, 5, 19, 9, 10, 18, 4, 20);
    line(c, 13, 3, 21, 11);
    c.drawCircle(19, 5, 3, p);
  }
}
