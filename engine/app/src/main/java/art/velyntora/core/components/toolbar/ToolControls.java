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


/** Owns components/toolbar behavior; the host is the workspace composition facade. */
final class ToolControls {
  private final MainActivity host;
  private Button figurasButton;
  private PlacedImageControls placedImageControls;
  private TextView sizeLabel;
  private SeekBar sizeSlider;
  private boolean syncingSize;

  private void syncSizeControl() {
    if (sizeSlider == null) return;
    int tool=host.readDrawing().currentTool();
    boolean wand = tool == DrawingView.MAGIC_WAND;
    int visibility=wand || ToolWidthPolicy.usesWidth(tool)?View.VISIBLE:View.GONE;
    sizeLabel.setVisibility(visibility);
    sizeSlider.setVisibility(visibility);
    int value = wand ? host.readDrawing().readWandTolerance() : Math.round(host.readDrawing().brushRadius() * 2);
    syncingSize = true;
    sizeSlider.setContentDescription(wand ? "Tolerancia" : "Anchura del pincel en píxeles");
    sizeSlider.setMax(wand ? 255 : 299);
    sizeSlider.setProgress(wand ? value : value - 1);
    sizeLabel.setText(wand ? "Tolerancia: " + value : "Anchura del pincel: " + value + " px");
    syncingSize = false;
  }
  ToolControls(MainActivity host) { this.host = host; }

  void configureGradient() {
    LinearLayout form = new LinearLayout(host);
    form.setOrientation(LinearLayout.VERTICAL);
    android.widget.Spinner type = new android.widget.Spinner(host);
    type.setAdapter(
        new android.widget.ArrayAdapter<>(
            host,
            android.R.layout.simple_spinner_dropdown_item,
            new String[] {"Lineal", "Radial", "Lineal reflejado", "Diamante", "Cónico"}));
    type.setSelection(host.readDrawing().gradientMode());
    type.setContentDescription("Geometría del degradado");
    form.addView(type);
    android.widget.CheckBox transparent = new android.widget.CheckBox(host);
    transparent.setText("Color primario a transparente");
    transparent.setChecked(host.readDrawing().gradientTransparent());
    form.addView(transparent);
    form.addView(host.text("Desactivado: color primario a secundario. Respeta alfa y opacidad."));
    new AlertDialog.Builder(host)
        .setTitle("Degradado")
        .setView(host.scrollForm(form))
        .setNegativeButton("Cancelar", null)
        .setPositiveButton(
            "Aplicar",
            (d, w) ->
                host.readDrawing().configureGradient(type.getSelectedItemPosition(), transparent.isChecked()))
        .show();
  }

  void tool(android.widget.GridLayout parent, String label, int tool) {
    Button iconButton = new Button(host);
    iconButton.setText(label);
    iconButton.setTextSize(12);
    iconButton.setAllCaps(false);
    iconButton.setMinWidth(0);
    iconButton.setMinimumWidth(0);
    iconButton.setBackgroundResource(R.drawable.tool_button_background);
    iconButton.setBackgroundTintList(null);
    iconButton.setSelected(tool == host.readDrawing().currentTool());
    host.readToolButtons().put(tool, iconButton);
    host.readToolLabels().put(tool, label);
    android.graphics.drawable.Drawable graphic = Tools.icon(tool, host);
    graphic.setBounds(0, 0, host.dp(24), host.dp(24));
    iconButton.setCompoundDrawables(null, graphic, null, null);
    iconButton.setText("");
    iconButton.setPadding(0, host.dp(12), 0, host.dp(12));
    iconButton.setContentDescription(label);
    if (android.os.Build.VERSION.SDK_INT >= 26) iconButton.setTooltipText(label);
    if(tool==DrawingView.RECTANGLE){
      figurasButton=iconButton;
      for(int[] group:new int[][]{FigurasModule.SIN_RELLENO,FigurasModule.CON_RELLENO})for(int id:group){
        host.readToolButtons().put(id,iconButton);host.readToolLabels().put(id,FigurasModule.label(id));
      }
    }
    if (tool == DrawingView.GRADIENT)
      iconButton.setOnLongClickListener(
          v -> {
            if (host.readProjectProgress() != null) return true;
            host.configureGradient();
            return true;
          });
    if (tool == DrawingView.LINE)
      iconButton.setOnLongClickListener(
          v -> {
            if (host.readProjectProgress() == null) host.readDrawing().confirmCurve();
            return true;
          });
    iconButton.setOnClickListener(
        v -> {
          if (host.readProjectProgress() != null || host.readDrawing().placedImage().active()) return;
          if(tool==DrawingView.RECTANGLE){
            FigurasSelector.show(iconButton,host.readDrawing(),()->{
              host.readDrawing().setColor(host.readActiveColor());host.syncToolState();
              if(!host.readPanelsInline() && host.readToolScroll()!=null)host.readToolScroll().setVisibility(View.GONE);
            });
            return;
          }
          host.readDrawing().setTool(tool);
          host.readDrawing().setColor(host.readActiveColor());
          host.syncToolState();
          if (tool == DrawingView.BRUSH)
            BrushSelector.show(iconButton,host.readDrawing(),host::syncToolState);
          if (tool != DrawingView.BRUSH && !host.readPanelsInline() && host.readToolScroll() != null) host.readToolScroll().setVisibility(View.GONE);
        });
    android.widget.GridLayout.LayoutParams cell = new android.widget.GridLayout.LayoutParams();
    cell.width = host.dp(48);
    cell.height = host.dp(48);
    parent.addView(iconButton, cell);
  }

  void syncToolState() {
    if (host.readDrawing() == null) return;
    int active = host.readDrawing().currentTool();
    for (int i = 0; i < host.readToolButtons().size(); i++) {
      boolean selected = host.readToolButtons().keyAt(i) == active;
      Button button = host.readToolButtons().valueAt(i);
      if(button==figurasButton)selected=FigurasModule.contains(active);
      button.setSelected(selected);
      if (android.os.Build.VERSION.SDK_INT >= 30)
        button.setStateDescription(selected ? "Activa" : "");
    }
    if(figurasButton!=null){
      int figure=FigurasModule.selected(host.readDrawing());
      android.graphics.drawable.Drawable icon=Tools.icon(figure,host);icon.setBounds(0,0,host.dp(24),host.dp(24));
      figurasButton.setCompoundDrawables(null,icon,null,null);
      figurasButton.setContentDescription("Figuras: "+FigurasModule.label(figure));
      if(android.os.Build.VERSION.SDK_INT>=26)figurasButton.setTooltipText("Figuras: "+FigurasModule.label(figure));
    }
    if (host.readSelectedTool() != null && host.readToolLabels().get(active) != null)
      host.readSelectedTool().setText(host.readToolLabels().get(active).split(":", 2)[0]);
    if (active == DrawingView.BRUSH && host.readSelectedTool() != null)
      host.readSelectedTool().setText(host.readDrawing().brushes().label());
    syncSizeControl();
    if (host.readGradientOptions() != null)
      host.readGradientOptions().setVisibility(active == DrawingView.GRADIENT ? View.VISIBLE : View.GONE);
    if (host.readCurveConfirm() != null)
      host.readCurveConfirm().setVisibility(active == DrawingView.LINE ? View.VISIBLE : View.GONE);
    if (host.readCurveCancel() != null)
      host.readCurveCancel().setVisibility(active == DrawingView.LINE ? View.VISIBLE : View.GONE);
    if(placedImageControls!=null)placedImageControls.sync();
  }

  void configureBrush() {
    LinearLayout form = new LinearLayout(host);
    form.setOrientation(LinearLayout.VERTICAL);
    SeekBar opacity = new SeekBar(host), hardness = new SeekBar(host);
    opacity.setContentDescription("Opacidad de la herramienta");
    opacity.setMax(100);
    opacity.setProgress(Math.round(host.readDrawing().brushOpacity() * 100));
    hardness.setMax(100);
    hardness.setProgress(Math.round(host.readDrawing().brushHardness() * 100));
    TextView opacityLabel = host.text("Opacidad: " + opacity.getProgress() + " %"),
        hardnessLabel = host.text("Dureza: " + hardness.getProgress() + " %");
    form.addView(opacityLabel);
    form.addView(opacity);
    form.addView(hardnessLabel);
    form.addView(hardness);
    android.widget.CheckBox square = new android.widget.CheckBox(host),
        pressure = new android.widget.CheckBox(host);
    square.setText("Punta cuadrada");
    square.setChecked(host.readDrawing().squareBrush());
    pressure.setText("Presión del lápiz: variar radio");
    pressure.setChecked(host.readDrawing().pressureBrush());
    form.addView(square);
    form.addView(pressure);
    opacity.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {
          public void onProgressChanged(SeekBar s, int n, boolean u) {
            opacityLabel.setText("Opacidad: " + n + " %");
          }

          public void onStartTrackingTouch(SeekBar s) {}

          public void onStopTrackingTouch(SeekBar s) {}
        });
    hardness.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {
          public void onProgressChanged(SeekBar s, int n, boolean u) {
            hardnessLabel.setText("Dureza: " + n + " %");
          }

          public void onStartTrackingTouch(SeekBar s) {}

          public void onStopTrackingTouch(SeekBar s) {}
        });
    new AlertDialog.Builder(host)
        .setTitle("Pincel y borrador")
        .setView(host.scrollForm(form))
        .setNegativeButton("Cancelar", null)
        .setPositiveButton(
            "Aplicar",
            (d, w) ->
                host.readDrawing().configureBrush(
                    opacity.getProgress() / 100f,
                    hardness.getProgress() / 100f,
                    square.isChecked(),
                    pressure.isChecked()))
        .show();
  }

  void create(LinearLayout root) {
    LinearLayout options = host.row();
    host.writeSelectedTool(host.text("Pincel"));
    host.readSelectedTool().setPadding(host.dp(8), 0, host.dp(16), 0);
    host.readSelectedTool().setSingleLine(true);
    host.readSelectedTool().setMaxWidth(host.dp(160));
    options.addView(host.readSelectedTool());

    LinearLayout sidebar = new LinearLayout(host);
    sidebar.setOrientation(LinearLayout.VERTICAL);
    sidebar.setBackgroundColor(0xFF2B2B2B);
    sidebar.setPadding(host.dp(3), host.dp(4), host.dp(3), host.dp(4));

    android.widget.GridLayout toolGrid = new android.widget.GridLayout(host);
    toolGrid.setColumnCount(2);
    sidebar.addView(toolGrid);
    host.tool(toolGrid, "Mover píxeles", DrawingView.MOVE_PIXELS);
    host.tool(toolGrid, "Cubeta", DrawingView.BUCKET);
    host.tool(toolGrid, "Mover contorno", DrawingView.MOVE_SELECTION);
    host.tool(toolGrid, "Degradado: mantener pulsado para configurar", DrawingView.GRADIENT);
    host.tool(toolGrid, "Zoom: toque acerca; toque largo aleja; arrastre vertical", DrawingView.ZOOM);
    host.tool(toolGrid, "Cuentagotas", DrawingView.PICKER);
    host.tool(toolGrid, "Desplazamiento", DrawingView.PAN);
    host.tool(toolGrid, "Texto", DrawingView.TEXT);
    host.tool(toolGrid, "Selección rectangular", DrawingView.SELECT_RECTANGLE);
    host.tool(
        toolGrid,
        "Línea/Curva: arrastrar tiradores; mantener pulsado para confirmar",
        DrawingView.LINE);
    host.tool(toolGrid, "Selección elíptica", DrawingView.SELECT_ELLIPSE);
    host.tool(toolGrid, "Figuras", DrawingView.RECTANGLE);
    host.tool(toolGrid, "Selección libre (contorno)", DrawingView.SELECT_FREE);
    host.tool(toolGrid, "Varita mágica", DrawingView.MAGIC_WAND);
    host.tool(toolGrid, "Pincel", DrawingView.BRUSH);
    host.tool(toolGrid, "Forma libre (contorno cerrado)", DrawingView.FREEFORM);
    host.tool(toolGrid, "Lápiz", DrawingView.PENCIL);
    host.tool(
        toolGrid,
        "Tampón de clonar: primer toque fija origen; seleccionar de nuevo para cambiarlo",
        DrawingView.CLONE);
    host.tool(toolGrid, "Borrador", DrawingView.ERASER);
    host.tool(
        toolGrid,
        "Recoloración: sustituye el color inicial; tolerancia de varita",
        DrawingView.RECOLOR);
    host.tool(toolGrid, "Remove AI: pinta el objeto y pulsa Eliminar", DrawingView.REMOVE_AI);
    TextView brushSizeLabel = host.text("Anchura del pincel: " + Math.round(host.readDrawing().brushRadius() * 2) + " px");
    brushSizeLabel.setSingleLine(true);
    // Reserve the widest value so changing digit counts cannot move the slider.
    int widthLabelSpace = (int) Math.ceil(
        brushSizeLabel.getPaint().measureText("Anchura del pincel: 888 px"))
        + brushSizeLabel.getPaddingLeft() + brushSizeLabel.getPaddingRight() + host.dp(8);
    options.addView(brushSizeLabel, new LinearLayout.LayoutParams(widthLabelSpace, -2));
    SeekBar brushSize = new SeekBar(host);
    sizeLabel = brushSizeLabel;
    sizeSlider = brushSize;
    brushSize.setContentDescription("Anchura del pincel en píxeles");
    brushSize.setMax(299);
    brushSize.setProgress(Math.round(host.readDrawing().brushRadius() * 2) - 1);
    // The enclosing horizontal toolbar must not steal this slider's drag.
    brushSize.setOnTouchListener((view, event) -> {
      int action = event.getActionMasked();
      if (action == android.view.MotionEvent.ACTION_DOWN)
        view.getParent().requestDisallowInterceptTouchEvent(true);
      else if (action == android.view.MotionEvent.ACTION_UP
          || action == android.view.MotionEvent.ACTION_CANCEL)
        view.getParent().requestDisallowInterceptTouchEvent(false);
      return false;
    });
    brushSize.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {
          @Override
          public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
            if (syncingSize) return;
            if (host.readDrawing().currentTool() == DrawingView.MAGIC_WAND) {
              host.writeWandTolerance(progress);
              host.readDrawing().setWandTolerance(progress);
              brushSizeLabel.setText("Tolerancia: " + progress);
              return;
            }
            int size = progress + 1;
            brushSizeLabel.setText("Anchura del pincel: " + size + " px");
            host.readDrawing().setBrushRadius(size / 2f);
          }

          @Override
          public void onStartTrackingTouch(SeekBar bar) {}

          @Override
          public void onStopTrackingTouch(SeekBar bar) {}
        });
    host.readDrawing().brushWidthChanged = this::syncSizeControl;
    syncSizeControl();
    options.addView(brushSize, new LinearLayout.LayoutParams(host.dp(120), host.dp(48)));
    OpacityControls.attach(host, host.readDrawing(), options);
    host.iconButton(options, "Configurar pincel", "settings", host::configureBrush);
    host.writeGradientOptions(host.iconButton(options, "Configurar degradado", "gradient", host::configureGradient));
    host.writeCurveConfirm(host.iconButton(options, "Confirmar Línea/Curva", "confirm", host.readDrawing()::confirmCurve));
    host.writeCurveCancel(host.iconButton(options, "Cancelar Línea/Curva", "cancel", host.readDrawing()::cancelCurve));
    SelectionConfirmationControls.attach(host,options);
    host.readDrawing().removeAi().controls(options);
    placedImageControls=new PlacedImageControls(host,options);
    host.addScrollable(root, options);
    ScrollView toolScroll = new ScrollView(host);
    toolScroll.addView(sidebar);
    host.writeToolScroll(toolScroll);

  }
}
