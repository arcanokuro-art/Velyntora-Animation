package art.velyntora.core;

import android.graphics.*;
import android.view.MotionEvent;

/** Gesture controller for text. Shared document state belongs to DrawingView. */
final class TextTool implements DrawingTool {
  public void down(DrawingView v, float x, float y, MotionEvent event) {
    v.drawing = false;
    if (v.textPositionListener != null) v.textPositionListener.accept((int) x, (int) y);
  }

  static boolean insertText(
      DrawingView v,
      String text,
      int x,
      int y,
      int size,
      boolean bold,
      boolean italic,
      String family) {
    if (v.brushOpacity <= 0 || text == null
        || text.trim().isEmpty()
        || text.length() > 4096
        || x < 0
        || y < 0
        || x >= v.canvasWidth
        || y >= v.canvasHeight) return false;
    android.text.TextPaint textPaint = new android.text.TextPaint(Paint.ANTI_ALIAS_FLAG);
    textPaint.setColor(v.color);
    textPaint.setAlpha(Math.round((v.color >>> 24) * v.brushOpacity));
    textPaint.setTextSize(Math.max(4, Math.min(256, size)));
    textPaint.setTypeface(
        android.graphics.Typeface.create(
            family,
            (bold ? android.graphics.Typeface.BOLD : 0)
                | (italic ? android.graphics.Typeface.ITALIC : 0)));
    int width = v.canvasWidth - x;
    android.text.StaticLayout layout =
        android.text.StaticLayout.Builder.obtain(text, 0, text.length(), textPaint, width)
            .setIncludePad(true)
            .build();
    int height = Math.min(v.canvasHeight - y, layout.getHeight());
    if (height <= 0) return false;
    Bitmap glyphs = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    try {
      layout.draw(new Canvas(glyphs));
      return v.pasteAt(glyphs, x, y);
    } finally {
      glyphs.recycle();
    }
  }
}
