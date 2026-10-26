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


/** Owns document/dimensions behavior; the host is the workspace composition facade. */
final class DocumentDimensionsDialog {
  private final MainActivity host;
  DocumentDimensionsDialog(MainActivity host) { this.host = host; }

  void resizeDocumentAsync(int w, int h, boolean scale, boolean bilinear, int anchor) {
    if (host.readProjectProgress() != null) {
      host.message("Espera a que termine la operación actual");
      return;
    }
    android.app.ProgressDialog progress = new android.app.ProgressDialog(host);
    progress.setMessage("Cambiando tamaño…");
    progress.setCancelable(false);
    host.writeProjectProgress(progress);
    progress.show();
    host.readDrawing().setEnabled(false);
    new Thread(
            () -> {
              boolean ok = host.readDrawing().resizeDocumentPixels(w, h, scale, bilinear, anchor);
              host.runOnUiThread(
                  () -> {
                    if (!host.isDestroyed()) {
                      progress.dismiss();
                      host.writeProjectProgress(null);
                      host.readDrawing().setEnabled(true);
                      if (ok) host.readDrawing().documentResized();
                      else host.message("Sin cambios o tamaño demasiado grande para estas capas");
                    }
                  });
            },
            "velyntora-resize")
        .start();
  }

  android.text.TextWatcher dimensionWatcher(Runnable change) {
    return new android.text.TextWatcher() {
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

      public void onTextChanged(CharSequence s, int start, int before, int count) {}

      public void afterTextChanged(android.text.Editable value) {
        change.run();
      }
    };
  }

  void configureDimensions(int mode) {
    LinearLayout form = new LinearLayout(host);
    form.setOrientation(LinearLayout.VERTICAL);
    android.widget.EditText width = new android.widget.EditText(host),
        height = new android.widget.EditText(host);
    width.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
    height.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
    width.setText(Integer.toString(host.readDrawing().documentWidth()));
    height.setText(Integer.toString(host.readDrawing().documentHeight()));
    form.addView(host.text("Ancho (px)"));
    form.addView(width);
    form.addView(host.text("Alto (px)"));
    form.addView(height);
    form.addView(host.text("Límite: 6000 px por lado y 36 millones de píxeles en total."));
    final int originalWidth = host.readDrawing().documentWidth(), originalHeight = host.readDrawing().documentHeight();
    final int[] ratio = {originalWidth, originalHeight}, lastEdited = {0};
    final boolean[] updating = {false};
    android.widget.CheckBox lockRatio = new android.widget.CheckBox(host);
    lockRatio.setText("Mantener proporciones");
    lockRatio.setChecked(mode == 1);
    android.widget.EditText percent = new android.widget.EditText(host);
    percent.setInputType(
        android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
    percent.setText("100");
    percent.setContentDescription("Escala uniforme en porcentaje");
    if (mode == 1) {
      form.addView(lockRatio);
      form.addView(host.text("Escala uniforme (%)"));
      form.addView(percent);
    }
    Runnable synchronize =
        () -> {
          if (updating[0] || mode != 1) return;
          updating[0] = true;
          try {
            DocumentDimensions dimensions;
            if (lastEdited[0] == 2) {
              dimensions =
                  DocumentDimensions.fromPercent(
                      originalWidth,
                      originalHeight,
                      Double.parseDouble(percent.getText().toString().replace(',', '.')));
              ratio[0] = originalWidth;
              ratio[1] = originalHeight;
            } else if (lockRatio.isChecked())
              dimensions =
                  lastEdited[0] == 0
                      ? DocumentDimensions.fromWidth(
                          ratio[0], ratio[1], Integer.parseInt(width.getText().toString()))
                      : DocumentDimensions.fromHeight(
                          ratio[0], ratio[1], Integer.parseInt(height.getText().toString()));
            else {
              percent.setText("");
              return;
            }
            if (lastEdited[0] != 0) width.setText(Integer.toString(dimensions.width));
            if (lastEdited[0] != 1) height.setText(Integer.toString(dimensions.height));
            width.setError(null);
            height.setError(null);
            percent.setError(null);
            if (lastEdited[0] != 2) percent.setText("");
          } catch (IllegalArgumentException ignored) {
            /* Keep partial input editable; validate when applying. */
          } finally {
            updating[0] = false;
          }
        };
    width.addTextChangedListener(
        host.dimensionWatcher(
            () -> {
              if (!updating[0]) {
                lastEdited[0] = 0;
                synchronize.run();
              }
            }));
    height.addTextChangedListener(
        host.dimensionWatcher(
            () -> {
              if (!updating[0]) {
                lastEdited[0] = 1;
                synchronize.run();
              }
            }));
    percent.addTextChangedListener(
        host.dimensionWatcher(
            () -> {
              if (!updating[0]) {
                lastEdited[0] = 2;
                synchronize.run();
              }
            }));
    lockRatio.setOnCheckedChangeListener(
        (button, checked) -> {
          if (checked)
            try {
              DocumentDimensions dimensions =
                  new DocumentDimensions(
                      Integer.parseInt(width.getText().toString()),
                      Integer.parseInt(height.getText().toString()));
              ratio[0] = dimensions.width;
              ratio[1] = dimensions.height;
            } catch (IllegalArgumentException ignored) {
            }
        });
    if (mode == 0) {
      android.widget.Spinner presets = new android.widget.Spinner(host);
      presets.setContentDescription("Tamaños rápidos de documento");
      presets.setAdapter(
          new android.widget.ArrayAdapter<String>(
              host,
              android.R.layout.simple_spinner_dropdown_item,
              new String[] {
                "Personalizado",
                "Cuadrado · 512 × 512",
                "Cuadrado · 1024 × 1024",
                "Horizontal · 1920 × 1080",
                "Vertical · 1080 × 1920",
                "Panorama · 2400 × 1000",
                "Horizontal · 2560 × 1600",
                "4K · 3840 × 2160"
              }));
      final int[][] sizes = {
        {originalWidth, originalHeight},
        {512, 512},
        {1024, 1024},
        {1920, 1080},
        {1080, 1920},
        {2400, 1000},
        {2560, 1600},
        {3840, 2160}
      };
      presets.setOnItemSelectedListener(
          new android.widget.AdapterView.OnItemSelectedListener() {
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}

            public void onItemSelected(
                android.widget.AdapterView<?> parent, View view, int position, long id) {
              if (position > 0) {
                width.setText(Integer.toString(sizes[position][0]));
                height.setText(Integer.toString(sizes[position][1]));
              }
            }
          });
      form.addView(host.text("Tamaños rápidos"));
      form.addView(presets);
    }
    android.widget.Spinner method = new android.widget.Spinner(host),
        anchor = new android.widget.Spinner(host);
    method.setAdapter(
        new android.widget.ArrayAdapter<String>(
            host,
            android.R.layout.simple_spinner_dropdown_item,
            new String[] {"Píxel cercano (pixel art)", "Bilineal (suave)"}));
    method.setSelection(1);
    method.setContentDescription("Método de remuestreo");
    anchor.setAdapter(
        new android.widget.ArrayAdapter<String>(
            host,
            android.R.layout.simple_spinner_dropdown_item,
            new String[] {
              "Arriba izquierda",
              "Arriba centro",
              "Arriba derecha",
              "Centro izquierda",
              "Centro",
              "Centro derecha",
              "Abajo izquierda",
              "Abajo centro",
              "Abajo derecha"
            }));
    anchor.setSelection(4);
    anchor.setContentDescription("Anclaje del contenido");
    if (mode == 1) {
      form.addView(host.text("Remuestreo"));
      form.addView(method);
    }
    if (mode == 2) {
      form.addView(host.text("Anclaje del contenido"));
      form.addView(anchor);
    }
    width.setContentDescription("Ancho en píxeles");
    height.setContentDescription("Alto en píxeles");
    form.addView(
        host.text(
            mode == 2
                ? "El anclaje determina dónde se conserva el contenido al ampliar o recortar. El"
                    + " área nueva es transparente."
                : mode == 1
                    ? "Todas las capas conservan sus propiedades. Bilineal interpola color y"
                        + " transparencia; píxel cercano mantiene bordes duros."
                    : "Se crea un documento nuevo. Guarda primero el dibujo actual."));
    AlertDialog dialog =
        new AlertDialog.Builder(host)
            .setTitle(
                mode == 0 ? "Nuevo documento" : mode == 1 ? "Tamaño de imagen" : "Tamaño de lienzo")
            .setView(host.scrollForm(form))
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Aplicar", null)
            .create();
    dialog.setOnShowListener(
        ignored ->
            dialog
                .getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(
                    view -> {
                      try {
                        DocumentDimensions dimensions;
                        if (mode == 1 && lastEdited[0] == 2)
                          dimensions =
                              DocumentDimensions.fromPercent(
                                  originalWidth,
                                  originalHeight,
                                  Double.parseDouble(
                                      percent.getText().toString().replace(',', '.')));
                        else if (mode == 1 && lockRatio.isChecked())
                          dimensions =
                              lastEdited[0] == 1
                                  ? DocumentDimensions.fromHeight(
                                      ratio[0],
                                      ratio[1],
                                      Integer.parseInt(height.getText().toString()))
                                  : DocumentDimensions.fromWidth(
                                      ratio[0],
                                      ratio[1],
                                      Integer.parseInt(width.getText().toString()));
                        else
                          dimensions =
                              new DocumentDimensions(
                                  Integer.parseInt(width.getText().toString()),
                                  Integer.parseInt(height.getText().toString()));
                        int w = dimensions.width, h = dimensions.height;
                        if (host.readProjectProgress() != null) {
                          host.message("Espera a que termine la operación actual");
                          return;
                        }
                        if (mode == 0) {
                          if (!host.readDrawing().newDocument(w, h)) {
                            host.message("No se pudo crear el documento");
                            return;
                          }
                          host.markDocumentClean("Sin título");
                          dialog.dismiss();
                        } else {
                          dialog.dismiss();
                          host.resizeDocumentAsync(
                              w,
                              h,
                              mode == 1,
                              method.getSelectedItemPosition() == 1,
                              anchor.getSelectedItemPosition());
                        }
                      } catch (IllegalArgumentException e) {
                        host.message(
                            e instanceof NumberFormatException
                                ? "Introduce dimensiones y porcentaje válidos"
                                : e.getMessage());
                      }
                    }));
    dialog.show();
  }
}
