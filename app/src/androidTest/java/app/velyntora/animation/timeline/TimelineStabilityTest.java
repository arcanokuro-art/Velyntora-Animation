package app.velyntora.animation.timeline;

import static org.junit.Assert.*;
import android.content.Context;
import android.graphics.*;
import android.view.View;
import android.widget.HorizontalScrollView;
import androidx.test.platform.app.InstrumentationRegistry;
import app.velyntora.animation.model.AnimationSequence;
import app.velyntora.animation.preview.FramePreviewCache;
import app.velyntora.animation.storage.AnimationProject;
import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import org.junit.Test;

public final class TimelineStabilityTest {
  @Test public void hardwareRecordingLoadsOnlyViewportAndKeepsVisibleThumbnails() throws Exception {
    android.app.Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
    Context context = instrumentation.getTargetContext();
    File folder = new File(context.getCacheDir(), "strip-stability-" + AnimationSequence.uuid());
    AnimationProject project = new AnimationProject(folder, "3233 fotogramas", 1280, 720, 8);
    ArrayList<AnimationSequence.Frame> frames = new ArrayList<>();
    Bitmap source = Bitmap.createBitmap(128, 72, Bitmap.Config.ARGB_8888);
    source.eraseColor(Color.rgb(190, 40, 80));
    for (int n = 0; n < 3233; n++) {
      String asset = AnimationSequence.uuid();
      frames.add(new AnimationSequence.Frame(AnimationSequence.uuid(), asset, 1));
      if (n < 40 || n >= 3193)
        try (FileOutputStream out = new FileOutputStream(project.previewFile(asset))) {
          assertTrue(source.compress(Bitmap.CompressFormat.PNG, 100, out));
        }
    }
    source.recycle();
    project.sequence.replace(frames, 0, 8);
    FramePreviewCache cache = new FramePreviewCache(1024 * 1024);
    TimelineStrip[] strip = new TimelineStrip[1];
    HorizontalScrollView[] scroll = new HorizontalScrollView[1];
    Field pending = FramePreviewCache.class.getDeclaredField("pending");
    pending.setAccessible(true);
    try {
      instrumentation.runOnMainSync(() -> {
        scroll[0] = new HorizontalScrollView(context);
        strip[0] = new TimelineStrip(context, cache, n -> {}, () -> {});
        scroll[0].addView(strip[0]);
        strip[0].bind(project);
        scroll[0].measure(View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.EXACTLY));
        scroll[0].layout(0, 0, 600, 300);
        strip[0].setViewport(0, 600);
        recordWholeChild(strip[0]);
        try {
          assertTrue("Offscreen frames filled the decode queue",
              ((Map<?, ?>) pending.get(cache)).size() <= 600 / strip[0].cell + 3);
        } catch (IllegalAccessException error) { throw new AssertionError(error); }
      });
      String first = project.sequence.frame(0).asset;
      long deadline = android.os.SystemClock.uptimeMillis() + 5000;
      while (cache.get(first, 128) == null && android.os.SystemClock.uptimeMillis() < deadline)
        Thread.sleep(20);
      assertNotNull("First thumbnail never loaded", cache.get(first, 128));
      Bitmap original = cache.get(first, 128);
      instrumentation.runOnMainSync(() -> {
        for (int n = 0; n < 80; n++) recordWholeChild(strip[0]);
        assertSame("Repeated redraws evicted the visible thumbnail", original, cache.get(first, 128));
        Bitmap screen = Bitmap.createBitmap(600, strip[0].getHeight(), Bitmap.Config.ARGB_8888);
        strip[0].draw(new Canvas(screen));
        int x = strip[0].cell / 2;
        int y = Math.round(57 * context.getResources().getDisplayMetrics().density);
        assertEquals(Color.rgb(190, 40, 80), screen.getPixel(x, y));
        screen.recycle();
        assertNull("Invisible frame was decoded", cache.get(project.sequence.frame(30).asset, 128));
        int left = 3220 * strip[0].cell;
        scroll[0].scrollTo(left, 0);
        strip[0].setViewport(scroll[0].getScrollX(), 600);
        recordWholeChild(strip[0]);
        try {
          assertTrue("Scrubbing queued the entire sequence",
              ((Map<?, ?>) pending.get(cache)).size() <= 20);
        } catch (IllegalAccessException error) { throw new AssertionError(error); }
      });
    } finally {
      instrumentation.runOnMainSync(cache::close);
      File[] files = new File(folder, "objects").listFiles();
      if (files != null) for (File file : files) file.delete();
      new File(folder, "objects").delete();
      folder.delete();
    }
  }

  private static void recordWholeChild(TimelineStrip strip) {
    Picture displayList = new Picture();
    // Reproduce the unconstrained clip used while Android records a large child display list.
    Canvas canvas = displayList.beginRecording(strip.getWidth(), strip.getHeight());
    strip.draw(canvas);
    displayList.endRecording();
  }
}
