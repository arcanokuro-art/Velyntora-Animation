package art.velyntora.core;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import java.io.InputStream;
import java.io.OutputStream;


/** Owns components/statusbar behavior; the host is the workspace composition facade. */
final class StatusBar {
  private final MainActivity host;
  StatusBar(MainActivity host) { this.host = host; }

  void updateStatus() {
    if (host.readDocumentTitle() != null)
      host.readDocumentTitle().setText(host.readDocumentName() + (host.dirty() ? " • Sin guardar" : "") + " — Velyntora Core");
    if (host.readStatus() == null) return;
    host.readStatus().setText(
        String.format(
                java.util.Locale.US,
                "%s%s  |  %d × %d px  |  Zoom: %.1f %%  |  Rotación: %.1f°",
                host.readDocumentName(),
                host.dirty() ? " • Sin guardar" : "",
                host.readDrawing().documentWidth(),
                host.readDrawing().documentHeight(),
                host.readDrawing().zoomPercent(),
                host.readDrawing().rotationDegrees())
            + host.readDrawing().cursorStatus());
  }

  void create(LinearLayout root) {
    host.writeStatus(host.text("800 × 800 px  |  Zoom: ajustar  |  No guardado"));
    host.readStatus().setPadding(host.dp(10), host.dp(4), host.dp(10), host.dp(4));
    root.addView(host.readStatus());
    host.readDrawing().setOnViewportChangedListener(host::updateStatus);

  }
}
