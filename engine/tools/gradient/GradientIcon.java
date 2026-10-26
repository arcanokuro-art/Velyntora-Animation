package art.velyntora.core;

import android.graphics.*;

/** Tool-local vector icon, rendered by the shared 24 dp drawable. */
final class GradientIcon extends ToolIcon {
  @Override
  void geometry(Canvas c) {
    for (int i = 0; i < 6; i++) {
      p.setAlpha(40 + i * 40);
      p.setStrokeWidth(3);
      line(c, 3 + i * 3.5f, 3, 3 + i * 3.5f, 21);
    }
    p.setAlpha(255);
    p.setStrokeWidth(1.8f);
  }
}
