package art.velyntora.core;

import android.app.AlertDialog;
import android.widget.*;
import app.velyntora.animation.frames.FrameSequence;

/** Timeline controls are separate from the editor's document adapter. */
final class TimelinePanel {
  private final AnimationActivity host;

  TimelinePanel(AnimationActivity host) {
    this.host = host;
  }

  void render(
      LinearLayout timeline,
      LinearLayout bar,
      FrameSequence sequence,
      boolean playing,
      boolean onionEnabled) {
    timeline.removeAllViews();
    for (int i = 0; i < sequence.size(); i++) {
      final int index = i;
      Button b = host.control("" + (i + 1), () -> host.switchTo(index));
      b.setContentDescription("Fotograma " + (i + 1));
      b.setTextColor(i == sequence.selected() ? 0xffbca3ff : 0xffffffff);
      timeline.addView(b, new LinearLayout.LayoutParams(host.dp(64), host.dp(60)));
    }
    bar.removeAllViews();
    bar.addView(host.control(playing ? "Pausar" : "Reproducir", host::togglePlayback));
    bar.addView(host.control("+ Fotograma", () -> host.add(false)));
    bar.addView(host.control("Duplicar", () -> host.add(true)));
    bar.addView(
        host.control(
            "Eliminar",
            () ->
                new AlertDialog.Builder(host)
                    .setMessage("¿Eliminar el fotograma actual?")
                    .setPositiveButton("Eliminar", (d, w) -> host.remove())
                    .setNegativeButton("Cancelar", null)
                    .show()));
    bar.addView(host.control("←", () -> host.move(-1)));
    bar.addView(host.control("→", () -> host.move(1)));
    bar.addView(host.control(sequence.fps() + " FPS", host::chooseFps));
    bar.addView(host.control("Cebolla: " + (onionEnabled ? "sí" : "no"), host::toggleOnion));
    bar.addView(host.control("Guardar proyecto", () -> host.picker(7101, "animacion.vlyanim")));
    bar.addView(host.control("Exportar PNG", () -> host.picker(7102, "fotogramas.zip")));
  }
}
