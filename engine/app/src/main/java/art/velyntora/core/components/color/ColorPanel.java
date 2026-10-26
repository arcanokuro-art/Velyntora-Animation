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


/** Owns components/color behavior; the host is the workspace composition facade. */
final class ColorPanel {
  private final MainActivity host;
  private art.velyntora.core.components.color.ColorPair colors =
      new art.velyntora.core.components.color.ColorPair(Color.BLACK, Color.WHITE);
  int primary() { return colors.primary(); }
  int secondary() { return colors.secondary(); }
  int updatePrimary(int value) { colors = new art.velyntora.core.components.color.ColorPair(value, colors.secondary()); return value; }
  int updateSecondary(int value) { colors = new art.velyntora.core.components.color.ColorPair(colors.primary(), value); return value; }
  ColorPanel(MainActivity host) { this.host = host; }

  void configureColor() {
    host.configureColor(false);
  }

  void configureColor(boolean secondary) {
    ColorPickerDialog.show(host, host.readActiveColor(), host.readSecondaryColor(), secondary,
        (primary, second) -> { host.setActiveColor(primary); host.setSecondaryColor(second); });
  }

  void setSecondaryColor(int value) {
    host.writeSecondaryColor(value);
    host.readDrawing().setSecondaryColor(value);
    if (host.readSecondaryColorButton() != null) {
      host.readSecondaryColorButton().setBackground(new ColorSwatchDrawable(value));
      host.readSecondaryColorButton().setContentDescription(
          String.format(
              java.util.Locale.US,
              "Color secundario: rojo %d, verde %d, azul %d, alfa %d. Elegir color",
              value >>> 16 & 255,
              value >>> 8 & 255,
              value & 255,
              value >>> 24));
    }
  }

  void setActiveColor(int color) {
    host.writeActiveColor(color);
    host.readDrawing().setColor(color);
    if (host.readCurrentColorButton() != null) {
      host.readCurrentColorButton().setBackground(new ColorSwatchDrawable(color));
      String label =
          String.format(
              java.util.Locale.US,
              "Color activo: rojo %d, verde %d, azul %d, alfa %d. Elegir color",
              color >>> 16 & 255,
              color >>> 8 & 255,
              color & 255,
              color >>> 24);
      host.readCurrentColorButton().setContentDescription(label);
      if (android.os.Build.VERSION.SDK_INT >= 26)
        host.readCurrentColorButton().setTooltipText(
            String.format(java.util.Locale.US, "Color activo #%08X", color));
    }
  }

  void create(LinearLayout root) {
    LinearLayout colors = host.row();
    host.writeCurrentColorButton(new Button(host));
    host.readCurrentColorButton().setText("");
    host.readCurrentColorButton().setPadding(0, 0, 0, 0);
    host.readCurrentColorButton().setBackgroundTintList(null);
    host.readCurrentColorButton().setOnClickListener(
        v -> {
          if (host.readProjectProgress() == null) host.configureColor();
        });
    colors.addView(host.readCurrentColorButton(), new LinearLayout.LayoutParams(host.dp(48), host.dp(48)));
    host.setActiveColor(host.readActiveColor());
    host.writeSecondaryColorButton(new Button(host));
    host.readSecondaryColorButton().setText("");
    host.readSecondaryColorButton().setPadding(0, 0, 0, 0);
    host.readSecondaryColorButton().setBackgroundTintList(null);
    host.readSecondaryColorButton().setOnClickListener(
        v -> {
          if (host.readProjectProgress() == null) host.configureColor(true);
        });
    colors.addView(host.readSecondaryColorButton(), new LinearLayout.LayoutParams(host.dp(48), host.dp(48)));
    host.setSecondaryColor(host.readSecondaryColor());
    host.iconButton(
        colors,
        "Intercambiar colores",
        "swap",
        () -> {
          int first = host.readActiveColor();
          host.setActiveColor(host.readSecondaryColor());
          host.setSecondaryColor(first);
        });
    Button saveColor = new Button(host);
    saveColor.setText("+");
    saveColor.setTextSize(22);
    saveColor.setMinWidth(0);saveColor.setMinimumWidth(0);
    saveColor.setPadding(0,0,0,0);
    saveColor.setContentDescription("Guardar color activo");
    if(android.os.Build.VERSION.SDK_INT>=26)
      saveColor.setTooltipText("Guardar color activo. Mantener pulsado para guardar el secundario");
    colors.addView(saveColor,new LinearLayout.LayoutParams(host.dp(48),host.dp(48)));
    int[] palette = {
      0xff000000,
      0xffffffff,
      0xff808080,
      0xffc0c0c0,
      0xffff0000,
      0xffff8080,
      0xffff9800,
      0xffffcc80,
      0xffffff00,
      0xffffff80,
      0xff00c000,
      0xff80ff80,
      0xff00ffff,
      0xff80ffff,
      0xff0000ff,
      0xff8080ff,
      0xffff00ff,
      0xffff80ff,
      0xff795548,
      0xffbcaaa4,
      0xff9c27b0,
      0xffce93d8
    };
    android.widget.GridLayout paletteGrid = new android.widget.GridLayout(host);
    paletteGrid.setRowCount(2);
    paletteGrid.setColumnCount(palette.length / 2);
    for (int i = 0; i < palette.length; i++) {
      final int color = palette[i];
      View swatch = new View(host);
      swatch.setBackgroundColor(color);
      swatch.setFocusable(true);
      swatch.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
      swatch.setContentDescription(
          String.format(java.util.Locale.US, "Color #%06X", color & 0xffffff));
      android.widget.GridLayout.LayoutParams cell =
          new android.widget.GridLayout.LayoutParams(
              android.widget.GridLayout.spec(i % 2), android.widget.GridLayout.spec(i / 2));
      cell.width = host.dp(24);
      cell.height = host.dp(24);
      paletteGrid.addView(swatch, cell);
      swatch.setOnClickListener(v -> host.setActiveColor(color));
      swatch.setOnLongClickListener(
          v -> {
            host.setSecondaryColor(color);
            return true;
          });
    }
    colors.addView(paletteGrid);
    SavedColorsControls.attach(host,colors,saveColor);
    host.writePaletteScroll(host.addScrollable(root, colors));

  }
}
