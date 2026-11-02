package app.velyntora.animation.playback;

import static org.junit.Assert.*;
import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import androidx.test.platform.app.InstrumentationRegistry;
import app.velyntora.animation.model.AnimationSequence;
import app.velyntora.animation.preview.FramePreviewCache;
import app.velyntora.animation.storage.AnimationProject;
import java.io.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

public final class PlaybackStabilityTest {
  @Test public void largeImportedProjectPlaysContinuouslyAndStopsWithoutLateCallbacks() throws Exception {
    android.app.Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
    Context context = instrumentation.getTargetContext();
    File folder = new File(context.getCacheDir(), "playback-stability-" + AnimationSequence.uuid());
    AnimationProject project = new AnimationProject(folder, "3233 fotogramas", 1280, 720, 30);
    ArrayList<String> assets = new ArrayList<>();
    Bitmap source = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(source);
    Paint paint = new Paint();
    for (int n = 0; n < 40; n++) {
      String asset = AnimationSequence.uuid();
      assets.add(asset);
      source.eraseColor(Color.rgb(20 + n * 4, 80, 150));
      paint.setColor(Color.WHITE);
      canvas.drawRect(n * 25, 20, n * 25 + 10, 700, paint);
      try (FileOutputStream out = new FileOutputStream(project.rasterFile(asset))) {
        assertTrue(source.compress(Bitmap.CompressFormat.PNG, 100, out));
      }
    }
    source.recycle();
    ArrayList<AnimationSequence.Frame> frames = new ArrayList<>();
    for (int n = 0; n < 3233; n++)
      frames.add(new AnimationSequence.Frame(AnimationSequence.uuid(), assets.get(n % assets.size()), 1));
    project.sequence.replace(frames, 0, 30);
    FramePreviewCache cache = new FramePreviewCache(24 * 1024 * 1024);
    PlaybackController controller = new PlaybackController(cache);
    ArrayList<Integer> shown = new ArrayList<>();
    ArrayList<Long> times = new ArrayList<>();
    CountDownLatch started = new CountDownLatch(1);
    try {
      instrumentation.runOnMainSync(() -> controller.start(project, (bitmap, frame) -> {
        assertNotNull(bitmap);
        assertTrue(bitmap.getWidth() >= 640);
        shown.add(frame);
        times.add(SystemClock.uptimeMillis());
        started.countDown();
      }));
      assertTrue("First frame was blocked", started.await(5, TimeUnit.SECONDS));
      Thread.sleep(3000);
      instrumentation.runOnMainSync(() -> {
        controller.stop();
        assertTrue("Playback stalled: " + shown, shown.size() >= 30);
        long gap = 0;
        for (int n = 1; n < times.size(); n++)
          gap = Math.max(gap, times.get(n) - times.get(n - 1));
        assertTrue("Playback froze for " + gap + "ms", gap < 750);
        assertTrue("Clock failed to advance", shown.get(shown.size() - 1) >= 60);
        android.util.Log.i("ANIMATION_STABILITY", "frames=" + shown.size() + " max_gap_ms=" + gap);
      });
      int count = shown.size();
      Thread.sleep(200);
      instrumentation.runOnMainSync(() -> assertEquals("Late callback after stop", count, shown.size()));
    } finally {
      instrumentation.runOnMainSync(() -> { controller.stop(); cache.close(); });
      File[] files = new File(folder, "objects").listFiles();
      if (files != null) for (File file : files) file.delete();
      new File(folder, "objects").delete();
      folder.delete();
    }
  }
}
