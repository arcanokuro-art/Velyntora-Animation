package art.velyntora.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

public final class DrawingView extends View {
  final CanvasEffects canvasEffects = new CanvasEffects(this);
  final CanvasSelection canvasSelection = new CanvasSelection(this);
  final CanvasDocument canvasDocument = new CanvasDocument(this);
  final CanvasViewport canvasViewport = new CanvasViewport(this);
  final CanvasInput canvasInput = new CanvasInput(this);
  final LayerOperations layerOperations = new LayerOperations(this);
  final CanvasRendering canvasRendering = new CanvasRendering(this);

  static {
    System.loadLibrary("velyntora_jni");
  }

  static native float nativeIndexedLayerOpacity(int index);
  static native boolean nativeBeginPlacedImage(String name,float opacity);
  static native void nativeImageStorage(String directory);
  static native boolean nativeLayerIsImage(int index);
  static native float[] nativeImageTransform(int index);
  static native boolean nativeBeginPlacedImageEdit(int index,float opacity);
  static native boolean nativePlacedImageElement(String path,float[] transform);
  static native int[] nativePrepareImageEdit(int index,int fd);
  static native int[] nativeComposeImageEdit(int[] moving,float opacity);
  static native void nativeCloseImageEdit();
  static native boolean nativeRasterizeImage(int index);
  static native int[] nativeTextPreviewStyle();
  static native int[] nativeTextPreviewPlane(int index);
  static native byte[] nativeTextContent(int index);
  static native int[] nativeTextStyle(int index);
  static native boolean nativePlacedTextElement(String path,float[] transform,byte[] content,int[] style);
  static native boolean nativePlacedImageTile(int x,int y,int width,int height,int[] pixels);
  static native boolean nativeCommitPlacedImage();
  static native void nativeCancelPlacedImage();
  private final PlacedImageSession placedImageSession=new PlacedImageSession(this);
  PlacedImageSession placedImage(){return placedImageSession;}
  static native int nativeLayerBlendMode(int index);
  static native boolean nativeLayerAction(int action);
  static native boolean nativeLayerProperties(int index,String name,boolean visible,float opacity,int mode);
  boolean applyLayerProperties(int index,String name,boolean visible,float opacity,int mode){return layerOperations.applyLayerProperties(index,name,visible,opacity,mode);}
  int layerBlendMode(int index){return layerOperations.layerBlendMode(index);}
  float layerOpacity(int index){return layerOperations.layerOpacity(index);}
  boolean layerAction(int action){return layerOperations.layerAction(action);}

  static native boolean nativeTransformSelection(
      int left, int top, int width, int height, byte[] mask, float degrees, float sx, float sy);

  static native boolean nativeEffect(int kind, int amount);

  static native boolean nativeUtility(int kind, int amount, int size, int parameter);

  static native boolean nativeObject(
      int kind, int amount, int tolerance, int color, boolean option);

  public boolean utility(int kind, int amount, int size, int parameter) {
    return canvasEffects.utility(kind,amount,size,parameter);
  }

  public boolean objectEffect(int kind, int amount, int tolerance, boolean option) {
    return canvasEffects.objectEffect(kind,amount,tolerance,option);
  }

  static native boolean nativeRender(
      int kind, int scale, int detail, int seed, int first, int second);

  public boolean render(int kind, int scale, int detail, int seed, int second) {
    return canvasEffects.render(kind,scale,detail,seed,second);
  }

  static native boolean nativeArtistic(int kind, int strength, int radius, int threshold);

  public boolean artistic(int kind, int strength, int radius, int threshold) {
    return canvasEffects.artistic(kind,strength,radius,threshold);
  }

  static native boolean nativeDistortion(
      int kind, int amount, int size, int angle, int cx, int cy);

  public boolean distortion(int kind, int amount, int size, int angle, int cx, int cy) {
    return canvasEffects.distortion(kind,amount,size,angle,cx,cy);
  }

  static native boolean nativeBlur(int kind, int amount, int angle, int cx, int cy);

  public boolean blur(int kind, int amount, int angle, int cx, int cy) {
    return canvasEffects.blur(kind,amount,angle,cx,cy);
  }

  static native boolean nativeColorAdjustment(int kind, int[] values);

  public boolean colorAdjustment(int kind, int[] values) {
    return canvasEffects.colorAdjustment(kind,values);
  }

  public boolean applyEffect(int kind, int amount) {
    return canvasEffects.applyEffect(kind,amount);
  }

  static native boolean nativeSetEffectSelection(byte[] mask);

  byte[] selectionMask() {
    return canvasSelection.selectionMask();
  }

  public boolean prepareEffectSelection() {
    return canvasEffects.prepareEffectSelection();
  }

  public void effectApplied() {
    canvasEffects.effectApplied();
  }

  static native boolean nativeSaveProject(int fd);

  static native boolean nativeOpenProject(int fd);

  static native boolean nativeSaveRecovery(int fd, long expectedRevision);

  public boolean writeRecovery(int fd, long expectedRevision) {
    return canvasDocument.writeRecovery(fd,expectedRevision);
  }

  public boolean writeProject(int fd) {
    return canvasDocument.writeProject(fd);
  }

  public boolean readProject(int fd) {
    return canvasDocument.readProject(fd);
  }

  public void projectOpened() {
    canvasDocument.projectOpened();
  }

  static native boolean nativeCreate(int w, int h);
  static native boolean nativeBeginImageImport(int w,int h);
  static native boolean nativeImageImportRegion(int x,int y,int w,int h,int[] pixels);
  static native boolean nativeFinishImageImport();
  static native void nativeCancelImageImport();
  static native void nativeAcceptImageImport();
  static native int[] nativeSampleRegion(int x,int y,int sample,int width,int height);

  static native void nativeHistoryDirectory(String directory);
  static native void nativeBeginEdit();

  static native boolean nativeClear();

  static native boolean nativeFlipActiveHorizontal();

  static native boolean nativeFlipActiveVertical();

  static native boolean nativeRotateActive180();

  static native boolean nativeRotateActive90(boolean clockwise);

  static native boolean nativeCropActiveSelection(int left, int top, int right, int bottom);

  static native boolean nativeCropActiveEllipse(int left, int top, int right, int bottom);

  static native boolean nativeTrimActiveMasked(
      int left, int top, int width, int height, byte[] mask);

  static native boolean nativeInvertActiveColors();

  static native boolean nativeGrayscaleActive();

  static native boolean nativeSepiaActive();

  static native boolean nativeBrightnessActive(int adjustment);

  static native boolean nativeContrastActive(int adjustment);

  static native boolean nativeThresholdActive(int threshold);

  static native boolean nativePosterizeActive(int levels);

  static native boolean nativeSolarizeActive(int threshold);

  static native boolean nativeSaturationActive(int adjustment);

  static native boolean nativeGammaActive(int percent);

  static native boolean nativeTintActive(
      int redAdjustment, int greenAdjustment, int blueAdjustment);

  static native boolean nativeSwapChannelsActive(int mode);

  static native boolean nativeColorBalanceActive(
      int redPercent, int greenPercent, int bluePercent);

  static native boolean nativeHueRotateActive(int degrees);

  static native boolean nativeLevelsActive(int blackPoint, int whitePoint);

  static native boolean nativeExposureActive(int percent);

  static native boolean nativeDesaturateChannelActive(int channel);

  static native boolean nativeAdjustAlphaActive(int percent);

  static native boolean nativeRemoveChannelActive(int channel);

  static native boolean nativeNormalizeActive();

  static native boolean nativeQuantizeActive(int step);

  static native boolean nativeClampHighlightsActive(int ceiling);

  static native boolean nativeLiftShadowsActive(int floor);

  static native boolean nativeAdjustChannelActive(int channel, int adjustment);

  static native void nativeSetBrushSelection(byte[] mask);

  static native void nativeStyledStroke(
      float x0,
      float y0,
      float x1,
      float y1,
      float radius,
      int color,
      float opacity,
      float hardness,
      boolean square,
      boolean eraser);

  static native void nativeStroke(float x0, float y0, float x1, float y1, float radius, int color);

  static native void nativeShape(int kind, int x0, int y0, int x1, int y1, int color);

  private static java.lang.ref.WeakReference<DrawingView> fillHost=new java.lang.ref.WeakReference<>(null);
  static boolean nativeFillSelection(int x,int y,int color,float opacity,byte[] selection){
    DrawingView drawing=fillHost.get();
    if(drawing!=null && (long)drawing.documentWidth()*drawing.documentHeight()>1048576
        && drawing.getContext() instanceof MainActivity activity){
      drawing.post(()->LayerWorkQueue.run(activity,()->nativeFillSelectionWorker(x,y,color,opacity,selection),true,null));
      return true;
    }
    return nativeFillSelectionWorker(x,y,color,opacity,selection);
  }
  static native boolean nativeFillSelectionWorker(int x,int y,int color,float opacity,byte[] selection);

  static native int nativeHistoryAvailability();

  static native boolean nativeUndo();

  static native boolean nativeRedo();

  static native int[] nativePixels();

  static native boolean nativeImport(int[] pixels);

  static native int nativePickColor(int x, int y);

  static native boolean nativeEraseSelection(int kind, int x0, int y0, int x1, int y1);

  static native boolean nativeEraseMaskedSelection(int x, int y, int w, int h, byte[] mask);

  static native boolean nativeMoveMaskedSelection(
      int x, int y, int w, int h, byte[] mask, int dx, int dy);

  static native int[] nativeCopySelection(int kind, int x0, int y0, int x1, int y1);

  static native boolean nativePasteSelection(int[] data, int x, int y);

  static native boolean nativeMovePixels(int kind, int x0, int y0, int x1, int y1, int dx, int dy);

  static native int nativeLayerCount();

  static native int nativeActiveLayer();

  static native boolean nativeAddLayer();

  static native boolean nativeSelectLayer(int index);

  static native boolean nativeDeleteLayer();

  static native boolean nativeToggleLayer();

  static native boolean nativeLayerVisible(int index);

  static native boolean nativeMoveLayer(int direction);

  static native boolean nativeSetLayerOpacity(float opacity);

  static native float nativeLayerOpacity();

  static native int[] nativeLayerThumbnail(int index);

  int canvasWidth = 800, canvasHeight = 800;

  static native boolean nativeLoadBitmap(int w, int h, int[] pixels);

  static native int nativeWidth();

  static native int nativeHeight();

  static native boolean nativeResizeDocument(int w, int h, boolean scalePixels);

  static native boolean nativeResizeDocumentOptions(
      int w, int h, boolean scalePixels, boolean bilinear, int anchor);

  static native boolean nativeCropDocument(int left, int top, int w, int h);

  static native long nativeRevision();

  public long revision() {
    return canvasDocument.revision();
  }

  public boolean hasPendingCurve() {
    return curve != null;
  }

  public static boolean hasDocument() {
    return nativeWidth() > 0;
  }

  public int documentWidth() {
    return canvasDocument.documentWidth();
  }

  public int documentHeight() {
    return canvasDocument.documentHeight();
  }

  public boolean resizeDocument(int w, int h, boolean scalePixels) {
    return canvasDocument.resizeDocument(w,h,scalePixels);
  }

  public boolean resizeDocument(int w, int h, boolean scalePixels, boolean bilinear, int anchor) {
    return canvasDocument.resizeDocument(w,h,scalePixels,bilinear,anchor);
  }

  public boolean resizeDocumentPixels(
      int w, int h, boolean scalePixels, boolean bilinear, int anchor) {
    return canvasDocument.resizeDocumentPixels(w,h,scalePixels,bilinear,anchor);
  }

  public void documentResized() {
    canvasDocument.documentResized();
  }

  void resetDocumentTools() {
    finishOpacityStroke();
    cursorX = cursorY = -1;
    cancelCurve();
    savedCurves.clear();
    cloneOriginReady = false;
    drawing = false;
    strokeEditing = false;
    movingSelection = false;
    movingPixels = false;
    navigating = false;
    shapePath.reset();
    dragBackup = null;
  }

  public boolean newDocument(int w, int h) {
    return canvasDocument.newDocument(w,h);
  }

  public boolean cropDocument() {
    return canvasDocument.cropDocument();
  }

  final Viewport viewport = new Viewport(canvasWidth, canvasHeight);
  boolean navigating;
  float gestureX, gestureY, gestureDistance, gestureAngle;
  int cursorX = -1, cursorY = -1;

  public String cursorStatus() {
    return canvasViewport.cursorStatus();
  }

  void updateCursor(float x, float y) {
    canvasInput.updateCursor(x,y);
  }

  @Override
  public boolean onHoverEvent(MotionEvent event) {
    return canvasInput.onHoverEvent(event);
  }

  Runnable viewportChangedListener;

  public void setOnViewportChangedListener(Runnable listener) {
    viewportChangedListener = listener;
  }

  public float zoomPercent() {
    return canvasViewport.zoomPercent();
  }

  public float rotationDegrees() {
    return canvasViewport.rotationDegrees();
  }

  void viewportChanged() {
    canvasViewport.viewportChanged();
  }

  public void fitCanvas() {
    canvasViewport.fitCanvas();
  }

  public void zoomBy(float factor) {
    canvasViewport.zoomBy(factor);
  }

  public void setZoomPercent(float percent) {
    canvasViewport.setZoomPercent(percent);
  }

  public void rotateView(float degrees) {
    canvasViewport.rotateView(degrees);
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    if (oldw == 0 || oldh == 0) viewport.fit(w, h);
    else {
      viewport.centerX += (w - oldw) / 2.;
      viewport.centerY += (h - oldh) / 2.;
    }
    viewportChanged();
  }

  void recordGesture(MotionEvent event) {
    canvasInput.recordGesture(event);
  }

  void cancelToolGesture() {
    canvasInput.cancelToolGesture();
  }

  public static final int BRUSH = 0,
      RECTANGLE = 1,
      ELLIPSE = 2,
      LINE = 3,
      BUCKET = 4,
      ERASER = 5,
      PICKER = 6,
      FILLED_RECTANGLE = 7,
      FILLED_ELLIPSE = 8,
      SELECT_RECTANGLE = 9,
      SELECT_ELLIPSE = 10,
      MOVE_SELECTION = 11,
      MOVE_PIXELS = 12,
      SELECT_FREE = 13,
      MAGIC_WAND = 14,
      TEXT = 15,
      ROUNDED_RECTANGLE = 16,
      FILLED_ROUNDED_RECTANGLE = 17,
      TRIANGLE = 18,
      FILLED_TRIANGLE = 19,
      PENCIL = 20,
      PAN = 21,
      ZOOM = 22,
      GRADIENT = 23,
      FREEFORM = 24,
      CIRCLE = 25,
      CLONE = 26,
      RECOLOR = 27,
      REMOVE_AI = 28;
  final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
  Bitmap bitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888);
  java.util.function.BiConsumer<Integer, Integer> textPositionListener;

  public void setOnTextPositionListener(java.util.function.BiConsumer<Integer, Integer> listener) {
    textPositionListener = listener;
  }

  public boolean insertText(
      String text, int x, int y, int size, boolean bold, boolean italic, String family) {
    return TextTool.insertText(this, text, x, y, size, bold, italic, family);
  }

  boolean pasteAt(Bitmap source, int x, int y) {
    int w = source.getWidth(), h = source.getHeight();
    int[] data = new int[2 + w * h];
    data[0] = w;
    data[1] = h;
    source.getPixels(data, 2, w, 0, 0, w, h);
    if (!nativePasteSelection(data, x, y)) return false;
    deselect();
    refresh();
    return true;
  }

  void additionalShape(int kind, float x0, float y0, float x1, float y1) {
    ShapeRaster.additionalShape(this, kind, x0, y0, x1, y1);
  }

  CurveDraft curve;
  CurveDraft dragBackup, canceledCurve;
  final CurveHistory curveHistory = new CurveHistory();
  int curveHandle = -1, nextCurveTag = 1;

  static native int nativeCurveTag();

  static native void nativeMarkCurve(int tag);

  final android.util.SparseArray<CurveSnapshot> savedCurves = new android.util.SparseArray<>();

  android.graphics.Path curvePath() {
    return LineCurveTool.curvePath(this);
  }

  public boolean confirmCurve() {
    return LineCurveTool.confirmCurve(this);
  }

  public void cancelCurve() {
    LineCurveTool.cancelCurve(this);
  }

  @Override
  public boolean onKeyDown(int code, android.view.KeyEvent event) {
    if(placedImage().key(code,event))return true;
    if (code == android.view.KeyEvent.KEYCODE_ENTER && confirmCurve()) return true;
    if (code == android.view.KeyEvent.KEYCODE_ESCAPE && curve != null) {
      cancelCurve();
      return true;
    }
    if (event.isCtrlPressed() && code == android.view.KeyEvent.KEYCODE_Z) {
      if (event.isShiftPressed()) redo();
      else undo();
      return true;
    }
    return super.onKeyDown(code, event);
  }

  final android.graphics.Path shapePath = new android.graphics.Path();

  void commitFreeform() {
    FreeformTool.commitFreeform(this);
  }

  final NavigationTool navigationTool = new NavigationTool(4);
  static native boolean nativeConfigureTipBrush(byte[] tips,byte[] texture,float[] parameters,float[] sizeCurve,float[] opacityCurve,float[] flowCurve);
  static native void nativeTipBrushStroke(float x0,float y0,float x1,float y1,float radius,int color,float opacity,float pressure);
  private BrushModule brushModule;
  BrushModule brushes() {
    if (brushModule == null) brushModule = new BrushModule(this);
    return brushModule;
  }
  static native void nativeAntialiasedStroke(float x0,float y0,float x1,float y1,
      float radius,int color,float opacity,float hardness,boolean square);

  int color = 0xFF202020, tool = BRUSH;
  int gradientMode, secondaryColor = 0xffffffff;
  boolean gradientTransparent = true;

  public void setSecondaryColor(int value) {
    secondaryColor = value;
  }

  public int gradientMode() {
    return gradientMode;
  }

  public boolean gradientTransparent() {
    return gradientTransparent;
  }

  public void configureGradient(int mode, boolean transparent) {
    if (mode < 0 || mode > 4) return;
    gradientMode = mode;
    gradientTransparent = transparent;
  }

  boolean cloneOriginReady;
  int cloneX, cloneY;

  static native void nativeBeginSampled(int x, int y);

  static native void nativeSampledStroke(
      boolean clone,
      int ox,
      int oy,
      int replacement,
      int tolerance,
      float x0,
      float y0,
      float x1,
      float y1,
      float radius,
      float opacity,
      float hardness);

  private final ToolOpacity toolOpacities = new ToolOpacity();
  Runnable toolOpacityChanged;
  private android.content.SharedPreferences opacityPreferences;
  private android.content.SharedPreferences opacityPreferences() {
    if (opacityPreferences == null) {
      opacityPreferences = getContext().getSharedPreferences("tool-opacity", android.content.Context.MODE_PRIVATE);
      for (int id = 0; id <= REMOVE_AI; id++)
        if (ToolOpacity.supports(id)) toolOpacities.set(id, opacityPreferences.getInt("tool-" + id, 100));
    }
    return opacityPreferences;
  }
  public boolean supportsToolOpacity() { return ToolOpacity.supports(tool); }
  public int toolOpacityPercent() { opacityPreferences(); return toolOpacities.get(tool); }
  public void setToolOpacityPercent(int value) {
    opacityPreferences();
    if (!supportsToolOpacity()) return;
    toolOpacities.set(tool, value);
    brushOpacity = toolOpacities.get(tool) / 100f;
    opacityPreferences.edit().putInt("tool-" + tool, toolOpacities.get(tool)).apply();
    if (toolOpacityChanged != null) toolOpacityChanged.run();
    invalidate();
  }
  void finishOpacityStroke() {
    if(brushModule!=null)brushModule.stop();
    if (strokeEditing) nativeEndOpacityStroke();
    strokeEditing = false;
  }
  static native boolean nativeBeginOpacityStroke(boolean grouped);
  static native void nativeEndOpacityStroke();
  float brushRadius = 4f, brushOpacity = 1f, brushHardness = 1f;
  boolean squareBrush, pressureBrush = true, strokeEditing;

  public float brushOpacity() {
    return brushOpacity;
  }

  public float brushHardness() {
    return brushHardness;
  }

  public boolean squareBrush() {
    return squareBrush;
  }

  public boolean pressureBrush() {
    return pressureBrush;
  }

  public void configureBrush(float opacity, float hardness, boolean square, boolean pressure) {
    if (Float.isFinite(opacity)) setToolOpacityPercent(Math.round(opacity * 100));
    brushHardness = hardness;
    squareBrush = square;
    pressureBrush = pressure;
  }

  void paintStroke(float x0, float y0, float x1, float y1, MotionEvent event) {
    StrokeSupport.paintStroke(this, x0, y0, x1, y1, event);
  }

  float previousX, previousY, startX, startY;
  boolean drawing;
  boolean hasSelection;
  boolean movingSelection;
  boolean movingPixels;
  final android.graphics.Path freePath = new android.graphics.Path();
  boolean freeSelectionReady;
  final android.graphics.Region freeRegion = new android.graphics.Region();
  int wandTolerance = 0;
  private final SelectionConfirmation selectionConfirmation=new SelectionConfirmation(this);
  SelectionConfirmation readSelectionConfirmation(){return selectionConfirmation;}
  final SelectionDragPreview selectionDragPreview=new SelectionDragPreview(this);
  SelectionDragPreview readSelectionDragPreview(){return selectionDragPreview;}
  final android.graphics.Path wandBoundary = new android.graphics.Path();

  int readWandTolerance(){return wandTolerance;}
  void installWandRegion(android.graphics.Region region){
    installWandRegion(region,region.getBoundaryPath());
  }
  void installWandRegion(android.graphics.Region region,android.graphics.Path outline){
    freeRegion.set(region);freePath.reset();wandBoundary.reset();
    selectionTool=MAGIC_WAND;android.graphics.Rect bounds=region.getBounds();
    selectionLeft=bounds.left;selectionTop=bounds.top;selectionRight=bounds.right;selectionBottom=bounds.bottom;
    hasSelection=!region.isEmpty();if(hasSelection)wandBoundary.set(outline);selectionConfirmation.begin();invalidate();
  }
  public void setWandTolerance(int value) {
    wandTolerance = Math.max(0, Math.min(255, value));
  }

  float moveStartX,
      moveStartY,
      moveOriginalLeft,
      moveOriginalTop,
      moveOriginalRight,
      moveOriginalBottom;
  int selectionTool;
  float selectionLeft, selectionTop, selectionRight, selectionBottom;
  final Paint selectionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  Runnable canvasChangedListener;
  java.util.function.IntConsumer pickedColorListener;

  public void setOnColorPickedListener(java.util.function.IntConsumer listener) {
    pickedColorListener = listener;
  }

  public void setOnCanvasChangedListener(Runnable listener) {
    canvasChangedListener = listener;
  }

  public DrawingView(Context context) {
    super(context);
    fillHost=new java.lang.ref.WeakReference<>(this);
    nativeHistoryDirectory(context.getCacheDir().getAbsolutePath());
    nativeImageStorage(context.getCacheDir().getAbsolutePath());
    opacityPreferences();
    brushOpacity = toolOpacities.get(tool) / 100f;
    setContentDescription(
        "Lienzo de dibujo; arrastra para usar la herramienta activa y utiliza dos dedos para"
            + " navegar");
    if (nativeWidth() == 0 && !nativeCreate(canvasWidth, canvasHeight))
      throw new IllegalStateException("Canvas error");
    refresh();
  }

  public boolean setLayerOpacity(float opacity) {
    return layerOperations.setLayerOpacity(opacity);
  }

  public float layerOpacity() {
    return layerOperations.layerOpacity();
  }

  public Bitmap layerThumbnail(int index) {
    return layerOperations.layerThumbnail(index);
  }

  static native String nativeLayerName(int index);

  static native boolean nativeRenameLayer(int index, String name);

  public String layerName(int index) {
    return layerOperations.layerName(index);
  }

  public boolean renameLayer(int index, String name) {
    return layerOperations.renameLayer(index,name);
  }

  public int layerCount() {
    return layerOperations.layerCount();
  }

  public int activeLayer() {
    return layerOperations.activeLayer();
  }

  public boolean layerVisible(int index) {
    return layerOperations.layerVisible(index);
  }

  public boolean addLayer() {
    return layerOperations.addLayer();
  }

  public boolean selectLayer(int index) {
    return layerOperations.selectLayer(index);
  }

  public boolean deleteLayer() {
    return layerOperations.deleteLayer();
  }

  public boolean toggleLayer() {
    return layerOperations.toggleLayer();
  }

  public boolean moveLayer(int direction) {
    return layerOperations.moveLayer(direction);
  }

  public void setColor(int value) {
    color = value;
  }

  Runnable brushWidthChanged;
  public void setBrushRadius(float radius) {
    if (Float.isFinite(radius) && radius >= .5f && radius <= 150f && radius != brushRadius) {
      brushRadius = radius;
      if (brushWidthChanged != null) brushWidthChanged.run();
      invalidate();
    }
  }

  public float brushRadius() {
    return brushRadius;
  }

  private RemoveAiModule removeAiModule;
  Runnable removeAiControlsChanged;
  RemoveAiModule removeAi() { if (removeAiModule == null) removeAiModule = new RemoveAiModule(this); return removeAiModule; }
  static native int[] nativeRemoveSource();
  static native boolean nativeRemoveCommit(int[] pixels, byte[] mask, int width, int height, int layer, long revision);
  @Override protected void onDetachedFromWindow() {
    if (removeAiModule != null) { removeAiModule.dispose(); removeAiModule = null; }
    finishOpacityStroke();
    super.onDetachedFromWindow();
  }

  public int currentTool() {
    return tool;
  }

  @Override
  public void setEnabled(boolean enabled) {
    if (!enabled) confirmCurve();
    super.setEnabled(enabled);
  }

  public void setTool(int value) {
    if (value < BRUSH || value > REMOVE_AI) return;
    if (value != LINE) {
      if (curve != null) confirmCurve();
      else if (canceledCurve != null) cancelCurve();
    }
    if (value == CLONE) cloneOriginReady = false;
    if (tool == REMOVE_AI || value == REMOVE_AI) removeAi().clear();
    finishOpacityStroke();
    tool = value;
    selectionConfirmation.toolChanged(value);
    opacityPreferences();
    brushOpacity = toolOpacities.get(tool) / 100f;
    if (toolOpacityChanged != null) toolOpacityChanged.run();
    if (removeAiControlsChanged != null) removeAiControlsChanged.run();
    // Switching away from a selection tool must not leave a selection
    // permanently active as an accidental overlay on subsequent drawings.
    invalidate();
  }

  public boolean transformSelection(float degrees, float sx, float sy) {
    return canvasSelection.transformSelection(degrees,sx,sy);
  }

  public boolean hasSelection() {
    return canvasSelection.hasSelection();
  }

  public boolean shrinkSelectionOnePixel() {
    return canvasSelection.shrinkSelectionOnePixel();
  }

  public boolean expandSelectionOnePixel() {
    return canvasSelection.expandSelectionOnePixel();
  }

  public boolean invertSelection() {
    return canvasSelection.invertSelection();
  }

  public boolean selectionContains(int px, int py) {
    return canvasSelection.selectionContains(px,py);
  }

  public boolean flipActiveHorizontal() {
    return canvasEffects.flipActiveHorizontal();
  }

  public boolean flipActiveVertical() {
    return canvasEffects.flipActiveVertical();
  }

  public boolean rotateActive180() {
    return canvasEffects.rotateActive180();
  }

  public boolean rotateActive90(boolean clockwise) {
    return canvasEffects.rotateActive90(clockwise);
  }

  public boolean adjustChannelActive(int channel, int adjustment) {
    return canvasEffects.adjustChannelActive(channel,adjustment);
  }

  public boolean liftShadowsActive(int floor) {
    return canvasEffects.liftShadowsActive(floor);
  }

  public boolean clampHighlightsActive(int ceiling) {
    return canvasEffects.clampHighlightsActive(ceiling);
  }

  public boolean quantizeActive(int step) {
    return canvasEffects.quantizeActive(step);
  }

  public boolean normalizeActive() {
    return canvasEffects.normalizeActive();
  }

  public boolean removeChannelActive(int channel) {
    return canvasEffects.removeChannelActive(channel);
  }

  public boolean adjustAlphaActive(int percent) {
    return canvasEffects.adjustAlphaActive(percent);
  }

  public boolean grayscaleFromChannelActive(int channel) {
    return canvasEffects.grayscaleFromChannelActive(channel);
  }

  public boolean exposureActive(int percent) {
    return canvasEffects.exposureActive(percent);
  }

  public boolean levelsActive(int blackPoint, int whitePoint) {
    return canvasEffects.levelsActive(blackPoint,whitePoint);
  }

  public boolean hueRotateActive(int degrees) {
    return canvasEffects.hueRotateActive(degrees);
  }

  public boolean colorBalanceActive(int redPercent, int greenPercent, int bluePercent) {
    return canvasEffects.colorBalanceActive(redPercent,greenPercent,bluePercent);
  }

  public boolean swapChannelsActive(int mode) {
    return canvasEffects.swapChannelsActive(mode);
  }

  public boolean tintActive(int red, int green, int blue) {
    return canvasEffects.tintActive(red,green,blue);
  }

  public boolean gammaActive(int percent) {
    return canvasEffects.gammaActive(percent);
  }

  public boolean saturationActive(int adjustment) {
    return canvasEffects.saturationActive(adjustment);
  }

  public boolean solarizeActive(int threshold) {
    return canvasEffects.solarizeActive(threshold);
  }

  public boolean posterizeActive(int levels) {
    return canvasEffects.posterizeActive(levels);
  }

  public boolean thresholdActive(int threshold) {
    return canvasEffects.thresholdActive(threshold);
  }

  public boolean contrastActive(int adjustment) {
    return canvasEffects.contrastActive(adjustment);
  }

  public boolean brightnessActive(int adjustment) {
    return canvasEffects.brightnessActive(adjustment);
  }

  public boolean sepiaActive() {
    return canvasEffects.sepiaActive();
  }

  public boolean grayscaleActive() {
    return canvasEffects.grayscaleActive();
  }

  public boolean invertActiveColors() {
    return canvasEffects.invertActiveColors();
  }

  public boolean trimActiveToFreeSelection() {
    return canvasSelection.trimActiveToFreeSelection();
  }

  public boolean trimActiveToEllipseSelection() {
    return canvasSelection.trimActiveToEllipseSelection();
  }

  public boolean trimActiveToRectSelection() {
    return canvasSelection.trimActiveToRectSelection();
  }

  public void clear() {
    if (nativeClear()) {
      deselect();
      refresh();
    }
  }

  public boolean pasteBitmap(Bitmap source) {
    return canvasDocument.pasteBitmap(source);
  }

  public Bitmap copySelection() {
    return canvasSelection.copySelection();
  }

  public boolean eraseSelection() {
    return canvasSelection.eraseSelection();
  }

  public void selectAll() {
    canvasSelection.selectAll();
  }

  public void deselect() {
    canvasSelection.deselect();
  }

  public void enableSelectionMove() {
    canvasSelection.enableSelectionMove();
  }

  public boolean moveSelectedPixels(int dx, int dy) {
    return canvasSelection.moveSelectedPixels(dx,dy);
  }

  public void undo() {
    finishOpacityStroke();
    LineCurveTool.undo(this);
  }

  public void redo() {
    finishOpacityStroke();
    LineCurveTool.redo(this);
  }

  public boolean loadBitmap(Bitmap source) {
    return canvasDocument.loadBitmap(source);
  }

  public Bitmap snapshot() {
    return canvasDocument.snapshot();
  }

  boolean strokeRefreshPending;
  int dirtyLeft=Integer.MAX_VALUE, dirtyTop=Integer.MAX_VALUE, dirtyRight, dirtyBottom;
  static native int[] nativeRegionPixels(int x,int y,int width,int height);

  void noteStrokeBounds(float x0,float y0,float x1,float y1,float radius) {
    canvasRendering.noteStrokeBounds(x0,y0,x1,y1,radius);
  }

  void refreshStrokePixels() {
    canvasRendering.refreshStrokePixels();
  }
  final Runnable strokeRefresh = () -> {
    strokeRefreshPending = false;
    refreshStrokePixels();
  };

  // Coalesce pointer events into one pixel transfer per display frame.
  void requestStrokeRefresh() {
    canvasRendering.requestStrokeRefresh();
  }

  void finishStrokeRefresh() {
    canvasRendering.finishStrokeRefresh();
  }
  void drawCanvasPixels(Canvas canvas){canvasRendering.drawPixels(canvas);}
  NativeTileDisplay.Preparation captureCanvasDetail(){return canvasRendering.captureDetail();}
  void installCanvasFrame(CanvasRendering.Frame frame){canvasRendering.installFrame(frame);}
  void importedFrameReady(CanvasRendering.Frame frame){
    resetDocumentTools();deselect();installCanvasFrame(frame);fitCanvas();
  }

  void refresh() {
    canvasRendering.refresh();
  }

  void refreshPixels() {
    canvasRendering.refreshPixels();
  }

  final Paint checkerPaint = new Paint();

  @Override
  protected void onDraw(Canvas canvas) {
    canvasRendering.onDraw(canvas);
  }

  void selectMatchingRegion(int sx, int sy) {
    canvasSelection.selectMatchingRegion(sx,sy);
  }

  void translateSelectionMask(int dx, int dy) {
    canvasSelection.translateSelectionMask(dx,dy);
  }

  void restoreMovedSelection() {
    canvasSelection.restoreMovedSelection();
  }

  void updateMovedSelection(float x, float y) {
    canvasSelection.updateMovedSelection(x,y);
  }

  void updateSelection(float x, float y) {
    canvasSelection.updateSelection(x,y);
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    return canvasInput.onTouchEvent(event);
  }

  void defaultDraw(Canvas canvas) { super.onDraw(canvas); }
  boolean defaultTouchEvent(MotionEvent event) { return super.onTouchEvent(event); }
  boolean defaultHoverEvent(MotionEvent event) { return super.onHoverEvent(event); }

  // Workspace composition API: components do not access host fields directly.
  int readDirtyLeft() { return dirtyLeft; }
  int writeDirtyLeft(int value) { dirtyLeft = value; return value; }
  int readDirtyTop() { return dirtyTop; }
  int writeDirtyTop(int value) { dirtyTop = value; return value; }
  int readDirtyRight() { return dirtyRight; }
  int writeDirtyRight(int value) { dirtyRight = value; return value; }
  int readCanvasWidth() { return canvasWidth; }
  int writeCanvasWidth(int value) { canvasWidth = value; return value; }
  int readDirtyBottom() { return dirtyBottom; }
  int writeDirtyBottom(int value) { dirtyBottom = value; return value; }
  int readCanvasHeight() { return canvasHeight; }
  int writeCanvasHeight(int value) { canvasHeight = value; return value; }
  android.graphics.Bitmap readBitmap() { return bitmap; }
  android.graphics.Bitmap writeBitmap(android.graphics.Bitmap value) { bitmap = value; return value; }
  boolean readStrokeRefreshPending() { return strokeRefreshPending; }
  boolean writeStrokeRefreshPending(boolean value) { strokeRefreshPending = value; return value; }
  java.lang.Runnable readStrokeRefresh() { return strokeRefresh; }
  java.lang.Runnable readCanvasChangedListener() { return canvasChangedListener; }
  java.lang.Runnable writeCanvasChangedListener(java.lang.Runnable value) { canvasChangedListener = value; return value; }
  art.velyntora.core.Viewport readViewport() { return viewport; }
  int readColor() { return color; }
  int writeColor(int value) { color = value; return value; }
  int readCursorX() { return cursorX; }
  int writeCursorX(int value) { cursorX = value; return value; }
  int readCursorY() { return cursorY; }
  int writeCursorY(int value) { cursorY = value; return value; }
  java.lang.Runnable readViewportChangedListener() { return viewportChangedListener; }
  java.lang.Runnable writeViewportChangedListener(java.lang.Runnable value) { viewportChangedListener = value; return value; }
  float readGestureX() { return gestureX; }
  float writeGestureX(float value) { gestureX = value; return value; }
  float readGestureY() { return gestureY; }
  float writeGestureY(float value) { gestureY = value; return value; }
  float readGestureDistance() { return gestureDistance; }
  float writeGestureDistance(float value) { gestureDistance = value; return value; }
  float readGestureAngle() { return gestureAngle; }
  float writeGestureAngle(float value) { gestureAngle = value; return value; }
  int readTool() { return tool; }
  int writeTool(int value) { tool = value; return value; }
  boolean readDrawing() { return drawing; }
  boolean writeDrawing(boolean value) { drawing = value; return value; }
  boolean readMovingSelection() { return movingSelection; }
  boolean writeMovingSelection(boolean value) { movingSelection = value; return value; }
  boolean readMovingPixels() { return movingPixels; }
  boolean writeMovingPixels(boolean value) { movingPixels = value; return value; }
  boolean readNavigating() { return navigating; }
  boolean writeNavigating(boolean value) { navigating = value; return value; }
  int readPAN() { return PAN; }
  int readZOOM() { return ZOOM; }
  boolean readStrokeEditing() { return strokeEditing; }
  boolean writeStrokeEditing(boolean value) { strokeEditing = value; return value; }
  float readStartX() { return startX; }
  float writeStartX(float value) { startX = value; return value; }
  float readPreviousX() { return previousX; }
  float writePreviousX(float value) { previousX = value; return value; }
  float readStartY() { return startY; }
  float writeStartY(float value) { startY = value; return value; }
  float readPreviousY() { return previousY; }
  float writePreviousY(float value) { previousY = value; return value; }
  float readSelectionLeft() { return selectionLeft; }
  float writeSelectionLeft(float value) { selectionLeft = value; return value; }
  float readSelectionTop() { return selectionTop; }
  float writeSelectionTop(float value) { selectionTop = value; return value; }
  float readSelectionRight() { return selectionRight; }
  float writeSelectionRight(float value) { selectionRight = value; return value; }
  float readSelectionBottom() { return selectionBottom; }
  float writeSelectionBottom(float value) { selectionBottom = value; return value; }
  boolean readHasSelection() { return hasSelection; }
  boolean writeHasSelection(boolean value) { hasSelection = value; return value; }
  int readMOVE_SELECTION() { return MOVE_SELECTION; }
}
