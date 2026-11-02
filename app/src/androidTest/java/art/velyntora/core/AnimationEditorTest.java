package art.velyntora.core;

import static org.junit.Assert.*;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.view.*;
import android.widget.*;
import androidx.test.platform.app.InstrumentationRegistry;
import app.velyntora.animation.storage.AnimationProject;
import java.io.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.zip.*;
import org.junit.Test;

/** Real native documents and real archive streams, with deterministic async waits. */
public final class AnimationEditorTest {
  private final Instrumentation i = InstrumentationRegistry.getInstrumentation();

  private View find(View root, Class<?> type) {
    if (type.isInstance(root)) return root;
    if (root instanceof ViewGroup) {
      ViewGroup group = (ViewGroup) root;
      for (int n = 0; n < group.getChildCount(); n++) {
        View found = find(group.getChildAt(n), type);
        if (found != null) return found;
      }
    }
    return null;
  }

  private Button button(View root, String label) {
    if (root instanceof Button
        && (label.contentEquals(((Button) root).getText())
            || (root.getContentDescription() != null
                && label.contentEquals(root.getContentDescription())))) return (Button) root;
    if (root instanceof ViewGroup) {
      ViewGroup group = (ViewGroup) root;
      for (int n = 0; n < group.getChildCount(); n++) {
        Button found = button(group.getChildAt(n), label);
        if (found != null) return found;
      }
    }
    return null;
  }

  private boolean busy(AnimationActivity host) {
    try {
      java.lang.reflect.Field f = AnimationActivity.class.getDeclaredField("busy");
      f.setAccessible(true);
      return f.getBoolean(host);
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  private AnimationProject project(AnimationActivity host) {
    try {
      java.lang.reflect.Field f = AnimationActivity.class.getDeclaredField("session");
      f.setAccessible(true);
      return ((AnimationSession) f.get(host)).project();
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  private void await(BooleanSupplier condition) {
    long deadline = SystemClock.uptimeMillis() + 20000;
    AtomicBoolean ready = new AtomicBoolean();
    do {
      i.runOnMainSync(() -> ready.set(condition.getAsBoolean()));
      if (ready.get()) return;
      SystemClock.sleep(30);
    } while (SystemClock.uptimeMillis() < deadline);
    throw new AssertionError("Timed out waiting for animation editor");
  }

  private void idle(AnimationActivity host, int count, int selected) {
    await(
        () ->
            !busy(host)
                && project(host) != null
                && project(host).sequence.size() == count
                && project(host).sequence.selected() == selected);
  }

  private DrawingView canvas(AnimationActivity host) {
    return (DrawingView) find(host.getWindow().getDecorView(), DrawingView.class);
  }

  private void click(AnimationActivity host, String label) {
    i.runOnMainSync(
        () -> {
          Button b = button(host.getWindow().getDecorView(), label);
          assertNotNull(label, b);
          b.performClick();
        });
  }

  private void shell(String command) throws IOException {
    try (ParcelFileDescriptor fd = i.getUiAutomation().executeShellCommand(command);
        InputStream out = new ParcelFileDescriptor.AutoCloseInputStream(fd)) {
      byte[] buffer = new byte[1024];
      while (out.read(buffer) != -1) {}
    }
  }

  private Uri output(String name) throws IOException {
    ContentValues values = new ContentValues();
    values.put(MediaStore.Downloads.DISPLAY_NAME, name);
    values.put(MediaStore.Downloads.MIME_TYPE, "application/zip");
    Uri uri =
        i.getTargetContext()
            .getContentResolver()
            .insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
    if (uri == null) throw new IOException("No output URI");
    return uri;
  }

  private AnimationActivity launch(Uri uri) {
    Intent intent =
        new Intent(i.getTargetContext(), AnimationActivity.class)
            .setAction(AnimationActivity.ACTION)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra("title", "Prueba con capas")
            .putExtra("width", 96)
            .putExtra("height", 64)
            .putExtra("fps", 12);
    if (uri != null) intent.setData(uri);
    return (AnimationActivity) i.startActivitySync(intent);
  }

  @Test
  public void layeredFramesPlaybackArchiveRoundTripAndFullResolutionPng() throws Exception {
    AnimationActivity host = launch(null);
    Uri archive = null, png = null;
    AnimationActivity restored = null;
    try {
      idle(host, 1, 0);
      i.runOnMainSync(
          () -> {
            assertEquals(96, canvas(host).documentWidth());
            assertEquals(64, canvas(host).documentHeight());
            assertTrue(canvas(host).addLayer());
            assertEquals(2, canvas(host).layerCount());
          });
      click(host, "Duplicar");
      idle(host, 2, 1);
      i.runOnMainSync(() -> assertEquals(2, canvas(host).layerCount()));
      click(host, "+ Fotograma");
      idle(host, 3, 2);
      i.runOnMainSync(() -> assertEquals(1, canvas(host).layerCount()));
      i.runOnMainSync(() -> host.select(0));
      idle(host, 3, 0);
      i.runOnMainSync(() -> assertEquals(2, canvas(host).layerCount()));
      i.runOnMainSync(() -> host.move(1));
      idle(host, 3, 1);
      i.runOnMainSync(() -> assertEquals(2, canvas(host).layerCount()));
      i.runOnMainSync(
          () -> assertFalse(button(host.getWindow().getDecorView(), "Cebolla: no").isSelected()));
      click(host, "Cebolla: no");
      i.runOnMainSync(
          () -> assertTrue(button(host.getWindow().getDecorView(), "Cebolla: sí").isSelected()));
      click(host, "Reproducir");
      await(() -> button(host.getWindow().getDecorView(), "Pausar") != null);
      click(host, "Pausar");
      idle(host, 3, 1);
      archive = output("integration.vlyanim");
      final Uri archiveUri = archive;
      i.runOnMainSync(
          () ->
              host.onActivityResult(
                  AnimationActivity.SAVE, Activity.RESULT_OK, new Intent().setData(archiveUri)));
      idle(host, 3, 1);
      try (InputStream in = i.getTargetContext().getContentResolver().openInputStream(archive)) {
        AnimationProject copy =
            new AnimationProject(
                new File(
                    i.getTargetContext().getCacheDir(), java.util.UUID.randomUUID().toString()),
                "x",
                1,
                1,
                1);
        copy.importArchive(in);
        assertEquals(3, copy.sequence.size());
        assertEquals(12, copy.sequence.fps());
      }
      i.runOnMainSync(host::onBackPressed);
      await(host::isDestroyed);
      restored = launch(archive);
      final AnimationActivity reopened = restored;
      idle(reopened, 3, 1);
      i.runOnMainSync(() -> assertEquals(2, canvas(reopened).layerCount()));
      png = output("integration-frames.zip");
      final Uri pngUri = png;
      i.runOnMainSync(
          () ->
              reopened.onActivityResult(
                  AnimationActivity.EXPORT, Activity.RESULT_OK, new Intent().setData(pngUri)));
      idle(reopened, 3, 1);
      int frames = 0;
      try (ZipInputStream zip =
          new ZipInputStream(i.getTargetContext().getContentResolver().openInputStream(png))) {
        ZipEntry entry;
        while ((entry = zip.getNextEntry()) != null) {
          if (!entry.getName().endsWith(".png")) continue;
          byte[] header = new byte[24];
          new DataInputStream(zip).readFully(header);
          assertEquals((byte) 137, header[0]);
          assertEquals(96, java.nio.ByteBuffer.wrap(header, 16, 4).getInt());
          assertEquals(64, java.nio.ByteBuffer.wrap(header, 20, 4).getInt());
          frames++;
        }
      }
      assertEquals(3, frames);
      i.runOnMainSync(() -> assertEquals(2, canvas(reopened).layerCount()));
      i.runOnMainSync(
          () ->
              reopened.setRequestedOrientation(
                  android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));
      await(
          () ->
              reopened.getResources().getConfiguration().orientation
                  == android.content.res.Configuration.ORIENTATION_LANDSCAPE);
      i.waitForIdleSync();
      SystemClock.sleep(2500); // Let the display rotation and saved-file toast finish.
      shell("screencap -p /sdcard/Download/animation-editor-phone.png");
      // Validate the wide tablet layout used by the design reference as well as the phone layout.
      i.runOnMainSync(reopened::onBackPressed);
      await(reopened::isDestroyed);
      shell("wm size 1700x2600");
      restored = launch(archive);
      final AnimationActivity tablet = restored;
      idle(tablet, 3, 1);
      i.runOnMainSync(
          () ->
              tablet.setRequestedOrientation(
                  android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));
      await(
          () ->
              tablet.getWindow().getDecorView().getWidth()
                      > tablet.getWindow().getDecorView().getHeight()
                  && tablet.getResources().getConfiguration().screenWidthDp > 900);
      i.waitForIdleSync();
      SystemClock.sleep(1500); // Wait for the landscape layout and rotation animation.
      shell("screencap -p /sdcard/Download/animation-editor-preview.png");
      i.runOnMainSync(
          () -> {
            for (String label : new String[] {"Duplicar", "Eliminar", "Más opciones"}) {
              Button control = button(tablet.getWindow().getDecorView(), label);
              android.graphics.Rect visible = new android.graphics.Rect();
              assertNotNull(label, control);
              assertTrue(label, control.getGlobalVisibleRect(visible));
              assertEquals(label, control.getWidth(), visible.width());
              assertEquals(label, control.getHeight(), visible.height());
            }
          });
    } finally {
      if (restored != null) {
        final AnimationActivity a = restored;
        if (!a.isDestroyed()) {
          i.runOnMainSync(a::onBackPressed);
          await(a::isDestroyed);
        }
      } else if (!host.isDestroyed()) i.runOnMainSync(host::onBackPressed);
      shell("wm size reset");
      if (archive != null) i.getTargetContext().getContentResolver().delete(archive, null, null);
      if (png != null) i.getTargetContext().getContentResolver().delete(png, null, null);
    }
  }
  private void stroke(AnimationActivity host, int color) {
    i.runOnMainSync(() -> {
      DrawingView.nativeBeginEdit();
      DrawingView.nativeStroke(12, 20, 72, 20, 3, color);
      canvas(host).viewportChanged();
    });
  }

  private void historyClick(AnimationActivity host, String label, int expected) {
    click(host, label);
    await(() -> host.readProjectProgress() == null
        && DrawingView.nativePickColor(40, 20) == expected);
  }

  @Test
  public void undoRedoSurviveFrameSwitchesAndAutosave() throws Exception {
    AnimationActivity host = launch(null);
    try {
      idle(host, 1, 0);
      final int[] blank = new int[1];
      i.runOnMainSync(() -> blank[0] = DrawingView.nativePickColor(40, 20));
      stroke(host, 0xff000000);
      i.runOnMainSync(() -> assertEquals(0xff000000, DrawingView.nativePickColor(40, 20)));
      i.runOnMainSync(() -> host.add(false));
      idle(host, 2, 1);
      stroke(host, 0xffff0000);
      i.runOnMainSync(() -> host.select(0));
      idle(host, 2, 0);
      historyClick(host, "Deshacer", blank[0]);
      i.runOnMainSync(() -> host.select(1));
      idle(host, 2, 1);
      i.runOnMainSync(() -> assertEquals(0xffff0000, DrawingView.nativePickColor(40, 20)));
      historyClick(host, "Deshacer", blank[0]);
      historyClick(host, "Rehacer", 0xffff0000);
      i.runOnMainSync(() -> host.select(0));
      idle(host, 2, 0);
      historyClick(host, "Rehacer", 0xff000000);
      historyClick(host, "Deshacer", blank[0]);
      // Run the actual idle-save callback: saving must not consume undo or redo.
      i.runOnMainSync(() -> {
        try {
          java.lang.reflect.Field f = AnimationActivity.class.getDeclaredField("saveWhenIdle");
          f.setAccessible(true);((Runnable)f.get(host)).run();
          java.lang.reflect.Field p = AnimationActivity.class.getDeclaredField("progress");
          p.setAccessible(true);
          assertEquals(View.GONE, ((View)p.get(host)).getVisibility());
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
      });
      await(() -> !busy(host));
      historyClick(host, "Rehacer", 0xff000000);
      historyClick(host, "Deshacer", blank[0]);
      historyClick(host, "Rehacer", 0xff000000);
    } finally {
      if (!host.isDestroyed()) {
        i.runOnMainSync(host::onBackPressed);
        await(host::isDestroyed);
      }
    }
  }
}
