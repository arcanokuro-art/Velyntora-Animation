package art.velyntora.core;

import android.graphics.*;

/** Tool-local vector icon, rendered by the shared 24 dp drawable. */
final class MovePixelsIcon extends ToolIcon {
  @Override
  void geometry(Canvas c) {
    move(c, 9);
  }
}
