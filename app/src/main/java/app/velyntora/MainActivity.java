package app.velyntora;

import android.app.*;
import android.content.*;
import android.os.Bundle;
import android.widget.*;
import app.velyntora.animation.setup.AnimationSetupDialog;
import app.velyntora.animation.storage.AnimationProject;
import app.velyntora.editor.EditorNavigator;
import app.velyntora.ui.start.*;
import java.io.File;

public final class MainActivity extends Activity implements StartActions {
  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    showHome();
  }

  private void showHome() {
    setContentView(new StartScreen(this, this).create());
    app.velyntora.ui.common.ImmersiveMode.apply(getWindow());
  }

  @Override
  public void onWindowFocusChanged(boolean focus) {
    super.onWindowFocusChanged(focus);
    if (focus) app.velyntora.ui.common.ImmersiveMode.apply(getWindow());
  }

  @Override
  protected void onResume() {
    super.onResume();
    showHome();
  }

  public void createAnimation() {
    AnimationSetupDialog.show(
        this,
        getSharedPreferences("animation", 0).getInt("fps", 12),
        settings -> EditorNavigator.create(this, settings));
  }

  public void openProject() {
    startActivityForResult(
        new Intent(Intent.ACTION_OPEN_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("*/*")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
        2001);
  }

  @Override
  protected void onActivityResult(int request, int result, Intent data) {
    super.onActivityResult(request, result, data);
    if (request == 2001 && result == RESULT_OK && data != null && data.getData() != null)
      EditorNavigator.open(this, data.getData());
  }

  public void showRecentProjects() {
    File[] dirs = AnimationProject.recent(new File(getFilesDir(), "animations"));
    if (dirs.length == 0) {
      info("Animaciones recientes", "Todavía no hay animaciones guardadas.");
      return;
    }
    String[] labels = new String[dirs.length];
    for (int i = 0; i < dirs.length; i++) {
      try {
        AnimationProject p = new AnimationProject(dirs[i], "Animación", 1280, 720, 12);
        p.read();
        labels[i] = p.title + " · " + p.sequence.size() + " fotogramas";
      } catch (java.io.IOException error) {
        labels[i] = "Proyecto incompleto";
      }
    }
    new AlertDialog.Builder(this)
        .setTitle("Animaciones recientes")
        .setItems(labels, (d, i) -> EditorNavigator.resume(this, dirs[i].getAbsolutePath()))
        .setNegativeButton("Cerrar", null)
        .show();
  }

  public void showPreferences() {
    SeekBar slider = new SeekBar(this);
    slider.setMax(59);
    slider.setProgress(getSharedPreferences("animation", 0).getInt("fps", 12) - 1);
    TextView value = new TextView(this);
    value.setText((slider.getProgress() + 1) + " FPS");
    LinearLayout box = new LinearLayout(this);
    box.setOrientation(LinearLayout.VERTICAL);
    box.addView(value);
    box.addView(slider);
    slider.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {
          public void onProgressChanged(SeekBar bar, int n, boolean user) {
            value.setText((n + 1) + " FPS");
          }

          public void onStartTrackingTouch(SeekBar b) {}

          public void onStopTrackingTouch(SeekBar b) {}
        });
    new AlertDialog.Builder(this)
        .setTitle("FPS para nuevas animaciones")
        .setView(box)
        .setPositiveButton(
            "Guardar",
            (d, w) ->
                getSharedPreferences("animation", 0)
                    .edit()
                    .putInt("fps", slider.getProgress() + 1)
                    .apply())
        .setNegativeButton("Cancelar", null)
        .show();
  }

  public void showHelp() {
    info(
        "Velyntora Animation · Piloto",
        "Crea una animación, dibuja en cada fotograma y usa la línea de tiempo para añadir,"
            + " duplicar o reordenar. Ajusta FPS y duración; activa Cebolla para ver los fotogramas"
            + " vecinos.\n\n"
            + "Las sesiones se guardan en el dispositivo. Usa Guardar animación para exportar un"
            + " proyecto .vlyanim con capas, o Exportar PNG para obtener la secuencia completa.\n\n"
            + "Powered By Zaskar Startup");
  }

  private void info(String title, String body) {
    new AlertDialog.Builder(this)
        .setTitle(title)
        .setMessage(body)
        .setPositiveButton("Aceptar", null)
        .show();
  }
}
