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



final class ToolsMenu {
 private final MainActivity host;
 ToolsMenu(MainActivity host) {this.host=host;}

  void populate(LinearLayout menus) {
    host.menu(
        menus,
        "Herramientas",
        new String[] {
          "Configurar pincel",
          "Elegir color…",
          "Color secundario…",
          "Intercambiar colores",
          "Configurar degradado…",
          "Confirmar Línea/Curva",
          "Cancelar Línea/Curva"
        },
        new Runnable[] {
          host::configureBrush,
          host::configureColor,
          () -> host.configureColor(true),
          () -> {
            int first = host.readActiveColor();
            host.setActiveColor(host.readSecondaryColor());
            host.setSecondaryColor(first);
          },
          host::configureGradient,
          host.readDrawing()::confirmCurve,
          host.readDrawing()::cancelCurve
        });
  }
}
