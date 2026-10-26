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


/** Owns components/effects behavior; the host is the workspace composition facade. */
final class EffectsPanel {
  private final MainActivity host;
  EffectsPanel(MainActivity host) { this.host = host; }

  void configureBrightness() {
    LinearLayout form = new LinearLayout(host);
    form.setOrientation(LinearLayout.VERTICAL);
    TextView value = new TextView(host);
    SeekBar slider = new SeekBar(host);
    slider.setMax(510);
    slider.setProgress(255);
    value.setText("Brillo: 0");
    slider.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {
          public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
            value.setText("Brillo: " + (progress - 255));
          }

          @Override
          public void onStartTrackingTouch(SeekBar bar) {}

          @Override
          public void onStopTrackingTouch(SeekBar bar) {}
        });
    form.addView(value);
    form.addView(slider);
    new AlertDialog.Builder(host)
        .setTitle("Ajustar brillo de capa")
        .setView(host.scrollForm(form))
        .setNegativeButton("Cancelar", null)
        .setPositiveButton(
            "Aplicar",
            (dialog, which) -> {
              int adjustment = slider.getProgress() - 255;
              if (adjustment == 0) return;
              if (!host.readDrawing().brightnessActive(adjustment)) host.message("No hay cambios de brillo");
            })
        .show();
  }

  void configureContrast() {
    LinearLayout form = new LinearLayout(host);
    form.setOrientation(LinearLayout.VERTICAL);
    TextView value = new TextView(host);
    SeekBar slider = new SeekBar(host);
    slider.setMax(200);
    slider.setProgress(100);
    value.setText("Contraste: 0");
    slider.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {
          public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
            value.setText("Contraste: " + (progress - 100));
          }

          @Override
          public void onStartTrackingTouch(SeekBar bar) {}

          @Override
          public void onStopTrackingTouch(SeekBar bar) {}
        });
    form.addView(value);
    form.addView(slider);
    new AlertDialog.Builder(host)
        .setTitle("Ajustar contraste de capa")
        .setView(host.scrollForm(form))
        .setNegativeButton("Cancelar", null)
        .setPositiveButton(
            "Aplicar",
            (dialog, which) -> {
              int adjustment = slider.getProgress() - 100;
              if (adjustment == 0) return;
              if (!host.readDrawing().contrastActive(adjustment)) host.message("No hay cambios de contraste");
            })
        .show();
  }

  void configureThreshold() {
    LinearLayout form = new LinearLayout(host);
    form.setOrientation(LinearLayout.VERTICAL);
    TextView value = new TextView(host);
    SeekBar slider = new SeekBar(host);
    slider.setMax(255);
    slider.setProgress(128);
    value.setText("Umbral: 128");
    slider.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {
          public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
            value.setText("Umbral: " + progress);
          }

          @Override
          public void onStartTrackingTouch(SeekBar bar) {}

          @Override
          public void onStopTrackingTouch(SeekBar bar) {}
        });
    form.addView(value);
    form.addView(slider);
    new AlertDialog.Builder(host)
        .setTitle("Blanco y negro por umbral")
        .setView(host.scrollForm(form))
        .setNegativeButton("Cancelar", null)
        .setPositiveButton(
            "Aplicar",
            (dialog, which) -> {
              if (!host.readDrawing().thresholdActive(slider.getProgress()))
                host.message("No hay cambios de umbral");
            })
        .show();
  }

  void configureSaturation() {
    LinearLayout form = new LinearLayout(host);
    form.setOrientation(LinearLayout.VERTICAL);
    TextView value = new TextView(host);
    SeekBar slider = new SeekBar(host);
    slider.setMax(200);
    slider.setProgress(100);
    value.setText("Saturación: 0");
    slider.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {
          public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
            value.setText("Saturación: " + (progress - 100));
          }

          @Override
          public void onStartTrackingTouch(SeekBar bar) {}

          @Override
          public void onStopTrackingTouch(SeekBar bar) {}
        });
    form.addView(value);
    form.addView(slider);
    new AlertDialog.Builder(host)
        .setTitle("Ajustar saturación de capa")
        .setView(host.scrollForm(form))
        .setNegativeButton("Cancelar", null)
        .setPositiveButton(
            "Aplicar",
            (dialog, which) -> {
              int adjustment = slider.getProgress() - 100;
              if (adjustment == 0) return;
              if (!host.readDrawing().saturationActive(adjustment)) host.message("No hay cambios de saturación");
            })
        .show();
  }

  void configureGamma() {
    LinearLayout form = new LinearLayout(host);
    form.setOrientation(LinearLayout.VERTICAL);
    TextView value = new TextView(host);
    SeekBar slider = new SeekBar(host);
    slider.setMax(290);
    slider.setProgress(90);
    value.setText("Gamma: 100 %");
    slider.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {
          public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
            value.setText("Gamma: " + (progress + 10) + " %");
          }

          @Override
          public void onStartTrackingTouch(SeekBar bar) {}

          @Override
          public void onStopTrackingTouch(SeekBar bar) {}
        });
    form.addView(value);
    form.addView(slider);
    new AlertDialog.Builder(host)
        .setTitle("Ajustar gamma de capa")
        .setView(host.scrollForm(form))
        .setNegativeButton("Cancelar", null)
        .setPositiveButton(
            "Aplicar",
            (dialog, which) -> {
              int percent = slider.getProgress() + 10;
              if (percent == 100) return;
              if (!host.readDrawing().gammaActive(percent)) host.message("No hay cambios de gamma");
            })
        .show();
  }

  void configurePosterization() {
    LinearLayout form = new LinearLayout(host);
    form.setOrientation(LinearLayout.VERTICAL);
    TextView value = new TextView(host);
    SeekBar slider = new SeekBar(host);
    slider.setMax(30);
    slider.setProgress(2);
    value.setText("Niveles por canal: 4");
    slider.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {
          public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
            value.setText("Niveles por canal: " + (progress + 2));
          }

          @Override
          public void onStartTrackingTouch(SeekBar bar) {}

          @Override
          public void onStopTrackingTouch(SeekBar bar) {}
        });
    form.addView(value);
    form.addView(slider);
    new AlertDialog.Builder(host)
        .setTitle("Posterizar capa")
        .setView(host.scrollForm(form))
        .setNegativeButton("Cancelar", null)
        .setPositiveButton(
            "Aplicar",
            (dialog, which) -> {
              if (!host.readDrawing().posterizeActive(slider.getProgress() + 2))
                host.message("No hay cambios al posterizar");
            })
        .show();
  }

  void configureSolarization() {
    LinearLayout form = new LinearLayout(host);
    form.setOrientation(LinearLayout.VERTICAL);
    TextView value = new TextView(host);
    SeekBar slider = new SeekBar(host);
    slider.setMax(255);
    slider.setProgress(128);
    value.setText("Umbral de solarización: 128");
    slider.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {
          public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
            value.setText("Umbral de solarización: " + progress);
          }

          @Override
          public void onStartTrackingTouch(SeekBar bar) {}

          @Override
          public void onStopTrackingTouch(SeekBar bar) {}
        });
    form.addView(value);
    form.addView(slider);
    new AlertDialog.Builder(host)
        .setTitle("Solarizar capa")
        .setView(host.scrollForm(form))
        .setNegativeButton("Cancelar", null)
        .setPositiveButton(
            "Aplicar",
            (dialog, which) -> {
              if (!host.readDrawing().solarizeActive(slider.getProgress()))
                host.message("No hay cambios al solarizar");
            })
        .show();
  }

  void configureRgbOffsets() {
    LinearLayout form = new LinearLayout(host);
    form.setOrientation(LinearLayout.VERTICAL);
    final SeekBar[] sliders = new SeekBar[3];
    final String[] names = {"Rojo", "Verde", "Azul"};
    for (int i = 0; i < sliders.length; i++) {
      TextView value = new TextView(host);
      value.setText(names[i] + ": 0");
      SeekBar slider = new SeekBar(host);
      slider.setMax(510);
      slider.setProgress(255);
      final String name = names[i];
      slider.setOnSeekBarChangeListener(
          new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
              value.setText(name + ": " + (progress - 255));
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) {}

            @Override
            public void onStopTrackingTouch(SeekBar bar) {}
          });
      form.addView(value);
      form.addView(slider);
      sliders[i] = slider;
    }
    new AlertDialog.Builder(host)
        .setTitle("Ajustar canales RGB")
        .setView(host.scrollForm(form))
        .setNegativeButton("Cancelar", null)
        .setPositiveButton(
            "Aplicar",
            (dialog, which) -> {
              int red = sliders[0].getProgress() - 255;
              int green = sliders[1].getProgress() - 255;
              int blue = sliders[2].getProgress() - 255;
              if (red == 0 && green == 0 && blue == 0) return;
              if (!host.readDrawing().tintActive(red, green, blue)) host.message("No hay cambios en canales RGB");
            })
        .show();
  }

  void configureLevels() {
    LinearLayout form = new LinearLayout(host);
    form.setOrientation(LinearLayout.VERTICAL);
    TextView blackValue = new TextView(host);
    TextView whiteValue = new TextView(host);
    SeekBar black = new SeekBar(host);
    SeekBar white = new SeekBar(host);
    black.setMax(254);
    white.setMax(254);
    black.setProgress(16);
    white.setProgress(238);
    blackValue.setText("Punto negro: 16");
    whiteValue.setText("Punto blanco: 239");
    black.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {
          public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
            blackValue.setText("Punto negro: " + progress);
          }

          @Override
          public void onStartTrackingTouch(SeekBar bar) {}

          @Override
          public void onStopTrackingTouch(SeekBar bar) {}
        });
    white.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {
          @Override
          public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
            whiteValue.setText("Punto blanco: " + (progress + 1));
          }

          @Override
          public void onStartTrackingTouch(SeekBar bar) {}

          @Override
          public void onStopTrackingTouch(SeekBar bar) {}
        });
    form.addView(blackValue);
    form.addView(black);
    form.addView(whiteValue);
    form.addView(white);
    new AlertDialog.Builder(host)
        .setTitle("Ajustar niveles de capa")
        .setView(host.scrollForm(form))
        .setNegativeButton("Cancelar", null)
        .setPositiveButton(
            "Aplicar",
            (dialog, which) -> {
              int blackPoint = black.getProgress();
              int whitePoint = white.getProgress() + 1;
              if (blackPoint >= whitePoint) {
                host.message("El punto blanco debe ser mayor que el punto negro");
                return;
              }
              if (blackPoint == 0 && whitePoint == 255) return;
              if (!host.readDrawing().levelsActive(blackPoint, whitePoint))
                host.message("No hay cambios de niveles");
            })
        .show();
  }

  void configureExposure() {
    LinearLayout form = new LinearLayout(host);
    form.setOrientation(LinearLayout.VERTICAL);
    TextView value = new TextView(host);
    SeekBar slider = new SeekBar(host);
    slider.setMax(290);
    slider.setProgress(90);
    value.setText("Exposición: 100 %");
    slider.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {
          public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
            value.setText("Exposición: " + (progress + 10) + " %");
          }

          @Override
          public void onStartTrackingTouch(SeekBar bar) {}

          @Override
          public void onStopTrackingTouch(SeekBar bar) {}
        });
    form.addView(value);
    form.addView(slider);
    new AlertDialog.Builder(host)
        .setTitle("Ajustar exposición de capa")
        .setView(host.scrollForm(form))
        .setNegativeButton("Cancelar", null)
        .setPositiveButton(
            "Aplicar",
            (dialog, which) -> {
              int percent = slider.getProgress() + 10;
              if (percent == 100) return;
              if (!host.readDrawing().exposureActive(percent)) host.message("No hay cambios de exposición");
            })
        .show();
  }

  void configureHue() {
    LinearLayout form = new LinearLayout(host);
    form.setOrientation(LinearLayout.VERTICAL);
    TextView value = new TextView(host);
    SeekBar slider = new SeekBar(host);
    slider.setMax(360);
    slider.setProgress(180);
    value.setText("Rotación de tono: 0°");
    slider.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {
          public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
            value.setText("Rotación de tono: " + (progress - 180) + "°");
          }

          @Override
          public void onStartTrackingTouch(SeekBar bar) {}

          @Override
          public void onStopTrackingTouch(SeekBar bar) {}
        });
    form.addView(value);
    form.addView(slider);
    new AlertDialog.Builder(host)
        .setTitle("Rotar tono de capa")
        .setView(host.scrollForm(form))
        .setNegativeButton("Cancelar", null)
        .setPositiveButton(
            "Aplicar",
            (dialog, which) -> {
              int degrees = slider.getProgress() - 180;
              if (degrees == 0) return;
              if (!host.readDrawing().hueRotateActive(degrees)) host.message("No hay cambios de tono");
            })
        .show();
  }

  void configureRgbBalance() {
    LinearLayout form = new LinearLayout(host);
    form.setOrientation(LinearLayout.VERTICAL);
    SeekBar[] sliders = new SeekBar[3];
    String[] names = {"Rojo", "Verde", "Azul"};
    for (int i = 0; i < sliders.length; i++) {
      TextView value = new TextView(host);
      value.setText(names[i] + ": 100 %");
      SeekBar slider = new SeekBar(host);
      slider.setMax(300);
      slider.setProgress(100);
      String name = names[i];
      slider.setOnSeekBarChangeListener(
          new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
              value.setText(name + ": " + progress + " %");
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) {}

            @Override
            public void onStopTrackingTouch(SeekBar bar) {}
          });
      form.addView(value);
      form.addView(slider);
      sliders[i] = slider;
    }
    new AlertDialog.Builder(host)
        .setTitle("Balance de canales RGB")
        .setView(host.scrollForm(form))
        .setNegativeButton("Cancelar", null)
        .setPositiveButton(
            "Aplicar",
            (dialog, which) -> {
              int red = sliders[0].getProgress();
              int green = sliders[1].getProgress();
              int blue = sliders[2].getProgress();
              if (red == 100 && green == 100 && blue == 100) return;
              if (!host.readDrawing().colorBalanceActive(red, green, blue))
                host.message("No hay cambios de balance");
            })
        .show();
  }

  void configureQuantization() {
    LinearLayout form = new LinearLayout(host);
    form.setOrientation(LinearLayout.VERTICAL);
    TextView value = new TextView(host);
    SeekBar slider = new SeekBar(host);
    slider.setMax(63);
    slider.setProgress(15);
    value.setText("Paso de cuantización: 16");
    slider.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {
          public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
            value.setText("Paso de cuantización: " + (progress + 1));
          }

          @Override
          public void onStartTrackingTouch(SeekBar bar) {}

          @Override
          public void onStopTrackingTouch(SeekBar bar) {}
        });
    form.addView(value);
    form.addView(slider);
    new AlertDialog.Builder(host)
        .setTitle("Cuantizar colores de capa")
        .setView(host.scrollForm(form))
        .setNegativeButton("Cancelar", null)
        .setPositiveButton(
            "Aplicar",
            (dialog, which) -> {
              if (!host.readDrawing().quantizeActive(slider.getProgress() + 1))
                host.message("No hay cambios al cuantizar");
            })
        .show();
  }

  void configureHighlightCeiling() {
    LinearLayout form = new LinearLayout(host);
    form.setOrientation(LinearLayout.VERTICAL);
    TextView value = new TextView(host);
    SeekBar slider = new SeekBar(host);
    slider.setMax(255);
    slider.setProgress(224);
    value.setText("Límite máximo RGB: 224");
    slider.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {
          public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
            value.setText("Límite máximo RGB: " + progress);
          }

          @Override
          public void onStartTrackingTouch(SeekBar bar) {}

          @Override
          public void onStopTrackingTouch(SeekBar bar) {}
        });
    form.addView(value);
    form.addView(slider);
    new AlertDialog.Builder(host)
        .setTitle("Limitar luces de capa")
        .setView(host.scrollForm(form))
        .setNegativeButton("Cancelar", null)
        .setPositiveButton(
            "Aplicar",
            (dialog, which) -> {
              int ceiling = slider.getProgress();
              if (ceiling == 255) return;
              if (!host.readDrawing().clampHighlightsActive(ceiling))
                host.message("No hay cambios al limitar luces");
            })
        .show();
  }

  void configureEffect(int kind) {
    String[] names = {
      "Desenfoque de caja", "Enfocar", "Detectar bordes", "Repujado", "Pixelar", "Ruido", "Viñeta"
    };
    LinearLayout form = new LinearLayout(host);
    form.setOrientation(LinearLayout.VERTICAL);
    int max = kind == 0 || kind == 4 ? 64 : 100;
    SeekBar amount = new SeekBar(host);
    amount.setMax(max - 1);
    amount.setProgress(kind == 0 ? 2 : kind == 4 ? 7 : kind == 5 ? 19 : 49);
    TextView label = host.text("Valor: " + (amount.getProgress() + 1));
    form.addView(label);
    form.addView(amount);
    form.addView(host.text("Se aplica a toda la capa activa. Puedes deshacer el resultado."));
    amount.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {
          public void onProgressChanged(SeekBar s, int n, boolean u) {
            label.setText("Valor: " + (n + 1));
          }

          public void onStartTrackingTouch(SeekBar s) {}

          public void onStopTrackingTouch(SeekBar s) {}
        });
    new AlertDialog.Builder(host)
        .setTitle(names[kind])
        .setView(host.scrollForm(form))
        .setNegativeButton("Cancelar", null)
        .setPositiveButton("Aplicar", (d, w) -> host.runEffect(kind, amount.getProgress() + 1))
        .show();
  }

  void runEffect(int kind, int amount) {
    host.runColorOperation(() -> host.readDrawing().applyEffect(kind, amount));
  }

  void runColorOperation(MainActivity.ColorOperation operation) {
    if (host.readProjectProgress() != null) {
      host.message("Espera a que termine la operación actual");
      return;
    }
    if (!host.readDrawing().prepareEffectSelection()) {
      host.message("No se pudo preparar la selección");
      return;
    }
    android.app.ProgressDialog progress = new android.app.ProgressDialog(host);
    progress.setMessage("Aplicando efecto…");
    progress.setCancelable(false);
    host.writeProjectProgress(progress);
    progress.show();
    host.readDrawing().setEnabled(false);
    new Thread(
            () -> {
              boolean changed = operation.getAsBoolean();
              host.runOnUiThread(
                  () -> {
                    if (!host.isDestroyed()) {
                      progress.dismiss();
                      host.writeProjectProgress(null);
                      host.readDrawing().setEnabled(true);
                      if (changed) host.readDrawing().effectApplied();
                      else host.message("La capa no tiene cambios para este efecto");
                    }
                  });
            },
            "velyntora-effect")
        .start();
  }
}
