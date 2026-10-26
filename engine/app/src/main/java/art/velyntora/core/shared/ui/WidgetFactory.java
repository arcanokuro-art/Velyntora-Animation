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


/** Owns shared/ui behavior; the host is the workspace composition facade. */
final class WidgetFactory {
  private final MainActivity host;
  WidgetFactory(MainActivity host) { this.host = host; }

  int dp(int value) {
    return Math.round(host.getResources().getDisplayMetrics().density * value);
  }

  View scrollForm(View form) {
    ScrollView scroll = new ScrollView(host);
    scroll.addView(form, new ScrollView.LayoutParams(-1, -2));
    return scroll;
  }

  LinearLayout row() {
    LinearLayout layout = new LinearLayout(host);
    layout.setOrientation(LinearLayout.HORIZONTAL);
    layout.setGravity(Gravity.CENTER_VERTICAL);
    return layout;
  }

  TextView text(String label) {
    TextView view = new TextView(host);
    view.setText(label);
    view.setTextColor(0xFFF0F0F0);
    view.setTextSize(12);
    return view;
  }

  HorizontalScrollView addScrollable(LinearLayout root, LinearLayout content) {
    HorizontalScrollView scroll = new HorizontalScrollView(host);
    scroll.setHorizontalScrollBarEnabled(false);
    scroll.addView(content);
    root.addView(scroll);
    return scroll;
  }

  Button button(LinearLayout parent, String label, Runnable action) {
    Button button = new Button(host);
    button.setText(label);
    button.setTextColor(0xFFF0F0F0);
    button.setBackgroundResource(R.drawable.tool_button_background);
    button.setBackgroundTintList(null);
    button.setTextSize(12);
    button.setAllCaps(false);
    button.setMinWidth(0);
    button.setMinimumWidth(0);
    button.setMinHeight(host.dp(48));
    button.setMinimumHeight(host.dp(48));
    String description =
        label.equals("+")
            ? "Añadir capa"
            : label.equals("−")
                ? "Eliminar capa"
                : label.equals("↑") ? "Subir capa" : label.equals("↓") ? "Bajar capa" : label;
    button.setContentDescription(description);
    button.setOnClickListener(
        v -> {
          if(host.readDrawing()!=null && host.readDrawing().placedImage().active() && !host.readDrawing().placedImage().allow(label))return;
          if (host.readProjectProgress() == null || label.equals("Deshacer") || label.equals("Rehacer")) action.run();
          else host.message("Espera a que termine la operación actual");
        });
    parent.addView(button);
    return button;
  }

  Button iconButton(LinearLayout parent, String label, String glyph, Runnable action) {
    Button button = host.button(parent, label, action);
    button.setText("");
    button.setPadding(host.dp(12), host.dp(12), host.dp(12), host.dp(12));
    WorkspaceIconDrawable icon = new WorkspaceIconDrawable(glyph);
    icon.setBounds(0, 0, host.dp(24), host.dp(24));
    button.setCompoundDrawables(icon, null, null, null);
    button.setLayoutParams(new LinearLayout.LayoutParams(host.dp(48), host.dp(48)));
    if (android.os.Build.VERSION.SDK_INT >= 26) button.setTooltipText(label);
    return button;
  }

  void message(String text) {
    Toast.makeText(host, text, Toast.LENGTH_SHORT).show();
  }
}
