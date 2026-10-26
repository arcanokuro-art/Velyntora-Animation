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



final class ImageMenu {
 private final MainActivity host;
 ImageMenu(MainActivity host) {this.host=host;}

  void populate(LinearLayout menus) {
    host.menu(
        menus,
        "Imagen",
        new String[] {
          "Nuevo lienzo…",
          "Cambiar tamaño de imagen…",
          "Cambiar tamaño de lienzo…",
          "Recortar documento a selección",
          "Voltear capa horizontalmente",
          "Voltear capa verticalmente",
          "Rotar capa 180°",
          "Rotar capa 90° derecha",
          "Rotar capa 90° izquierda",
          "Conservar solo selección rectangular (capa)",
          "Conservar solo selección elíptica (capa)",
          "Conservar selección libre o varita (capa)"
        },
        new Runnable[] {
          () -> host.protectDocument(() -> host.configureDimensions(0)),
          () -> host.configureDimensions(1),
          () -> host.configureDimensions(2),
          () -> {
            if (!host.readDrawing().cropDocument())
              host.message("Selecciona el área del documento que quieres conservar");
          },
          () -> {
            if (!host.readDrawing().flipActiveHorizontal()) host.message("La capa no tiene cambios para voltear");
          },
          () -> {
            if (!host.readDrawing().flipActiveVertical()) host.message("La capa no tiene cambios para voltear");
          },
          () -> {
            if (!host.readDrawing().rotateActive180()) host.message("La capa no tiene cambios para rotar");
          },
          () -> {
            if (!host.readDrawing().rotateActive90(true)) host.message("La capa no tiene cambios para rotar");
          },
          () -> {
            if (!host.readDrawing().rotateActive90(false)) host.message("La capa no tiene cambios para rotar");
          },
          () -> {
            if (!host.readDrawing().trimActiveToRectSelection())
              host.message("Selecciona un rectángulo válido con contenido exterior");
          },
          () -> {
            if (!host.readDrawing().trimActiveToEllipseSelection())
              host.message("Selecciona una elipse válida con contenido exterior");
          },
          () -> {
            if (!host.readDrawing().trimActiveToFreeSelection())
              host.message("Selecciona una región libre o con varita que tenga contenido exterior");
          }
        });
  }
}
