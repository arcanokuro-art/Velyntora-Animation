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



final class CommandBar {
 private final MainActivity host;
 CommandBar(MainActivity host) {this.host=host;}

  void create(LinearLayout root) {
    LinearLayout header = host.row();
    host.iconButton(header, "Menú principal", "menu", host::openMainMenu);
    LinearLayout commands = host.row();
    host.iconButton(commands, "Nuevo", "new", () -> host.protectDocument(() -> host.configureDimensions(0)));
    host.iconButton(commands, "Abrir", "open", host::openImage);
    host.iconButton(commands, "Guardar", "save", () -> host.projectPicker(true));
    host.iconButton(commands, "Deshacer", "undo", host::undo);
    host.iconButton(commands, "Rehacer", "redo", host::redo);
    HorizontalScrollView shortcuts = new HorizontalScrollView(host);
    shortcuts.setHorizontalScrollBarEnabled(false);
    shortcuts.addView(commands);
    header.addView(shortcuts, new LinearLayout.LayoutParams(0, host.dp(48), 1));
    host.writeDocumentTitle(host.text("Imagen no guardada — Velyntora Core"));
    host.readDocumentTitle().setSingleLine(true);
    host.readDocumentTitle().setEllipsize(android.text.TextUtils.TruncateAt.END);
    host.readDocumentTitle().setGravity(Gravity.CENTER);
    header.addView(host.readDocumentTitle(), new LinearLayout.LayoutParams(0, host.dp(48), 1));
    host.iconButton(header, "Herramientas", "tools", () -> host.togglePanel(host.readToolScroll()));
    host.iconButton(header, "Capas", "layers", () -> host.togglePanel(host.readLayerScroll()));
    root.addView(header);

  }
}
