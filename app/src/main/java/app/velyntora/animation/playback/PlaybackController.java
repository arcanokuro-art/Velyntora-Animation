package app.velyntora.animation.playback;

import android.graphics.Bitmap;
import android.os.*;
import app.velyntora.animation.preview.FramePreviewCache;
import app.velyntora.animation.storage.AnimationProject;

/** Playback reads immutable proxies; it never swaps or edits the native document. */
public final class PlaybackController {
  public interface Listener {
    void show(Bitmap image, int frame);
  }

  private final Handler timer = new Handler(Looper.getMainLooper());
  private final FramePreviewCache cache;
  private AnimationProject project;
  private PlaybackClock clock;
  private Listener listener;
  private int generation, displayed = -1, requested = -1;
  private boolean running, loading;

  public PlaybackController(FramePreviewCache cache) {
    this.cache = cache;
  }

  public boolean running() {
    return running;
  }

  public void start(AnimationProject project, Listener listener) {
    stop();
    this.project = project;
    this.listener = listener;
    clock = new PlaybackClock(project.sequence, SystemClock.uptimeMillis());
    running = true;
    timer.post(tick);
  }

  private final Runnable tick =
      new Runnable() {
        public void run() {
          if (!running) return;
          int index = clock.frame(SystemClock.uptimeMillis());
          if (index != displayed && !loading) {
            loading = true;
            requested = index;
            int token = generation;
            String asset = project.sequence.frame(index).asset;
            cache.load(
                asset,
                project.displayFile(asset,1024),
                1024,
                bitmap -> {
                  if (!running || token != generation) return;
                  requested = -1;
                  loading = false;
                  if (bitmap != null) {
                    displayed = index;
                    listener.show(bitmap, index);
                  }
                });
          }
          timer.postDelayed(this, 16);
        }
      };

  public void stop() {
    running = false;
    loading = false;
    generation++;
    displayed = requested = -1;
    timer.removeCallbacks(tick);
  }
}
