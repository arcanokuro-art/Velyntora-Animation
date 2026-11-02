package app.velyntora.animation.timeline;

import android.content.Context;
import android.view.*;
import android.widget.*;
import app.velyntora.animation.preview.FramePreviewCache;
import app.velyntora.animation.storage.AnimationProject;

/** Presentation only; all document mutations are owned by the animation session. */
public final class TimelinePanel extends LinearLayout {
  public interface Actions {
    void select(int i);

    void add(boolean duplicate);

    void delete();

    void move(int delta);

    void playback();

    void fps();

    void exposure();

    void onion();

    void save();

    void export();
  }

  private final Actions actions;
  private final LinearLayout buttons;
  private final TextView status;
  private final TimelineStrip strip;
  private final HorizontalScrollView scroll;

  public TimelinePanel(Context c, FramePreviewCache cache, Actions actions) {
    super(c);
    this.actions = actions;
    setOrientation(VERTICAL);
    setBackgroundColor(0xff202329);
    status = new TextView(c);
    status.setTextColor(0xffeef1f7);
    status.setTextSize(11);
    addView(status, new LayoutParams(-1, dp(18)));
    scroll = new HorizontalScrollView(c);
    strip = new TimelineStrip(c, cache, actions::select);
    scroll.addView(strip);
    addView(scroll, new LayoutParams(-1, dp(70)));
    HorizontalScrollView controls = new HorizontalScrollView(c);
    buttons = new LinearLayout(c);
    controls.addView(buttons);
    addView(controls, new LayoutParams(-1, dp(44)));
  }

  private int dp(int n) {
    return Math.round(n * getResources().getDisplayMetrics().density);
  }

  private void button(String label, Runnable action) {
    Button b = new Button(getContext());
    b.setText(label);
    b.setAllCaps(false);
    b.setTextSize(12);
    b.setContentDescription(label);
    b.setOnClickListener(v -> action.run());
    buttons.addView(b, new LayoutParams(-2, -1));
  }

  public void bind(AnimationProject project, boolean playing, boolean onion) {
    strip.bind(project);
    buttons.removeAllViews();
    if (project == null) {
      status.setText("Preparando animación…");
      return;
    }
    status.setText(
        project.title
            + " · Fotograma "
            + (project.sequence.selected() + 1)
            + "/"
            + project.sequence.size()
            + " · "
            + project.sequence.fps()
            + " FPS · "
            + String.format(
                java.util.Locale.getDefault(),
                "%.2f s",
                project.sequence.totalTicks() / (double) project.sequence.fps()));
    button(playing ? "Pausar" : "Reproducir", actions::playback);
    button("+ Fotograma", () -> actions.add(false));
    button("Duplicar", () -> actions.add(true));
    button("Eliminar", actions::delete);
    button("← Mover", () -> actions.move(-1));
    button("Mover →", () -> actions.move(1));
    button(project.sequence.fps() + " FPS", actions::fps);
    button("Duración: " + project.sequence.current().exposure, actions::exposure);
    button("Cebolla: " + (onion ? "sí" : "no"), actions::onion);
    button("Guardar animación", actions::save);
    button("Exportar PNG", actions::export);
    scroll.post(
        () ->
            scroll.smoothScrollTo(
                Math.max(0, project.sequence.selected() * strip.cell - scroll.getWidth() / 2), 0));
  }
}
