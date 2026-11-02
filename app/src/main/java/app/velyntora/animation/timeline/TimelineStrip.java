package app.velyntora.animation.timeline;

import android.content.Context;
import android.graphics.*;
import android.view.*;
import app.velyntora.animation.preview.FramePreviewCache;
import app.velyntora.animation.storage.AnimationProject;
import java.util.function.IntConsumer;

/** Draws only visible cells, so long sequences do not allocate a View per frame. */
final class TimelineStrip extends View {
  private final FramePreviewCache cache;
  private final IntConsumer select;
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
  private AnimationProject project;
  private app.velyntora.animation.model.AnimationSequence sequence;
  private int generation;
  final int cell;

  TimelineStrip(Context context, FramePreviewCache cache, IntConsumer select) {
    super(context);
    this.cache = cache;
    this.select = select;
    cell = dp(76);
    setContentDescription("Línea de tiempo de fotogramas");
    setFocusable(true);
  }

  private int dp(int n) {
    return Math.round(n * getResources().getDisplayMetrics().density);
  }

  void bind(AnimationProject project) {
    this.project = project;
    this.sequence = project == null ? null : project.sequence.copy();
    generation++;
    requestLayout();
    invalidate();
  }

  protected void onMeasure(int width, int height) {
    setMeasuredDimension(cell * (project == null ? 1 : sequence.size()), dp(70));
  }

  protected void onDraw(Canvas canvas) {
    if (project == null) return;
    Rect clip = canvas.getClipBounds();
    int first = Math.max(0, clip.left / cell),
        last = Math.min(sequence.size(), clip.right / cell + 1);
    int token = generation;
    for (int i = first; i < last; i++) {
      float x = i * cell;
      boolean active = i == sequence.selected();
      paint.setColor(active ? 0xff237ef4 : 0xff4b4e55);
      canvas.drawRoundRect(
          x + dp(2), dp(2), x + cell - dp(2), getHeight() - dp(2), dp(4), dp(4), paint);
      String asset = sequence.frame(i).asset;
      if (asset != null) {
        Bitmap thumbnail = cache.get(asset, 128);
        if (thumbnail != null)
          canvas.drawBitmap(
              thumbnail, null, new RectF(x + dp(6), dp(6), x + cell - dp(6), dp(48)), paint);
        else
          cache.load(
              asset,
              project.previewFile(asset),
              128,
              image -> {
                if (token == generation && image != null) invalidate();
              });
      }
      paint.setColor(Color.WHITE);
      paint.setTextSize(dp(12));
      canvas.drawText((i + 1) + " · " + sequence.frame(i).exposure, x + dp(6), dp(63), paint);
    }
  }

  public boolean onTouchEvent(MotionEvent e) {
    if (project == null) return false;
    if (e.getActionMasked() == MotionEvent.ACTION_UP) {
      int index = (int) e.getX() / cell;
      if (index >= 0 && index < sequence.size()) {
        performClick();
        select.accept(index);
      }
    }
    return true;
  }

  public boolean performClick() {
    super.performClick();
    return true;
  }
}
