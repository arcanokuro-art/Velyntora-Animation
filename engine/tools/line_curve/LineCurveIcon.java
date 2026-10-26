package art.velyntora.core;

import android.graphics.*;

/** Tool-local vector icon, rendered by the shared 24 dp drawable. */
final class LineCurveIcon extends ToolIcon {
  @Override
  void geometry(Canvas c) {
    line(c, 5, 18, 9, 9, 18, 5);
    node(c, 5, 18);
    node(c, 9, 9);
    node(c, 18, 5);
  }
}
