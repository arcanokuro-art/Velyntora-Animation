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



final class EffectsMenu {
 private final MainActivity host;
 EffectsMenu(MainActivity host) {this.host=host;}

  void populate(LinearLayout menus) {
    AdjustmentDialogs filters = new AdjustmentDialogs(host, host.readDrawing());
    host.menu(
        menus,
        "Efectos",
        new String[] {
          "Básicos",
          "Desenfoques",
          "Distorsiones",
          "Arte y fotografía",
          "Generadores",
          "Más filtros",
          "Objetos"
        },
        new Runnable[] {
          () ->
              new AlertDialog.Builder(host)
                  .setTitle("Efectos básicos")
                  .setItems(
                      new String[] {
                        "Desenfoque de caja…",
                        "Enfocar…",
                        "Detectar bordes…",
                        "Repujado…",
                        "Pixelar…",
                        "Ruido…",
                        "Viñeta…"
                      },
                      (d, k) -> host.configureEffect(k))
                  .show(),
          filters::openBlurMenu,
          filters::openDistortionMenu,
          filters::openArtisticMenu,
          filters::openRenderMenu,
          filters::openUtilityMenu,
          filters::openObjectMenu
        });
  }
}
