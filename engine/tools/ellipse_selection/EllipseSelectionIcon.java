package art.velyntora.core;

import android.graphics.*;

/** Tool-local vector icon, rendered by the shared 24 dp drawable. */
final class EllipseSelectionIcon extends ToolIcon {
  @Override
  void geometry(Canvas c) {
    p.setPathEffect(new DashPathEffect(new float[] {1, 3}, 0));
    c.drawOval(3, 3, 21, 21, p);
    p.setPathEffect(null);
  }
}
