package app.velyntora.editor;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

public final class EditorNavigator {
  private EditorNavigator() {}

  public static void create(Activity host) {
    host.startActivity(
        new Intent(host, art.velyntora.core.AnimationActivity.class)
            .setAction("app.velyntora.animation.CREATE"));
  }

  public static void open(Activity host, Uri uri) {
    host.startActivity(
        new Intent(host, art.velyntora.core.AnimationActivity.class)
            .setAction("app.velyntora.animation.OPEN")
            .setData(uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
  }
}
