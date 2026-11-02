package app.velyntora.animation.timeline;

import android.content.Context;
import android.graphics.*;
import android.view.*;
import app.velyntora.animation.preview.FramePreviewCache;
import app.velyntora.animation.storage.AnimationProject;
import java.util.function.IntConsumer;

/** Virtualized filmstrip backed by an immutable sequence snapshot. */
final class TimelineStrip extends View {
  private final FramePreviewCache cache;
  private final IntConsumer select;
  private final Runnable add;
  private final Paint paint = new Paint(3);
  private AnimationProject project;
  private app.velyntora.animation.model.AnimationSequence sequence;
  private int generation, active;
  int cell;
  private static final int MAX_STRIP_WIDTH = 16000000;

  TimelineStrip(Context c, FramePreviewCache cache, IntConsumer select, Runnable add) {
    super(c);
    this.cache = cache;
    this.select = select;
    this.add = add;
    cell = dp(100);
    setContentDescription("Línea de tiempo de fotogramas; toca una miniatura para seleccionar");
    setFocusable(true);
  }

  private int dp(int n) {
    return Math.round(n * getResources().getDisplayMetrics().density);
  }

  void bind(AnimationProject project) {
    this.project = project;
    sequence = project == null ? null : project.sequence.copy();
    active = sequence == null ? 0 : sequence.selected();
    // View measurement encodes its size in 24 bits. Keep every frame reachable at high density.
    cell = sequence == null ? dp(100) : Math.max(1,Math.min(dp(100),MAX_STRIP_WIDTH/(sequence.size()+1)));
    generation++;
    requestLayout();
    invalidate();
  }

  void setPlaybackFrame(int frame) {
    active = frame;
    invalidate();
  }

  protected void onMeasure(int w, int h) {
    setMeasuredDimension(cell * (sequence == null ? 1 : sequence.size() + 1), dp(94));
  }

  protected void onDraw(Canvas c) {
    if (sequence == null) return;
    Rect clip = c.getClipBounds();
    int first = Math.max(0, clip.left / cell),
        last = Math.min(sequence.size() + 1, clip.right / cell + 1),
        token = generation;
    for (int i = first; i < last; i++) {
      float x = i * cell;
      RectF tile = new RectF(x + dp(4), dp(24), x + cell - dp(4), dp(91));
      paint.setStyle(Paint.Style.FILL);
      paint.setTextAlign(Paint.Align.CENTER);
      paint.setTextSize(dp(11));
      paint.setColor(0xffd7dde2);
      if (i == sequence.size()) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1));
        paint.setPathEffect(new DashPathEffect(new float[] {dp(5), dp(4)}, 0));
        c.drawRoundRect(tile, dp(5), dp(5), paint);
        paint.setPathEffect(null);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(dp(26));
        c.drawText("+", x + cell / 2f, dp(52), paint);
        paint.setTextSize(dp(10));
        c.drawText("Añadir", x + cell / 2f, dp(69), paint);
        c.drawText("fotograma", x + cell / 2f, dp(82), paint);
        continue;
      }
      c.drawText(String.valueOf(i + 1), x + cell / 2f, dp(16), paint);
      paint.setColor(Color.WHITE);
      c.drawRoundRect(tile, dp(4), dp(4), paint);
      String asset = sequence.frame(i).asset;
      if (asset != null) {
        Bitmap image = cache.get(asset, 128);
        if (image != null) {
          float scale =
              Math.min(
                  (tile.width() - dp(4)) / image.getWidth(),
                  (tile.height() - dp(4)) / image.getHeight());
          float w = image.getWidth() * scale, h = image.getHeight() * scale;
          c.drawBitmap(
              image,
              null,
              new RectF(
                  tile.centerX() - w / 2,
                  tile.centerY() - h / 2,
                  tile.centerX() + w / 2,
                  tile.centerY() + h / 2),
              paint);
        } else
          cache.load(
              asset,
              project.previewFile(asset),
              128,
              imageLoaded -> {
                if (token == generation && imageLoaded != null) invalidate();
              });
      }
      paint.setStyle(Paint.Style.STROKE);
      paint.setStrokeWidth(dp(i == active ? 3 : 1));
      paint.setColor(i == active ? 0xffff269d : 0xff424b53);
      c.drawRoundRect(tile, dp(4), dp(4), paint);
    }
    paint.setStyle(Paint.Style.FILL);
  }

  public boolean onTouchEvent(MotionEvent e) {
    if (sequence == null) return false;
    if (e.getActionMasked() == MotionEvent.ACTION_UP) {
      int index = (int) e.getX() / cell;
      performClick();
      if (index == sequence.size()) add.run();
      else if (index >= 0 && index < sequence.size()) select.accept(index);
    }
    return true;
  }

  public boolean performClick() {
    super.performClick();
    return true;
  }
}
