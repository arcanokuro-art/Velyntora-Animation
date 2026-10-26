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


/** Owns app/workspace behavior; the host is the workspace composition facade. */
final class WorkspaceComposition {
  private final MainActivity host;
  WorkspaceComposition(MainActivity host) { this.host = host; }

  void onCreate(Bundle state) {
    
    boolean freshProcess = !DrawingView.hasDocument();
    host.writeDrawing(new DrawingView(host));
    if (host.readSavedRevision() < 0 && freshProcess) host.writeSavedRevision(host.readDrawing().revision());
    host.readDrawing().setOnTextPositionListener(host::configureText);
    host.readDrawing().setOnColorPickedListener(
        color -> {
          host.setActiveColor(color);
          host.message("Color seleccionado");
        });
    host.readDrawing().setOnCanvasChangedListener(
        () -> {
          host.syncToolState();
          host.updateStatus();
          host.readLayerRefreshHandler().removeCallbacks(host.readLayerRefreshTask());
          host.readLayerRefreshHandler().postDelayed(host.readLayerRefreshTask(), 120);
        });
    LinearLayout root = new LinearLayout(host);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundColor(0xFF2B2B2B);
    host.getWindow().setStatusBarColor(0xFF202020);
    host.getWindow().setNavigationBarColor(0xFF202020);

    host.composeWorkspaceMenus();

    new CommandBar(host).create(root);
    host.composeToolControls(root);
    host.composeLayersPanel(root);
    host.refreshLayerPanel();
    host.writeWorkspaceFrame(new android.widget.FrameLayout(host));
    host.layoutWorkspace();
    root.addView(host.readWorkspaceFrame(), new LinearLayout.LayoutParams(-1, 0, 1));

    host.composeColorPanel(root);
    host.updateWorkspaceChrome();
    host.composeStatusBar(root);
    host.setContentView(root);
    host.syncToolState();
    host.updateStatus();
    if (freshProcess && !EditorEntry.hasRequest(host.getIntent())) host.recoverDocument();
  }

  void updateWorkspaceChrome() {
    android.content.res.Configuration config = host.getResources().getConfiguration();
    WorkspaceLayout layout =
        new WorkspaceLayout(
            Math.max(1, config.screenWidthDp),
            Math.max(1, config.screenHeightDp),
            config.fontScale);
    if (host.readPaletteScroll() != null) host.readPaletteScroll().setVisibility(View.VISIBLE);
  }

  void togglePanel(ScrollView panel) {
    if (panel == null) return;
    boolean show = panel.getVisibility() != View.VISIBLE;
    if (show && !host.readPanelsInline()) {
      host.readToolScroll().setVisibility(View.GONE);
      host.readLayerScroll().setVisibility(View.GONE);
    }
    panel.setVisibility(show ? View.VISIBLE : View.GONE);
  }

  void detach(View view) {
    if (view.getParent() instanceof android.view.ViewGroup)
      ((android.view.ViewGroup) view.getParent()).removeView(view);
  }

  void layoutWorkspace() {
    android.content.res.Configuration config = host.getResources().getConfiguration();
    WorkspaceLayout layout =
        new WorkspaceLayout(
            Math.max(1, config.screenWidthDp),
            Math.max(1, config.screenHeightDp),
            config.fontScale);
    host.writePanelsInline(layout.inline);
    host.updateWorkspaceChrome();
    host.detach(host.readDrawing());
    host.detach(host.readToolScroll());
    host.detach(host.readLayerScroll());
    host.readWorkspaceFrame().removeAllViews();
    if (host.readPanelsInline()) {
      LinearLayout row = host.row();
      row.addView(host.readToolScroll(), new LinearLayout.LayoutParams(host.dp(layout.toolsWidth), -1));
      row.addView(host.readDrawing(), new LinearLayout.LayoutParams(0, -1, 1));
      row.addView(host.readLayerScroll(), new LinearLayout.LayoutParams(host.dp(layout.layersWidth), -1));
      host.readWorkspaceFrame().addView(row, new android.widget.FrameLayout.LayoutParams(-1, -1));
      host.readToolScroll().setVisibility(View.VISIBLE);
      host.readLayerScroll().setVisibility(View.VISIBLE);
    } else {
      host.readWorkspaceFrame().addView(host.readDrawing(), new android.widget.FrameLayout.LayoutParams(-1, -1));
      host.readWorkspaceFrame().addView(
          host.readToolScroll(),
          new android.widget.FrameLayout.LayoutParams(host.dp(layout.toolsWidth), -1, Gravity.START));
      host.readWorkspaceFrame().addView(
          host.readLayerScroll(),
          new android.widget.FrameLayout.LayoutParams(host.dp(layout.layersWidth), -1, Gravity.END));
      host.readToolScroll().setVisibility(View.GONE);
      host.readLayerScroll().setVisibility(View.GONE);
    }
    host.readToolScroll().setBackgroundColor(0xFF2B2B2B);
    host.readLayerScroll().setBackgroundColor(0xFF202020);
    host.readToolScroll().setElevation(host.dp(6));
    host.readLayerScroll().setElevation(host.dp(6));
  }
}
