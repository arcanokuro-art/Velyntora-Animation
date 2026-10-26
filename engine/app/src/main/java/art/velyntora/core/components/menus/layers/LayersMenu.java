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



final class LayersMenu {
 private final MainActivity host;
 LayersMenu(MainActivity host) {this.host=host;}

  void populate(LinearLayout menus) {
    host.menu(
        menus,
        "Capas",
        new String[] {
          "Añadir capa",
          "Seleccionar capa",
          "Propiedades de capa…",
          "Duplicar capa",
          "Combinar con la capa inferior",
          "Eliminar capa",
          "Mostrar / ocultar",
          "Subir capa",
          "Bajar capa"
        },
        new Runnable[] {
          host::addLayer,
          host::chooseLayer,
          () -> host.renameLayer(host.readDrawing().activeLayer()),
          () -> {host.readDrawing().layerAction(0);host.refreshLayerPanel();},
          () -> {host.readDrawing().layerAction(1);host.refreshLayerPanel();},
          host::deleteLayer,
          host::toggleLayer,
          () -> host.moveLayer(1),
          () -> host.moveLayer(-1)
        });
  }
}
