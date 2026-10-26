package art.velyntora.core;

import android.view.View;
import android.view.MotionEvent;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

/** Owns the opacity widgets; callbacks always resolve the current tool. */
final class OpacityControls {
  static void attach(MainActivity activity, DrawingView drawing, LinearLayout options) {
    TextView label = activity.text("Opacidad: 100 %");
    label.setSingleLine(true);
    int width = (int)Math.ceil(label.getPaint().measureText("Opacidad: 100 %"))
        + label.getPaddingLeft() + label.getPaddingRight() + activity.dp(8);
    options.addView(label, new LinearLayout.LayoutParams(width, -2));
    SeekBar slider = new SeekBar(activity);
    slider.setContentDescription("Opacidad de la herramienta");
    slider.setMax(100);
    options.addView(slider, new LinearLayout.LayoutParams(activity.dp(120), activity.dp(48)));
    final boolean[] syncing = {false};
    Runnable sync = () -> {
      syncing[0] = true;
      boolean visible = drawing.supportsToolOpacity();
      label.setVisibility(visible ? View.VISIBLE : View.GONE);
      slider.setVisibility(visible ? View.VISIBLE : View.GONE);
      slider.setProgress(drawing.toolOpacityPercent());
      label.setText("Opacidad: " + drawing.toolOpacityPercent() + " %");
      syncing[0] = false;
    };
    slider.setOnTouchListener((view, event) -> {
      int action = event.getActionMasked();
      if (action == MotionEvent.ACTION_DOWN) view.getParent().requestDisallowInterceptTouchEvent(true);
      else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL)
        view.getParent().requestDisallowInterceptTouchEvent(false);
      return false;
    });
    slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      public void onProgressChanged(SeekBar bar, int n, boolean fromUser) {
        if (!syncing[0]) drawing.setToolOpacityPercent(n);
      }
      public void onStartTrackingTouch(SeekBar bar) {}
      public void onStopTrackingTouch(SeekBar bar) {}
    });
    drawing.toolOpacityChanged = sync;
    sync.run();
  }
}
