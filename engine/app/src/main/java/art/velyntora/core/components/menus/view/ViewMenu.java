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



final class ViewMenu {
 private final MainActivity host;
 ViewMenu(MainActivity host) {this.host=host;}

  void populate(LinearLayout menus) {
    host.menu(
        menus,
        "Ver",
        new String[] {
          "Ajustar al lienzo",
          "Acercar",
          "Alejar",
          "Zoom 100 %",
          "Zoom 7000 %",
          "Rotar vista 15° derecha",
          "Rotar vista 15° izquierda",
          "Restablecer rotación",
          "Mostrar / ocultar herramientas",
          "Mostrar / ocultar capas"
        },
        new Runnable[] {
          host.readDrawing()::fitCanvas,
          () -> host.readDrawing().zoomBy(1.25f),
          () -> host.readDrawing().zoomBy(0.8f),
          () -> host.readDrawing().setZoomPercent(100),
          () -> host.readDrawing().setZoomPercent(7000),
          () -> host.readDrawing().rotateView(15),
          () -> host.readDrawing().rotateView(-15),
          () -> host.readDrawing().rotateView(-host.readDrawing().rotationDegrees()),
          () -> host.togglePanel(host.readToolScroll()),
          () -> host.togglePanel(host.readLayerScroll())
        });
  }
}
