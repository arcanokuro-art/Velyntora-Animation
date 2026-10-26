package art.velyntora.core;

import android.graphics.*;

/** Tool-local vector icon, rendered by the shared 24 dp drawable. */
final class TextIcon extends ToolIcon {
  @Override
  void geometry(Canvas c) {
    p.setTypeface(Typeface.DEFAULT_BOLD);
    p.setTextSize(21);
    p.setStyle(Paint.Style.FILL);
    c.drawText("T", 2, 20, p);
    p.setTextSize(15);
    c.drawText("T", 13, 20, p);
    p.setStyle(Paint.Style.STROKE);
  }
}
