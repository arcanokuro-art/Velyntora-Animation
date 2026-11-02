package app.velyntora.animation.setup;

import android.app.*;
import android.text.InputType;
import android.widget.*;
import java.util.function.Consumer;

/** New project settings remain outside the engine's per-document resize dialogs. */
public final class AnimationSetupDialog {
  public static final class Settings {
    public final String title;
    public final int width, height, fps;

    public Settings(String title, int width, int height, int fps) {
      this.title = title;
      this.width = width;
      this.height = height;
      this.fps = fps;
    }
  }

  public static void show(Activity host, int defaultFps, Consumer<Settings> create) {
    defaultFps=app.velyntora.animation.model.AnimationLimits.clampFps(defaultFps);
    LinearLayout form = new LinearLayout(host);
    form.setOrientation(LinearLayout.VERTICAL);
    form.setPadding(32, 16, 32, 16);
    EditText title = new EditText(host);
    title.setHint("Nombre de la animación");
    title.setSingleLine(true);
    form.addView(title);
    EditText width = input(host, form, "Anchura (1–6000 px)", "1280"),
        height = input(host, form, "Altura (1–6000 px)", "720");
    String[] labels = {
      "HD · 1280 × 720",
      "Full HD · 1920 × 1080",
      "Cuadrado · 1080 × 1080",
      "Vertical · 1080 × 1920",
      "Personalizado"
    };
    int[][] sizes = {{1280, 720}, {1920, 1080}, {1080, 1080}, {1080, 1920}};
    Spinner preset = new Spinner(host);
    preset.setAdapter(
        new ArrayAdapter<String>(host, android.R.layout.simple_spinner_dropdown_item, labels));
    form.addView(preset);
    preset.setOnItemSelectedListener(
        new android.widget.AdapterView.OnItemSelectedListener() {
          public void onItemSelected(
              android.widget.AdapterView<?> p, android.view.View v, int position, long id) {
            if (position < sizes.length) {
              width.setText("" + sizes[position][0]);
              height.setText("" + sizes[position][1]);
            }
          }

          public void onNothingSelected(android.widget.AdapterView<?> p) {}
        });
    TextView rate = new TextView(host);
    rate.setText(defaultFps + " FPS");
    form.addView(rate);
    SeekBar fps = new SeekBar(host);
    fps.setMax(app.velyntora.animation.model.AnimationLimits.MAX_FPS-1);
    fps.setProgress(defaultFps - 1);
    form.addView(fps);
    fps.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {
          public void onProgressChanged(SeekBar b, int value, boolean user) {
            rate.setText((value + 1) + " FPS");
          }

          public void onStartTrackingTouch(SeekBar b) {}

          public void onStopTrackingTouch(SeekBar b) {}
        });
    ScrollView scroll = new ScrollView(host);
    scroll.addView(form);
    AlertDialog dialog =
        new AlertDialog.Builder(host)
            .setTitle("Nueva animación")
            .setView(scroll)
            .setPositiveButton("Crear", null)
            .setNegativeButton("Cancelar", null)
            .create();
    dialog.setOnShowListener(
        d ->
            dialog
                .getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(
                    v -> {
                      try {
                        int w = Integer.parseInt(width.getText().toString()),
                            h = Integer.parseInt(height.getText().toString());
                        String name = title.getText().toString().trim();
                        if (w < 1 || h < 1 || w > 6000 || h > 6000 || name.length() > 100)
                          throw new IllegalArgumentException();
                        dialog.dismiss();
                        create.accept(
                            new Settings(
                                name.isEmpty() ? "Sin título" : name, w, h, fps.getProgress() + 1));
                      } catch (IllegalArgumentException error) {
                        Toast.makeText(
                                host,
                                "Usa dimensiones de 1 a 6000 px y un título de hasta 100"
                                    + " caracteres",
                                Toast.LENGTH_LONG)
                            .show();
                      }
                    }));
    dialog.show();
  }

  private static EditText input(Activity host, LinearLayout form, String hint, String value) {
    EditText field = new EditText(host);
    field.setHint(hint);
    field.setText(value);
    field.setInputType(InputType.TYPE_CLASS_NUMBER);
    form.addView(field);
    return field;
  }
}
