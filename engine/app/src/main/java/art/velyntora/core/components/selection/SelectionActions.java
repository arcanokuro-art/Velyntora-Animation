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


/** Owns components/selection behavior; the host is the workspace composition facade. */
final class SelectionActions {
  private final MainActivity host;
  SelectionActions(MainActivity host) { this.host = host; }

  void copySelection() {
    Bitmap copy = host.readDrawing().copySelection();
    if (copy == null) {
      host.message("No hay selección válida");
      return;
    }
    if (host.readSelectionClipboard() != null && !host.readSelectionClipboard().isRecycled())
      host.readSelectionClipboard().recycle();
    host.writeSelectionClipboard(copy);
    host.message("Selección copiada a la memoria de Velyntora");
  }

  void moveSelectedContent() {
    LinearLayout form = new LinearLayout(host);
    form.setOrientation(LinearLayout.VERTICAL);
    android.widget.EditText horizontal = new android.widget.EditText(host);
    horizontal.setInputType(
        android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
    horizontal.setHint("Desplazamiento horizontal (px)");
    horizontal.setText("0");
    android.widget.EditText vertical = new android.widget.EditText(host);
    vertical.setInputType(
        android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
    vertical.setHint("Desplazamiento vertical (px)");
    vertical.setText("0");
    form.addView(horizontal);
    form.addView(vertical);
    new AlertDialog.Builder(host)
        .setTitle("Mover contenido seleccionado")
        .setView(host.scrollForm(form))
        .setNegativeButton("Cancelar", null)
        .setPositiveButton(
            "Mover",
            (dialog, which) -> {
              try {
                int dx = Integer.parseInt(horizontal.getText().toString().trim());
                int dy = Integer.parseInt(vertical.getText().toString().trim());
                if (!host.readDrawing().moveSelectedPixels(dx, dy)) host.message("No se pudo mover la selección");
              } catch (NumberFormatException error) {
                host.message("Introduce desplazamientos numéricos válidos");
              }
            })
        .show();
  }

  void configureWandTolerance() {
    LinearLayout form = new LinearLayout(host);
    form.setOrientation(LinearLayout.VERTICAL);
    TextView value = new TextView(host);
    value.setText("Tolerancia: " + host.readWandTolerance() + " / 255");
    SeekBar slider = new SeekBar(host);
    slider.setMax(255);
    slider.setProgress(host.readWandTolerance());
    slider.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {
          public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
            value.setText("Tolerancia: " + progress + " / 255");
          }

          @Override
          public void onStartTrackingTouch(SeekBar bar) {}

          @Override
          public void onStopTrackingTouch(SeekBar bar) {}
        });
    form.addView(value);
    form.addView(slider);
    new AlertDialog.Builder(host)
        .setTitle("Varita mágica")
        .setView(host.scrollForm(form))
        .setNegativeButton("Cancelar", null)
        .setPositiveButton(
            "Aplicar",
            (dialog, which) -> {
              host.writeWandTolerance(slider.getProgress());
              host.readDrawing().setWandTolerance(host.readWandTolerance());
            })
        .show();
  }

  void duplicateSelection() {
    Bitmap copy = host.readDrawing().copySelection();
    if (copy == null) {
      host.message("No hay selección válida para duplicar");
      return;
    }
    try {
      if (!host.readDrawing().pasteBitmap(copy)) host.message("No se pudo duplicar la selección");
      else host.message("Selección duplicada");
    } finally {
      copy.recycle();
    }
  }

  void pasteSelection() {
    if (host.readSelectionClipboard() == null || host.readSelectionClipboard().isRecycled()) {
      host.message("No hay contenido copiado");
      return;
    }
    if (!host.readDrawing().pasteBitmap(host.readSelectionClipboard())) host.message("No se pudo pegar la selección");
    else host.message("Selección pegada");
  }

  void cutSelection() {
    Bitmap copy = host.readDrawing().copySelection();
    if (copy == null) {
      host.message("No hay selección válida");
      return;
    }
    if (!host.readDrawing().eraseSelection()) {
      copy.recycle();
      host.message("No se pudo cortar la selección");
      return;
    }
    if (host.readSelectionClipboard() != null && !host.readSelectionClipboard().isRecycled())
      host.readSelectionClipboard().recycle();
    host.writeSelectionClipboard(copy);
    host.message("Selección cortada");
  }

  void configureSelectionTransform() {
    if (!host.readDrawing().hasSelection()) {
      host.message("Crea una selección primero");
      return;
    }
    LinearLayout form = new LinearLayout(host);
    form.setOrientation(LinearLayout.VERTICAL);
    SeekBar angle = new SeekBar(host), scaleX = new SeekBar(host), scaleY = new SeekBar(host);
    angle.setMax(360);
    angle.setProgress(180);
    scaleX.setMax(390);
    scaleX.setProgress(90);
    scaleY.setMax(390);
    scaleY.setProgress(90);
    TextView label = host.text("Rotación: 0°  |  Escala X/Y: 100 % / 100 %");
    form.addView(label);
    form.addView(host.text("Rotación"));
    form.addView(angle);
    form.addView(host.text("Escala horizontal"));
    form.addView(scaleX);
    form.addView(host.text("Escala vertical"));
    form.addView(scaleY);
    SeekBar.OnSeekBarChangeListener listener =
        new SeekBar.OnSeekBarChangeListener() {
          public void onProgressChanged(SeekBar s, int n, boolean u) {
            label.setText(
                "Rotación: "
                    + (angle.getProgress() - 180)
                    + "°  |  X/Y: "
                    + (scaleX.getProgress() + 10)
                    + " % / "
                    + (scaleY.getProgress() + 10)
                    + " %");
          }

          public void onStartTrackingTouch(SeekBar s) {}

          public void onStopTrackingTouch(SeekBar s) {}
        };
    angle.setOnSeekBarChangeListener(listener);
    scaleX.setOnSeekBarChangeListener(listener);
    scaleY.setOnSeekBarChangeListener(listener);
    android.widget.CheckBox flipX = new android.widget.CheckBox(host),
        flipY = new android.widget.CheckBox(host);
    flipX.setText("Reflejar horizontalmente");
    flipY.setText("Reflejar verticalmente");
    form.addView(flipX);
    form.addView(flipY);
    form.addView(
        host.text(
            "Gira alrededor del centro de la selección. El contenido exterior al lienzo se"
                + " recorta."));
    new AlertDialog.Builder(host)
        .setTitle("Transformar selección")
        .setView(host.scrollForm(form))
        .setNegativeButton("Cancelar", null)
        .setPositiveButton(
            "Aplicar",
            (d, w) -> {
              if (!host.readDrawing().transformSelection(
                  angle.getProgress() - 180,
                  (scaleX.getProgress() + 10) / 100f * (flipX.isChecked() ? -1 : 1),
                  (scaleY.getProgress() + 10) / 100f * (flipY.isChecked() ? -1 : 1)))
                host.message("La transformación no cambia el contenido de la selección");
            })
        .show();
  }
}
