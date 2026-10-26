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



final class EditMenu {
 private final MainActivity host;
 EditMenu(MainActivity host) {this.host=host;}

  void populate(LinearLayout menus) {
    host.menu(
        menus,
        "Editar",
        new String[] {
          "Deshacer",
          "Rehacer",
          "Copiar selección",
          "Cortar selección",
          "Pegar selección",
          "Duplicar selección",
          "Mover contenido…",
          "Transformar selección…",
          "Seleccionar todo",
          "Invertir selección",
          "Expandir selección 1 px",
          "Contraer selección 1 px",
          "Borrar selección",
          "Deseleccionar"
        },
        new Runnable[] {
          host::undo,
          host::redo,
          host::copySelection,
          host::cutSelection,
          host::pasteSelection,
          host::duplicateSelection,
          host::moveSelectedContent,
          host::configureSelectionTransform,
          host.readDrawing()::selectAll,
          () -> {
            if (!host.readDrawing().invertSelection()) host.message("No se pudo invertir la selección");
          },
          () -> {
            if (!host.readDrawing().expandSelectionOnePixel()) host.message("No se pudo expandir la selección");
          },
          () -> {
            if (!host.readDrawing().shrinkSelectionOnePixel()) host.message("No se pudo contraer la selección");
          },
          () -> {
            if (!host.readDrawing().eraseSelection()) host.message("No hay selección válida");
          },
          host.readDrawing()::deselect
        });
  }
}
