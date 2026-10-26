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

/** Android workspace modeled after Pinta's tool, canvas, palette and status regions. */
public final class MainActivity extends Activity {
  private final DocumentSession documentSession = new DocumentSession(this);
  private final StatusBar statusBar = new StatusBar(this);
  private final SelectionActions selectionActions = new SelectionActions(this);
  private final EffectsPanel effectsPanel = new EffectsPanel(this);
  private final WidgetFactory widgetFactory = new WidgetFactory(this);
  private final WorkspaceComposition workspaceComposition = new WorkspaceComposition(this);
  private final ColorPanel colorPanel = new ColorPanel(this);
  private final ToolControls toolControls = new ToolControls(this);
  private final WorkspaceMenus workspaceMenus = new WorkspaceMenus(this);
  private final HistoryActions historyActions = new HistoryActions(this);
  private final LayersPanel layersPanel = new LayersPanel(this);
  private final DocumentStorage documentStorage = new DocumentStorage(this);
  private final TextDialog textDialog = new TextDialog(this);
  private final DocumentDimensionsDialog documentDimensionsDialog = new DocumentDimensionsDialog(this);

  private static final int SAVE_PROJECT = 43,
      OPEN_PROJECT = 44,
      SAVE_JPEG = 45,
      SAVE_WEBP = 46,
      SAVE_BMP = 47,
      SAVE_TGA = 48,
      OPEN_TGA = 49,
      SAVE_ORA = 50,
      OPEN_ORA = 51,
      SAVE_TIFF = 52,
      OPEN_TIFF = 53,
      SAVE_GIF = 54,
      OPEN_ICO = 55,
      SAVE_ICO = 56,
      OPEN_PPM = 57,
      SAVE_PPM = 58,
      OPEN_BMP = 59,
      OPEN_PCX = 60,
      OPEN_XBM = 61,
      OPEN_XPM = 62,
      OPEN_SVG = 63,
      OPEN_CONTAINER = 64;
  private static final int SAVE_PNG = 41;
  private static final int OPEN_IMAGE = 42;
  private DrawingView drawing;
  private TextView status, documentTitle;
  private Button gradientOptions, curveConfirm, curveCancel;
  private LinearLayout menuRegistry;
  private TextView selectedTool;
  private LinearLayout layerItems;
  private ScrollView layerScroll, toolScroll;
  private android.widget.FrameLayout workspaceFrame;
  private boolean panelsInline;
  private HorizontalScrollView commandScroll, paletteScroll;
  private Button currentColorButton, secondaryColorButton;
  private final android.util.SparseArray<String> toolLabels = new android.util.SparseArray<>();
  private final android.util.SparseArray<Button> toolButtons = new android.util.SparseArray<>();
  private final java.util.ArrayList<Bitmap> thumbnails = new java.util.ArrayList<>();
  private android.os.Handler layerRefreshHandler =
      new android.os.Handler(android.os.Looper.getMainLooper());
  private SeekBar layerOpacity;
  private Bitmap selectionClipboard;
  private android.app.ProgressDialog projectProgress;
  private static long savedRevision = -1;
  private static String documentName = "Sin título";
  private static final java.util.concurrent.ExecutorService recoveryExecutor =
      java.util.concurrent.Executors.newSingleThreadExecutor();

  boolean dirty() {
    return documentSession.dirty();
  }

  void markDocumentClean(String name) {
    documentSession.markDocumentClean(name);
  }

  void updateStatus() {
    statusBar.updateStatus();
  }

  void protectDocument(Runnable action) {
    documentSession.protectDocument(action);
  }

  @Override
  public boolean onKeyDown(int key, android.view.KeyEvent event) {
    if(drawing!=null && drawing.placedImage().active()){
      if(projectProgress!=null)return true;
      if(key==android.view.KeyEvent.KEYCODE_ENTER){drawing.placedImage().confirm();return true;}
      if(key==android.view.KeyEvent.KEYCODE_ESCAPE){drawing.placedImage().cancel();return true;}
      if(event.isCtrlPressed()){
        if(key==android.view.KeyEvent.KEYCODE_Z){if(event.isShiftPressed())drawing.placedImage().redo();else drawing.placedImage().undo();}
        else if(key==android.view.KeyEvent.KEYCODE_Y)drawing.placedImage().redo();
        return true;
      }
    }
    if (event.isCtrlPressed()) {
      if (projectProgress != null) return true;
      switch (key) {
        case android.view.KeyEvent.KEYCODE_Z:
          if (event.isShiftPressed()) redo();
          else undo();
          return true;
        case android.view.KeyEvent.KEYCODE_Y:
          redo();
          return true;
        case android.view.KeyEvent.KEYCODE_S:
          projectPicker(true);
          return true;
        case android.view.KeyEvent.KEYCODE_O:
          openImage();
          return true;
        case android.view.KeyEvent.KEYCODE_N:
          protectDocument(() -> configureDimensions(0));
          return true;
        case android.view.KeyEvent.KEYCODE_A:
          drawing.selectAll();
          return true;
        case android.view.KeyEvent.KEYCODE_D:
          drawing.deselect();
          return true;
        case android.view.KeyEvent.KEYCODE_C:
          copySelection();
          return true;
        case android.view.KeyEvent.KEYCODE_X:
          cutSelection();
          return true;
        case android.view.KeyEvent.KEYCODE_V:
          pasteSelection();
          return true;
      }
    }
    return super.onKeyDown(key, event);
  }

  @Override
  public void onBackPressed() {
    if(drawing!=null && drawing.placedImage().active()){drawing.placedImage().cancel();return;}
    protectDocument(() -> super.onBackPressed());
  }

  @Override
  protected void onStop() {
    super.onStop();
    documentSession.saveRecovery();
  }

  void recoverDocument() {
    documentSession.recoverDocument();
  }

  void copySelection() {
    selectionActions.copySelection();
  }

  void moveSelectedContent() {
    selectionActions.moveSelectedContent();
  }

  /** User-configurable brightness, with one native undo checkpoint per application. */
  void configureBrightness() {
    effectsPanel.configureBrightness();
  }

  /** Configure contrast without changing pixels until Apply is pressed. */
  void configureContrast() {
    effectsPanel.configureContrast();
  }

  /** Choose a binary threshold without applying it during slider movement. */
  void configureThreshold() {
    effectsPanel.configureThreshold();
  }

  /** Configure saturation with a single undoable apply operation. */
  void configureSaturation() {
    effectsPanel.configureSaturation();
  }

  /** Configure gamma percentage; no change is made until Apply. */
  void configureGamma() {
    effectsPanel.configureGamma();
  }

  /** Adjustable posterization without modifying the document until confirmation. */
  void configurePosterization() {
    effectsPanel.configurePosterization();
  }

  /** Set the solarization threshold; applying is a single undoable operation. */
  void configureSolarization() {
    effectsPanel.configureSolarization();
  }

  /** Set individual red, green, and blue offsets on the active layer. */
  void configureRgbOffsets() {
    effectsPanel.configureRgbOffsets();
  }

  /** Edit black and white points before applying the levels operation. */
  void configureLevels() {
    effectsPanel.configureLevels();
  }

  /** Configure exposure without modifying the document until Apply. */
  void configureExposure() {
    effectsPanel.configureExposure();
  }

  /** Rotate hue by a selected angle without applying during slider movement. */
  void configureHue() {
    effectsPanel.configureHue();
  }

  /** Independently scale red, green and blue channels using one undoable operation. */
  void configureRgbBalance() {
    effectsPanel.configureRgbBalance();
  }

  /** Choose quantization step; only apply on confirmation. */
  void configureQuantization() {
    effectsPanel.configureQuantization();
  }

  /** Choose the maximum RGB channel value for highlight limiting. */
  void configureHighlightCeiling() {
    effectsPanel.configureHighlightCeiling();
  }

  private int wandTolerance = 0;

  void configureWandTolerance() {
    selectionActions.configureWandTolerance();
  }

  void duplicateSelection() {
    selectionActions.duplicateSelection();
  }

  void pasteSelection() {
    selectionActions.pasteSelection();
  }

  void cutSelection() {
    selectionActions.cutSelection();
  }

  int dp(int value) {
    return widgetFactory.dp(value);
  }

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    workspaceComposition.onCreate(state);
    EditorEntry.enter(this, state);
    EditorEntry.immersive(getWindow());
  }

  @Override public void onWindowFocusChanged(boolean focus) {
    super.onWindowFocusChanged(focus);
    if (focus) EditorEntry.immersive(getWindow());
  }

  void configureColor() {
    colorPanel.configureColor();
  }

  void configureColor(boolean secondary) {
    colorPanel.configureColor(secondary);
  }

  void configureGradient() {
    toolControls.configureGradient();
  }

  void setSecondaryColor(int value) {
    colorPanel.setSecondaryColor(value);
  }

  void setActiveColor(int color) {
    colorPanel.setActiveColor(color);
  }

  void updateWorkspaceChrome() {
    workspaceComposition.updateWorkspaceChrome();
  }

  void togglePanel(ScrollView panel) {
    workspaceComposition.togglePanel(panel);
  }

  void detach(View view) {
    workspaceComposition.detach(view);
  }

  void layoutWorkspace() {
    workspaceComposition.layoutWorkspace();
  }

  @Override
  public void onConfigurationChanged(android.content.res.Configuration config) {
    super.onConfigurationChanged(config);
    if (workspaceFrame != null) layoutWorkspace();
  }

  View scrollForm(View form) {
    return widgetFactory.scrollForm(form);
  }

  LinearLayout row() {
    return widgetFactory.row();
  }

  TextView text(String label) {
    return widgetFactory.text(label);
  }

  HorizontalScrollView addScrollable(LinearLayout root, LinearLayout content) {
    return widgetFactory.addScrollable(root,content);
  }

  Button button(LinearLayout parent, String label, Runnable action) {
    return widgetFactory.button(parent,label,action);
  }

  void tool(android.widget.GridLayout parent, String label, int tool) {
    toolControls.tool(parent,label,tool);
  }

  void syncToolState() {
    toolControls.syncToolState();
  }

  Button iconButton(LinearLayout parent, String label, String glyph, Runnable action) {
    return widgetFactory.iconButton(parent,label,glyph,action);
  }

  void openMainMenu() {
    workspaceMenus.openMainMenu();
  }

  void menu(LinearLayout parent, String title, String[] labels, Runnable[] actions) {
    workspaceMenus.menu(parent,title,labels,actions);
  }

  void undo() {
    historyActions.undo();
  }

  void redo() {
    historyActions.redo();
  }

  private final Runnable layerRefreshTask = this::refreshLayerPanel;

  @Override
  protected void onDestroy() {
    if(drawing!=null)drawing.placedImage().dispose();
    if (projectProgress != null) {
      projectProgress.dismiss();
      projectProgress = null;
    }
    layerRefreshHandler.removeCallbacks(layerRefreshTask);
    if (layerItems != null) layerItems.removeAllViews();
    for (Bitmap old : thumbnails) old.recycle();
    thumbnails.clear();
    super.onDestroy();
  }

  void refreshLayerPanel() {
    layersPanel.refreshLayerPanel();
  }

  void addLayer() {
    layersPanel.addLayer();
  }

  void renameLayer(int index) {
    layersPanel.renameLayer(index);
  }

  void chooseLayer() {
    layersPanel.chooseLayer();
  }

  void deleteLayer() {
    layersPanel.deleteLayer();
  }

  void toggleLayer() {
    layersPanel.toggleLayer();
  }

  void moveLayer(int direction) {
    layersPanel.moveLayer(direction);
  }

  void message(String text) {
    widgetFactory.message(text);
  }

  /** Decode with a bounded memory footprint, even for very large source images. */
  Bitmap decodeImage(Uri uri) throws java.io.IOException {
    return documentStorage.decodeImage(uri);
  }

  void configureSelectionTransform() {
    selectionActions.configureSelectionTransform();
  }

  void configureEffect(int kind) {
    effectsPanel.configureEffect(kind);
  }

  void runEffect(int kind, int amount) {
    effectsPanel.runEffect(kind,amount);
  }

  interface ColorOperation {
    boolean getAsBoolean();
  }

  void runColorOperation(ColorOperation operation) {
    effectsPanel.runColorOperation(operation);
  }

  void configureText(int x, int y) {
    textDialog.configureText(x,y);
  }

  void configureBrush() {
    toolControls.configureBrush();
  }

  void resizeDocumentAsync(int w, int h, boolean scale, boolean bilinear, int anchor) {
    documentDimensionsDialog.resizeDocumentAsync(w,h,scale,bilinear,anchor);
  }

  android.text.TextWatcher dimensionWatcher(Runnable change) {
    return documentDimensionsDialog.dimensionWatcher(change);
  }

  void configureDimensions(int mode) {
    documentDimensionsDialog.configureDimensions(mode);
  }

  void saveImage(String mime, String name, int request) {
    documentStorage.saveImage(mime,name,request);
  }

  void projectPicker(boolean save) {
    documentStorage.projectPicker(save);
  }

  void openRasterPicker(boolean save) {
    documentStorage.openRasterPicker(save);
  }

  void transferOpenRaster(Uri uri, boolean save) {
    documentStorage.transferOpenRaster(uri,save);
  }

  void transferProject(Uri uri, boolean save) {
    documentStorage.transferProject(uri,save);
  }

  void exportRaster(Uri uri, int request) {
    documentStorage.exportRaster(uri,request);
  }

  void openTga() {
    documentStorage.openTga();
  }

  void openTiff() {
    documentStorage.openTiff();
  }

  void openIco() {
    documentStorage.openIco();
  }

  void openPpm() {
    documentStorage.openPpm();
  }

  void importTga(Uri uri) {
    documentStorage.importTga(uri);
  }

  void importRaster(Uri uri, int request) {
    documentStorage.importRaster(uri,request);
  }

  void savePng() {
    documentStorage.savePng();
  }

  android.app.AlertDialog readImportConfirmation() { return documentStorage.importConfirmation(); }

  void importImage(Uri uri) {
    documentStorage.importImage(uri);
  }

  private final java.util.concurrent.ConcurrentHashMap<String, String> fileNames =
      new java.util.concurrent.ConcurrentHashMap<>();

  String fileName(Uri uri) {
    return documentStorage.fileName(uri);
  }

  IconContainers.Image decodeContainerImage(byte[] encoded) throws java.io.IOException {
    return documentStorage.decodeContainerImage(encoded);
  }

  void openDetected(Uri uri) {
    documentStorage.openDetected(uri);
  }

  void openImage() {
    documentStorage.openImage();
  }

  @Override
  protected void onActivityResult(int request, int result, Intent data) {
    super.onActivityResult(request, result, data);
    if (result != RESULT_OK || data == null || data.getData() == null) return;
    Uri uri = data.getData();
    if (request == OPEN_ORA
        || request == OPEN_PROJECT
        || request == OPEN_TIFF
        || request == OPEN_PPM
        || request == OPEN_ICO
        || request == OPEN_TGA
        || request == OPEN_IMAGE) {
      protectDocument(() -> acceptFileResult(request, uri));
      return;
    }
    if(request==PlacedImageSession.PICK_IMAGE){drawing.placedImage().load(uri);return;}
    acceptFileResult(request, uri);
  }

  void acceptFileResult(int request, Uri uri) {
    documentStorage.acceptFileResult(request,uri);
  }

  // Workspace composition API: components do not access host fields directly.
  art.velyntora.core.DrawingView readDrawing() { return drawing; }
  art.velyntora.core.DrawingView writeDrawing(art.velyntora.core.DrawingView value) { drawing = value; return value; }
  long readSavedRevision() { return savedRevision; }
  long writeSavedRevision(long value) { savedRevision = value; return value; }
  android.os.Handler readLayerRefreshHandler() { return layerRefreshHandler; }
  android.os.Handler writeLayerRefreshHandler(android.os.Handler value) { layerRefreshHandler = value; return value; }
  java.lang.Runnable readLayerRefreshTask() { return layerRefreshTask; }
  int readSAVE_JPEG() { return SAVE_JPEG; }
  int readSAVE_WEBP() { return SAVE_WEBP; }
  int readSAVE_BMP() { return SAVE_BMP; }
  int readSAVE_TGA() { return SAVE_TGA; }
  int readSAVE_TIFF() { return SAVE_TIFF; }
  int readSAVE_GIF() { return SAVE_GIF; }
  int readSAVE_ICO() { return SAVE_ICO; }
  int readSAVE_PPM() { return SAVE_PPM; }
  android.widget.ScrollView readToolScroll() { return toolScroll; }
  android.widget.ScrollView writeToolScroll(android.widget.ScrollView value) { toolScroll = value; return value; }
  android.widget.ScrollView readLayerScroll() { return layerScroll; }
  android.widget.ScrollView writeLayerScroll(android.widget.ScrollView value) { layerScroll = value; return value; }
  int readActiveColor() { return colorPanel.primary(); }
  int writeActiveColor(int value) { return colorPanel.updatePrimary(value); }
  int readSecondaryColor() { return colorPanel.secondary(); }
  int writeSecondaryColor(int value) { return colorPanel.updateSecondary(value); }
  android.widget.LinearLayout readMenuRegistry() { return menuRegistry; }
  android.widget.LinearLayout writeMenuRegistry(android.widget.LinearLayout value) { menuRegistry = value; return value; }
  android.widget.TextView readDocumentTitle() { return documentTitle; }
  android.widget.TextView writeDocumentTitle(android.widget.TextView value) { documentTitle = value; return value; }
  android.widget.TextView readSelectedTool() { return selectedTool; }
  android.widget.TextView writeSelectedTool(android.widget.TextView value) { selectedTool = value; return value; }
  android.widget.Button readGradientOptions() { return gradientOptions; }
  android.widget.Button writeGradientOptions(android.widget.Button value) { gradientOptions = value; return value; }
  android.widget.Button readCurveConfirm() { return curveConfirm; }
  android.widget.Button writeCurveConfirm(android.widget.Button value) { curveConfirm = value; return value; }
  android.widget.Button readCurveCancel() { return curveCancel; }
  android.widget.Button writeCurveCancel(android.widget.Button value) { curveCancel = value; return value; }
  android.app.ProgressDialog readProjectProgress() { return projectProgress; }
  android.app.ProgressDialog writeProjectProgress(android.app.ProgressDialog value) { projectProgress = value; return value; }
  android.widget.SeekBar readLayerOpacity() { return layerOpacity; }
  android.widget.SeekBar writeLayerOpacity(android.widget.SeekBar value) { layerOpacity = value; return value; }
  android.widget.LinearLayout readLayerItems() { return layerItems; }
  android.widget.LinearLayout writeLayerItems(android.widget.LinearLayout value) { layerItems = value; return value; }
  android.widget.FrameLayout readWorkspaceFrame() { return workspaceFrame; }
  android.widget.FrameLayout writeWorkspaceFrame(android.widget.FrameLayout value) { workspaceFrame = value; return value; }
  android.widget.Button readCurrentColorButton() { return currentColorButton; }
  android.widget.Button writeCurrentColorButton(android.widget.Button value) { currentColorButton = value; return value; }
  android.widget.Button readSecondaryColorButton() { return secondaryColorButton; }
  android.widget.Button writeSecondaryColorButton(android.widget.Button value) { secondaryColorButton = value; return value; }
  android.widget.HorizontalScrollView readPaletteScroll() { return paletteScroll; }
  android.widget.HorizontalScrollView writePaletteScroll(android.widget.HorizontalScrollView value) { paletteScroll = value; return value; }
  android.widget.TextView readStatus() { return status; }
  android.widget.TextView writeStatus(android.widget.TextView value) { status = value; return value; }
  boolean readPanelsInline() { return panelsInline; }
  boolean writePanelsInline(boolean value) { panelsInline = value; return value; }
  java.util.ArrayList<android.graphics.Bitmap> readThumbnails() { return thumbnails; }
  int readSAVE_PROJECT() { return SAVE_PROJECT; }
  int readOPEN_PROJECT() { return OPEN_PROJECT; }
  int readSAVE_ORA() { return SAVE_ORA; }
  int readOPEN_ORA() { return OPEN_ORA; }
  int readOPEN_TGA() { return OPEN_TGA; }
  int readOPEN_TIFF() { return OPEN_TIFF; }
  int readOPEN_ICO() { return OPEN_ICO; }
  int readOPEN_PPM() { return OPEN_PPM; }
  int readOPEN_BMP() { return OPEN_BMP; }
  int readOPEN_PCX() { return OPEN_PCX; }
  int readOPEN_XBM() { return OPEN_XBM; }
  int readOPEN_XPM() { return OPEN_XPM; }
  int readOPEN_SVG() { return OPEN_SVG; }
  int readOPEN_CONTAINER() { return OPEN_CONTAINER; }
  int readSAVE_PNG() { return SAVE_PNG; }
  java.util.concurrent.ConcurrentHashMap<java.lang.String,java.lang.String> readFileNames() { return fileNames; }
  int readOPEN_IMAGE() { return OPEN_IMAGE; }
  android.util.SparseArray<android.widget.Button> readToolButtons() { return toolButtons; }
  android.util.SparseArray<java.lang.String> readToolLabels() { return toolLabels; }
  java.lang.String readDocumentName() { return documentName; }
  java.lang.String writeDocumentName(java.lang.String value) { documentName = value; return value; }
  java.util.concurrent.ExecutorService readRecoveryExecutor() { return recoveryExecutor; }
  android.graphics.Bitmap readSelectionClipboard() { return selectionClipboard; }
  android.graphics.Bitmap writeSelectionClipboard(android.graphics.Bitmap value) { selectionClipboard = value; return value; }
  int readWandTolerance() { return wandTolerance; }
  int writeWandTolerance(int value) { wandTolerance = value; return value; }
  void composeColorPanel(LinearLayout root) { colorPanel.create(root); }
  void composeLayersPanel(LinearLayout root) { layersPanel.create(root); }
  void composeToolControls(LinearLayout root) { toolControls.create(root); }
  void composeStatusBar(LinearLayout root) { statusBar.create(root); }
  void composeWorkspaceMenus() { workspaceMenus.createRegistry(); }
}
