package art.velyntora.core;

import static org.junit.Assert.*;

import android.app.*;
import android.content.*;
import android.view.*;
import android.widget.*;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;

/** Real UI -> native layered documents -> frame switching and lifecycle. */
public final class AnimationIntegrationTest {
  private final Instrumentation i = InstrumentationRegistry.getInstrumentation();

  private Button button(View v, String text) {
    if (v instanceof Button && text.contentEquals(((Button) v).getText())) return (Button) v;
    if (v instanceof ViewGroup) {
      ViewGroup g = (ViewGroup) v;
      for (int n = 0; n < g.getChildCount(); n++) {
        Button b = button(g.getChildAt(n), text);
        if (b != null) return b;
      }
    }
    return null;
  }

  private void click(Activity a, String text) {
    Button b = button(a.getWindow().getDecorView(), text);
    assertNotNull("Missing control: " + text, b);
    b.performClick();
  }

  @Test
  public void nativeFramesDuplicateSwitchAndPlayback() throws Exception {
    AnimationActivity a =
        (AnimationActivity)
            i.startActivitySync(
                new Intent(i.getTargetContext(), AnimationActivity.class)
                    .setAction("app.velyntora.animation.CREATE")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    try {
      i.waitForIdleSync();
      i.runOnMainSync(
          () -> {
            assertFalse(a.isFinishing());
            assertEquals(1280, a.readDrawing().documentWidth());
            int initial = a.readDrawing().layerCount();
            a.readDrawing().addLayer();
            assertEquals(initial + 1, a.readDrawing().layerCount());
            click(a, "Duplicar");
            assertEquals(initial + 1, a.readDrawing().layerCount());
            click(a, "+ Fotograma");
            assertEquals(initial, a.readDrawing().layerCount());
            click(a, "1");
            assertEquals(initial + 1, a.readDrawing().layerCount());
            click(a, "Cebolla: no");
            assertNotNull(button(a.getWindow().getDecorView(), "Cebolla: sí"));
            click(a, "Reproducir");
            assertNotNull(button(a.getWindow().getDecorView(), "Pausar"));
            click(a, "Pausar");
            assertNotNull(button(a.getWindow().getDecorView(), "Reproducir"));
          });
      i.waitForIdleSync();
      try (android.os.ParcelFileDescriptor command =
              i.getUiAutomation()
                  .executeShellCommand("screencap -p /sdcard/Download/animation-editor.png");
          java.io.InputStream output =
              new android.os.ParcelFileDescriptor.AutoCloseInputStream(command)) {
        byte[] bytes = new byte[1024];
        while (output.read(bytes) != -1) {}
      }
    } finally {
      i.runOnMainSync(a::finish);
    }
  }

  @Test
  public void exportedProjectOpensThroughHomeWithNativeLayers() throws Exception {
    android.content.Context context = i.getTargetContext();
    AnimationActivity source =
        (AnimationActivity)
            i.startActivitySync(
                new Intent(context, AnimationActivity.class)
                    .setAction("app.velyntora.animation.CREATE")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    final String[] session = new String[1];
    final int[] layers = new int[1];
    i.runOnMainSync(
        () -> {
          source.readDrawing().addLayer();
          layers[0] = source.readDrawing().layerCount();
          session[0] = source.getPreferences(0).getString("lastSession", null);
          source.finish();
        });
    i.waitForIdleSync();
    app.velyntora.animation.project.AnimationProject project =
        new app.velyntora.animation.project.AnimationProject(new java.io.File(session[0]));
    project.readMetadata();
    android.content.ContentValues values = new android.content.ContentValues();
    values.put(android.provider.MediaStore.Downloads.DISPLAY_NAME, "animation-integration.vlyanim");
    values.put(android.provider.MediaStore.Downloads.MIME_TYPE, "application/zip");
    android.net.Uri uri =
        context
            .getContentResolver()
            .insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
    assertNotNull(uri);
    android.app.Activity home = null;
    android.app.Activity opened = null;
    Instrumentation.ActivityMonitor monitor =
        i.addMonitor(AnimationActivity.class.getName(), null, false);
    try {
      try (java.io.OutputStream out = context.getContentResolver().openOutputStream(uri)) {
        project.export(out, false);
      }
      home =
          i.startActivitySync(
              new Intent(context, app.velyntora.MainActivity.class)
                  .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
      final android.app.Activity host = home;
      i.runOnMainSync(() -> app.velyntora.editor.EditorNavigator.open(host, uri));
      opened = i.waitForMonitorWithTimeout(monitor, 10000);
      assertNotNull(opened);
      final AnimationActivity restored = (AnimationActivity) opened;
      i.waitForIdleSync();
      i.runOnMainSync(
          () -> {
            assertFalse(restored.isFinishing());
            assertEquals(layers[0], restored.readDrawing().layerCount());
          });
    } finally {
      if (opened != null) {
        final android.app.Activity activity = opened;
        i.runOnMainSync(activity::finish);
      }
      if (home != null) {
        final android.app.Activity activity = home;
        i.runOnMainSync(activity::finish);
      }
      i.removeMonitor(monitor);
      context.getContentResolver().delete(uri, null, null);
    }
  }
}
