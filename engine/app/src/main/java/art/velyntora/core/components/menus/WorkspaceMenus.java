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


/** Owns components/menus behavior; the host is the workspace composition facade. */
final class WorkspaceMenus {
  private final MainActivity host;
  WorkspaceMenus(MainActivity host) { this.host = host; }

  void openMainMenu() {
    String[] labels = new String[host.readMenuRegistry().getChildCount()];
    for (int i = 0; i < labels.length; i++)
      labels[i] = ((Button) host.readMenuRegistry().getChildAt(i)).getText().toString();
    new AlertDialog.Builder(host)
        .setTitle("Velyntora Core")
        .setItems(labels, (dialog, index) -> host.readMenuRegistry().getChildAt(index).performClick())
        .show();
  }

  void menu(LinearLayout parent, String title, String[] labels, Runnable[] actions) {
    host.button(
        parent,
        title,
        () ->
            new AlertDialog.Builder(host)
                .setTitle(title)
                .setItems(
                    labels,
                    (dialog, index) -> {
                      if (host.readProjectProgress() == null) actions[index].run();
                    })
                .show());
  }

  void createRegistry() {
    LinearLayout menus = host.row();
    new FileMenu(host).populate(menus);
    new EditMenu(host).populate(menus);
    new ViewMenu(host).populate(menus);
    new AdjustmentDialogs(host, host.readDrawing()).addMenu(menus);
    new ColorAdjustmentsMenu(host).populate(menus);
    new ImageMenu(host).populate(menus);
    new ToolsMenu(host).populate(menus);
    new EffectsMenu(host).populate(menus);
    new HelpMenu(host).populate(menus);
    host.writeMenuRegistry(menus);


  }
}
