package app.velyntora.editor;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import app.velyntora.animation.setup.AnimationSetupDialog.Settings;
import art.velyntora.core.AnimationActivity;

/** The home screen passes project settings or a .vlyanim URI to the animation editor. */
public final class EditorNavigator {
  private EditorNavigator() {}

  private static Intent intent(Activity host) {
    return new Intent(host, AnimationActivity.class)
        .setAction(AnimationActivity.ACTION)
        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
  }

  public static void create(Activity host, Settings s) {
    host.startActivity(
        intent(host)
            .putExtra("title", s.title)
            .putExtra("width", s.width)
            .putExtra("height", s.height)
            .putExtra("fps", s.fps));
  }

  public static void open(Activity host, Uri uri) {
    host.startActivity(intent(host).setData(uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
  }

  public static void resume(Activity host, String folder) {
    host.startActivity(intent(host).putExtra("session", folder));
  }
}
