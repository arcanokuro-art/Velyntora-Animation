package app.velyntora.animation.playback;

import android.graphics.Bitmap;
import android.os.*;
import app.velyntora.animation.model.AnimationSequence;
import app.velyntora.animation.preview.FramePreviewCache;
import app.velyntora.animation.storage.AnimationProject;
import java.util.HashSet;

/** Independent bounded read-ahead; playback never swaps the editable native document. */
public final class PlaybackController {
  public interface Listener { void show(Bitmap image, int frame); }
  private static final int SIDE = 1024, LOOK_AHEAD = 4;
  private final Handler timer = new Handler(Looper.getMainLooper());
  private final FramePreviewCache cache;
  private final HashSet<String> pending = new HashSet<>();
  private AnimationProject project;
  private AnimationSequence sequence;
  private PlaybackClock clock;
  private Listener listener;
  private int generation, displayed = -1;
  private boolean running;

  public PlaybackController(FramePreviewCache cache) { this.cache = cache; }
  public boolean running() { return running; }

  public void start(AnimationProject project, Listener listener) {
    stop();
    this.project = project;
    this.sequence = project.sequence.copy();
    this.listener = listener;
    running = true;
    int token = generation;
    int first = sequence.selected();
    request(first, () -> {
      if (!running || generation != token) return;
      // Start the clock after the first image is ready, not while waiting for disk.
      clock = new PlaybackClock(sequence, SystemClock.uptimeMillis());
      show(first);
      prefetch(first);
      timer.post(tick);
    });
  }

  private void show(int index) {
    Bitmap image = cache.get(sequence.frame(index).asset, SIDE);
    if (image != null && displayed != index) {
      displayed = index;
      listener.show(image, index);
    }
  }

  private void request(int index, Runnable ready) {
    String asset = sequence.frame(index).asset;
    if (!pending.add(asset)) return;
    int token = generation;
    cache.load(asset, project.displayFile(asset, SIDE), SIDE, image -> {
      if (!running || generation != token) return;
      pending.remove(asset);
      if (ready != null) ready.run();
    });
  }

  private void prefetch(int index) {
    // At most the current image plus four upcoming images, never the entire project.
    for (int n = 1; n <= Math.min(LOOK_AHEAD, sequence.size() - 1); n++) {
      int next = (index + n) % sequence.size();
      String asset = sequence.frame(next).asset;
      if (cache.get(asset, SIDE) == null && pending.size() < LOOK_AHEAD + 1)
        request(next, null);
    }
  }

  private final Runnable tick = new Runnable() {
    public void run() {
      if (!running || clock == null) return;
      int index = clock.frame(SystemClock.uptimeMillis());
      show(index);
      if (displayed != index && pending.size() < LOOK_AHEAD + 1)
        request(index, null);
      prefetch(index);
      // Keep the last good bitmap on screen while a miss is decoded.
      timer.postDelayed(this, 16);
    }
  };

  public void stop() {
    running = false;
    generation++;
    displayed = -1;
    clock = null;
    pending.clear();
    timer.removeCallbacks(tick);
  }
}
