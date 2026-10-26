package art.velyntora.core;

import static org.junit.Assert.*;

import android.app.Instrumentation;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;

/** Runs against the actual Android widgets, JNI engine, and lifecycle on an emulator. */
public final class WorkspaceDeviceTests {
  private Instrumentation instrumentation;
  private MainActivity activity;

  @Before
  public void open() {
    instrumentation = InstrumentationRegistry.getInstrumentation();
    Intent intent = new Intent(instrumentation.getTargetContext(), MainActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    activity = (MainActivity) instrumentation.startActivitySync(intent);
    instrumentation.waitForIdleSync();
    instrumentation.runOnMainSync(
        () -> activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));
    long deadline = android.os.SystemClock.uptimeMillis() + 5000;
    java.util.concurrent.atomic.AtomicBoolean ready =
        new java.util.concurrent.atomic.AtomicBoolean();
    do {
      instrumentation.waitForIdleSync();
      instrumentation.runOnMainSync(
          () ->
              ready.set(
                  activity.getResources().getConfiguration().orientation == 1
                      && drawing() != null
                      && drawing().isLaidOut()
                      && !drawing().isLayoutRequested()
                      && drawing().getWidth() > 0
                      && drawing().getHeight() > 0));
      if (!ready.get()) android.os.SystemClock.sleep(50);
    } while (!ready.get() && android.os.SystemClock.uptimeMillis() < deadline);
    assertTrue("Workspace must finish layout before gestures", ready.get());
  }

  @After
  public void close() {
    instrumentation.runOnMainSync(() -> activity.finish());
    instrumentation.waitForIdleSync();
  }

  private DrawingView drawing() {
    return findDrawing(activity.getWindow().getDecorView());
  }

  private DrawingView findDrawing(View view) {
    if (view instanceof DrawingView) return (DrawingView) view;
    if (view instanceof ViewGroup) {
      ViewGroup group = (ViewGroup) view;
      for (int i = 0; i < group.getChildCount(); i++) {
        DrawingView found = findDrawing(group.getChildAt(i));
        if (found != null) return found;
      }
    }
    return null;
  }

  private Button findButton(View view, String label) {
    if (view instanceof Button
        && (label.contentEquals(((Button) view).getText())
            || label.contentEquals(view.getContentDescription()))) return (Button) view;
    if (view instanceof ViewGroup) {
      ViewGroup group = (ViewGroup) view;
      for (int i = 0; i < group.getChildCount(); i++) {
        Button found = findButton(group.getChildAt(i), label);
        if (found != null) return found;
      }
    }
    return null;
  }

  @Test
  public void rotationRetainsLayersAndGivesCanvasSpace() {
    instrumentation.runOnMainSync(
        () -> {
          assertTrue(drawing().newDocument(64, 32));
          assertTrue(drawing().addLayer());
        });
    int count = drawing().layerCount();
    long revision = drawing().revision();
    for (int orientation :
        new int[] {
          ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }) {
      instrumentation.runOnMainSync(() -> activity.setRequestedOrientation(orientation));
      instrumentation.waitForIdleSync();
      long end = android.os.SystemClock.uptimeMillis() + 5000;
      while (android.os.SystemClock.uptimeMillis() < end) {
        int wanted = orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE ? 2 : 1;
        if (activity.getResources().getConfiguration().orientation == wanted) break;
        android.os.SystemClock.sleep(50);
      }
      instrumentation.waitForIdleSync();
      instrumentation.runOnMainSync(
          () -> {
            assertNotNull(drawing());
            assertTrue(drawing().getWidth() > 0);
            assertTrue(
                drawing().getHeight() > 48 * activity.getResources().getDisplayMetrics().density);
            assertEquals(count, drawing().layerCount());
            assertEquals(revision, drawing().revision());
          });
    }
  }

  @Test
  public void unsavedBackOffersCancelWithoutLosingLayers() {
    instrumentation.runOnMainSync(
        () -> {
          assertTrue(drawing().newDocument(16, 16));
          assertTrue(drawing().addLayer());
          activity.onBackPressed();
        });
    instrumentation.waitForIdleSync();
    // Dialog windows are separate roots; use UiAutomation to observe the user-facing choice.
    android.view.accessibility.AccessibilityNodeInfo cancel = null;
    long deadline = android.os.SystemClock.uptimeMillis() + 5000;
    while (cancel == null && android.os.SystemClock.uptimeMillis() < deadline) {
      android.view.accessibility.AccessibilityNodeInfo root =
          instrumentation.getUiAutomation().getRootInActiveWindow();
      if (root != null) {
        java.util.List<android.view.accessibility.AccessibilityNodeInfo> choices =
            root.findAccessibilityNodeInfosByText("Cancelar");
        if (choices.isEmpty()) choices = root.findAccessibilityNodeInfosByText("CANCELAR");
        if (!choices.isEmpty()) cancel = choices.get(0);
      }
      if (cancel == null) android.os.SystemClock.sleep(100);
    }
    assertNotNull("Unsaved-change dialog must expose Cancel", cancel);
    while (!cancel.isClickable() && cancel.getParent() != null) cancel = cancel.getParent();
    long revision = drawing().revision();
    assertTrue(
        "Cancel must accept click",
        cancel.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK));
    instrumentation.waitForIdleSync();
    assertFalse("Cancel must retain the activity", activity.isFinishing());
    assertEquals("Cancel must retain both layers", 2, drawing().layerCount());
    assertEquals(revision, drawing().revision());
  }

  @Test
  public void projectAndRecoveryPreserveDimensionsAndRejectStaleRevision() throws Exception {
    java.io.File file =
        new java.io.File(
            instrumentation.getTargetContext().getCacheDir(), "device-recovery-test.vlycore");
    instrumentation.runOnMainSync(() -> assertTrue(drawing().newDocument(23, 17)));
    long revision = drawing().revision();
    try (android.os.ParcelFileDescriptor fd =
        android.os.ParcelFileDescriptor.open(
            file,
            android.os.ParcelFileDescriptor.MODE_CREATE
                | android.os.ParcelFileDescriptor.MODE_READ_WRITE
                | android.os.ParcelFileDescriptor.MODE_TRUNCATE)) {
      assertFalse(drawing().writeRecovery(fd.getFd(), revision - 1));
      assertEquals(0, file.length());
      assertTrue(drawing().writeRecovery(fd.getFd(), revision));
    }
    instrumentation.runOnMainSync(() -> assertTrue(drawing().newDocument(3, 2)));
    try (android.os.ParcelFileDescriptor fd =
        android.os.ParcelFileDescriptor.open(
            file, android.os.ParcelFileDescriptor.MODE_READ_ONLY)) {
      assertTrue(drawing().readProject(fd.getFd()));
    }
    instrumentation.runOnMainSync(
        () -> {
          drawing().projectOpened();
          assertEquals(23, drawing().documentWidth());
          assertEquals(17, drawing().documentHeight());
        });
    assertTrue(file.delete());
  }

  private void gesture(DrawingView view, float x0, float y0, float x1, float y1, boolean cancel) {
    long now = android.os.SystemClock.uptimeMillis();
    float left = view.getWidth() / 2f - view.documentWidth() / 2f,
        top = view.getHeight() / 2f - view.documentHeight() / 2f;
    int[] actions = {
      android.view.MotionEvent.ACTION_DOWN,
      android.view.MotionEvent.ACTION_MOVE,
      cancel ? android.view.MotionEvent.ACTION_CANCEL : android.view.MotionEvent.ACTION_UP
    };
    for (int i = 0; i < actions.length; i++) {
      android.view.MotionEvent event =
          android.view.MotionEvent.obtain(
              now,
              now + i * 10,
              actions[i],
              left + (i == 0 ? x0 : x1),
              top + (i == 0 ? y0 : y1),
              0);
      try {
        view.onTouchEvent(event);
      } finally {
        event.recycle();
      }
    }
  }

  @Test
  public void shapesRespectSelectionOpacityAndCanceledGesture() {
    instrumentation.runOnMainSync(
        () -> {
          DrawingView view = drawing();
          Bitmap blank = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888);
          try {
            assertTrue(view.loadBitmap(blank));
          } finally {
            blank.recycle();
          }
          view.setZoomPercent(100);
          view.setTool(DrawingView.SELECT_RECTANGLE);
          gesture(view, 8, 8, 24, 24, false);
          assertTrue(view.hasSelection());
          view.setColor(0xffff0000);
          view.configureBrush(.5f, 1, false, false);
          view.setTool(DrawingView.FILLED_RECTANGLE);
          view.setToolOpacityPercent(50);
          gesture(view, 1, 1, 32, 32, false);
          assertTrue("Painting a shape must preserve selection", view.hasSelection());
          Bitmap image = view.snapshot();
          try {
            assertEquals(0x80ff0000, image.getPixel(10, 10));
            assertEquals(0, image.getPixel(2, 2));
            assertEquals(0, image.getPixel(30, 30));
          } finally {
            image.recycle();
          }
          long revision = view.revision();
          view.setTool(DrawingView.RECTANGLE);
          gesture(view, 9, 9, 20, 20, true);
          assertEquals("Canceled shape must not create history", revision, view.revision());
          view.undo();
          image = view.snapshot();
          try {
            assertEquals(0, image.getPixel(10, 10));
          } finally {
            image.recycle();
          }
          view.setTool(DrawingView.GRADIENT);
          view.configureBrush(1, 1, false, false);
          view.setSecondaryColor(0xff0000ff);
          view.configureGradient(0, false);
          gesture(view, .5f, .5f, 63.5f, .5f, false);
          image = view.snapshot();
          try {
            assertEquals(0xffff0000, image.getPixel(0, 10));
            assertEquals(0xff0000ff, image.getPixel(63, 10));
          } finally {
            image.recycle();
          }
          view.undo();
          image = view.snapshot();
          try {
            assertEquals(0, image.getPixel(0, 10));
          } finally {
            image.recycle();
          }
        });
  }

  @Test
  public void svgRasterizationAndPackagedXpmColors() throws Exception {
    String source =
        "<svg xmlns='http://www.w3.org/2000/svg' width='40' height='20' viewBox='0 0 40 20'><rect"
            + " width='20' height='20' fill='#ff0000'/></svg>";
    Bitmap bitmap =
        SvgRaster.read(
            new java.io.ByteArrayInputStream(
                source.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    try {
      assertEquals(40, bitmap.getWidth());
      assertEquals(20, bitmap.getHeight());
      assertEquals(0xffff0000, bitmap.getPixel(10, 10));
      assertEquals(0, bitmap.getPixel(30, 10));
    } finally {
      bitmap.recycle();
    }
    source =
        "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 20000 10000'><rect width='20000'"
            + " height='10000' fill='blue'/></svg>";
    bitmap =
        SvgRaster.read(
            new java.io.ByteArrayInputStream(
                source.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    try {
      assertTrue((long) bitmap.getWidth() * bitmap.getHeight() <= art.velyntora.core.document.CanvasLimits.MAX_PIXELS);
      assertTrue(bitmap.getWidth() <= art.velyntora.core.document.CanvasLimits.MAX_SIDE);
      assertEquals(5000,bitmap.getWidth());assertEquals(2500,bitmap.getHeight());
      assertEquals(0xff0000ff, bitmap.getPixel(1, 1));
    } finally {
      bitmap.recycle();
    }
    String xpm = "\"1 1 1 1\",\"a c Light Goldenrod Yellow\",\"a\"";
    assertEquals(
        0xfffafad2,
        XRasterReader.xpm(
                new java.io.ByteArrayInputStream(
                    xpm.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
            .pixels[0]);
    try {
      SvgRaster.read(
          new java.io.ByteArrayInputStream(
              "<!DOCTYPE svg><svg/>".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
      fail("Entity declarations must be rejected");
    } catch (java.io.IOException expected) {
    }
  }

  @Test
  public void quickCommandsHaveAccessibleTouchTargets() {
    instrumentation.runOnMainSync(
        () -> {
          float density = activity.getResources().getDisplayMetrics().density;
          for (String label :
              new String[] {
                "Menú principal", "Nuevo", "Guardar", "Deshacer", "Rehacer", "Herramientas", "Capas"
              }) {
            Button button = findButton(activity.getWindow().getDecorView(), label);
            assertNotNull(label, button);
            assertNotNull(label, button.getContentDescription());
            assertTrue(label, button.getMeasuredHeight() >= Math.round(48 * density));
          }
        });
  }

  @Test
  public void enlargedPixelsHaveNoInterpolatedColors() {
    instrumentation.runOnMainSync(
        () -> {
          DrawingView view = drawing();
          Bitmap source =
              Bitmap.createBitmap(
                  new int[] {0xffff0000, 0xff0000ff}, 2, 1, Bitmap.Config.ARGB_8888);
          try {
            assertTrue(view.loadBitmap(source));
          } finally {
            source.recycle();
          }
          view.rotateView(-view.rotationDegrees());
          view.setZoomPercent(1600);
          Bitmap capture =
              Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
          try {
            view.draw(new android.graphics.Canvas(capture));
            int x = view.getWidth() / 2, y = view.getHeight() / 2;
            for (int offset = -14; offset <= -2; offset++)
              assertEquals(
                  "Enlarged red pixel must stay red", 0xffff0000, capture.getPixel(x + offset, y));
            for (int offset = 2; offset <= 14; offset++)
              assertEquals(
                  "Enlarged blue pixel must stay blue",
                  0xff0000ff,
                  capture.getPixel(x + offset, y));
          } finally {
            capture.recycle();
          }
        });
  }

  private android.widget.GridLayout toolGrid(View view) {
    if (view instanceof android.widget.GridLayout
        && ((android.widget.GridLayout) view).getChildCount() == 21)
      return (android.widget.GridLayout) view;
    if (view instanceof ViewGroup) {
      ViewGroup group = (ViewGroup) view;
      for (int i = 0; i < group.getChildCount(); i++) {
        android.widget.GridLayout found = toolGrid(group.getChildAt(i));
        if (found != null) return found;
      }
    }
    return null;
  }

  @Test
  public void workspaceUsesTwoColumnsAndIconCommands() {
    instrumentation.runOnMainSync(
        () -> {
          View root = activity.getWindow().getDecorView();
          assertNotNull(toolGrid(root));
          assertEquals(2, toolGrid(root).getColumnCount());
          for (String label :
              new String[] {"Nuevo", "Abrir", "Guardar", "Deshacer", "Rehacer", "Menú principal"}) {
            Button button = findButton(root, label);
            assertNotNull(label, button);
            assertEquals("Command uses icon", 0, button.getText().length());
            assertNotNull(button.getCompoundDrawables()[0]);
          }
        });
  }

  @Test
  public void captureLandscapeWorkspace() throws Exception {
    instrumentation.runOnMainSync(
        () -> {
          activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
          assertTrue(drawing().newDocument(800, 600));
          drawing().setTool(DrawingView.LINE);
        });
    long deadline = android.os.SystemClock.uptimeMillis() + 5000;
    while (activity.getResources().getConfiguration().orientation != 2
        && android.os.SystemClock.uptimeMillis() < deadline) android.os.SystemClock.sleep(50);
    instrumentation.waitForIdleSync();
    instrumentation.runOnMainSync(
        () -> {
          drawing().rotateView(-drawing().rotationDegrees());
          drawing().fitCanvas();
        });
    instrumentation.waitForIdleSync();
    Bitmap screenshot = instrumentation.getUiAutomation().takeScreenshot();
    assertNotNull(screenshot);
    screenshot.recycle();
    String directory = "/sdcard/Download/velyntora-core-workspace";
    for (String command :
        new String[] {
          "mkdir -p " + directory,
          "screencap -p "
              + directory
              + "/workspace-"
              + activity.getResources().getConfiguration().fontScale
              + ".png"
        }) {
      try (android.os.ParcelFileDescriptor result =
              instrumentation.getUiAutomation().executeShellCommand(command);
          java.io.FileInputStream input = new java.io.FileInputStream(result.getFileDescriptor())) {
        byte[] buffer = new byte[1024];
        while (input.read(buffer) != -1) {}
      }
    }
  }

  @Test
  public void toolModulesKeepShapePixelsAndUndo() {
    instrumentation.runOnMainSync(
        () -> {
          DrawingView v = drawing();
          for (int tool :
              new int[] {
                DrawingView.RECTANGLE,
                DrawingView.ELLIPSE,
                DrawingView.ROUNDED_RECTANGLE,
                DrawingView.TRIANGLE,
                DrawingView.FILLED_RECTANGLE,
                DrawingView.FILLED_ELLIPSE,
                DrawingView.FILLED_ROUNDED_RECTANGLE,
                DrawingView.FILLED_TRIANGLE,
                DrawingView.CIRCLE
              }) {
            Bitmap blank = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888);
            try {
              assertTrue(v.loadBitmap(blank));
            } finally {
              blank.recycle();
            }
            v.setZoomPercent(100);
            v.setColor(0xffff0000);
            v.configureBrush(1, 1, false, false);
            v.setTool(tool);
            v.setToolOpacityPercent(100);
            long revision = v.revision();
            gesture(v, 8, 8, 40, 40, true);
            assertEquals("Canceled shape " + tool, revision, v.revision());
            gesture(v, 8, 8, 40, 40, false);
            assertTrue("Shape must edit pixels " + tool, v.revision() > revision);
            Bitmap image = v.snapshot();
            try {
              int[] pixels = new int[4096];
              image.getPixels(pixels, 0, 64, 0, 0, 64, 64);
              boolean painted = false;
              for (int color : pixels) painted |= color != 0;
              assertTrue("Shape must leave pixels " + tool, painted);
            } finally {
              image.recycle();
            }
            v.undo();
            image = v.snapshot();
            try {
              int[] pixels = new int[4096];
              image.getPixels(pixels, 0, 64, 0, 0, 64, 64);
              for (int color : pixels) assertEquals("Undo shape " + tool, 0, color);
            } finally {
              image.recycle();
            }
          }
        });
  }

  @Test
  public void lineModuleEnterCommitsAndControlZReopensDraft() {
    instrumentation.runOnMainSync(
        () -> {
          DrawingView v = drawing();
          assertTrue(v.newDocument(64, 64));
          v.setZoomPercent(100);
          v.setColor(0xffff0000);
          v.setTool(DrawingView.LINE);
          long revision = v.revision();
          gesture(v, 8, 8, 40, 40, false);
          assertTrue(v.hasPendingCurve());
          assertEquals("Draft must not modify native history", revision, v.revision());
          assertTrue(
              v.onKeyDown(
                  android.view.KeyEvent.KEYCODE_ENTER,
                  new android.view.KeyEvent(
                      android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENTER)));
          assertFalse(v.hasPendingCurve());
          assertTrue(v.revision() > revision);
          android.view.KeyEvent undo =
              new android.view.KeyEvent(
                  0,
                  0,
                  android.view.KeyEvent.ACTION_DOWN,
                  android.view.KeyEvent.KEYCODE_Z,
                  0,
                  android.view.KeyEvent.META_CTRL_ON);
          assertTrue(v.onKeyDown(android.view.KeyEvent.KEYCODE_Z, undo));
          assertTrue("Undo must restore editable curve", v.hasPendingCurve());
          assertTrue(
              v.onKeyDown(
                  android.view.KeyEvent.KEYCODE_ESCAPE,
                  new android.view.KeyEvent(
                      android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ESCAPE)));
          assertFalse(v.hasPendingCurve());
        });
  }

  @Test
  public void lineModuleInsertsInterpolatingNodesAndKeepsMultipleLinesEditable() {
    instrumentation.runOnMainSync(() -> {
      DrawingView v = drawing();
      assertTrue(v.newDocument(128,128));
      v.setZoomPercent(100);
      v.setTool(DrawingView.LINE);
      gesture(v,10,20,110,20,false);
      assertEquals(2,v.curve.x.length);
      gesture(v,60,20,60,60,false);
      assertEquals(3,v.curve.x.length);
      assertEquals(60f,v.curve.evaluate(.5f,false),.001f);
      gesture(v,10,100,110,100,false);
      assertEquals(1,v.curve.others.size());
      assertTrue(v.hasPendingCurve());
      assertTrue(v.confirmCurve());
      assertFalse(v.hasPendingCurve());
      v.undo();
      assertTrue(v.hasPendingCurve());
      assertEquals(1,v.curve.others.size());
      assertEquals(3,v.curve.others.get(0).x.length);
      v.redo();
      assertFalse(v.hasPendingCurve());
    });
  }

  @Test
  public void strokesRespondOnTouchAndShapesPreviewBeforeRelease() {
    instrumentation.runOnMainSync(() -> {
      DrawingView v=drawing();
      for (int tool : new int[]{DrawingView.BRUSH,DrawingView.PENCIL}) {
        assertTrue(v.newDocument(128,128));
        v.setZoomPercent(100);
        v.setTool(tool);
        v.setColor(0xffff0000);
        Bitmap initial=v.snapshot();
        int initialPixel;
        try { initialPixel=initial.getPixel(32,32); } finally { initial.recycle(); }
        long now=android.os.SystemClock.uptimeMillis();
        float left=v.getWidth()/2f-64, top=v.getHeight()/2f-64;
        android.view.MotionEvent down=android.view.MotionEvent.obtain(now,now,
            android.view.MotionEvent.ACTION_DOWN,left+32,top+32,0);
        v.onTouchEvent(down);down.recycle();
        Bitmap dot=v.snapshot();
        try { assertTrue("Stroke must start on touch",dot.getPixel(32,32)!=initialPixel); }
        finally { dot.recycle(); }
        android.view.MotionEvent up=android.view.MotionEvent.obtain(now,now+10,
            android.view.MotionEvent.ACTION_UP,left+32,top+32,0);
        v.onTouchEvent(up);up.recycle();
        v.undo();
        Bitmap undone=v.snapshot();
        try { assertEquals("One undo removes the whole stroke",initialPixel,undone.getPixel(32,32)); }
        finally { undone.recycle(); }
      }
      for (int tool : new int[]{DrawingView.RECTANGLE,DrawingView.ELLIPSE,
          DrawingView.ROUNDED_RECTANGLE,DrawingView.FILLED_TRIANGLE,DrawingView.CIRCLE}) {
        assertTrue(v.newDocument(128,128));
        v.setTool(tool);
        long revision=v.revision();
        v.startX=10;v.startY=10;v.previousX=90;v.previousY=80;v.drawing=true;
        Bitmap overlay=Bitmap.createBitmap(128,128,Bitmap.Config.ARGB_8888);
        try {
          Tools.controller(tool).preview(v,new android.graphics.Canvas(overlay),1);
          int[] pixels=new int[128*128];overlay.getPixels(pixels,0,128,0,0,128,128);
          boolean visible=false;for(int pixel:pixels) if(pixel!=0){visible=true;break;}
          assertTrue("Preview while dragging: "+tool,visible);
          assertEquals("Preview must not change document history",revision,v.revision());
        } finally { overlay.recycle();v.drawing=false; }
      }
    });
  }

  private android.widget.SeekBar findBrushWidth(View view) {
    if(view instanceof android.widget.SeekBar &&
        "Anchura del pincel en píxeles".contentEquals(view.getContentDescription()==null ? "" : view.getContentDescription()))
      return (android.widget.SeekBar)view;
    if(view instanceof ViewGroup) {
      ViewGroup group=(ViewGroup)view;
      for(int i=0;i<group.getChildCount();i++) {
        android.widget.SeekBar found=findBrushWidth(group.getChildAt(i));
        if(found!=null)return found;
      }
    }
    return null;
  }

  @Test
  public void toolbarWidthDragChangesBrushInsteadOfScrollingToolbar() {
    instrumentation.runOnMainSync(() -> {
      android.widget.SeekBar slider=findBrushWidth(activity.getWindow().getDecorView());
      assertNotNull(slider);
      android.widget.HorizontalScrollView toolbar=(android.widget.HorizontalScrollView)slider.getParent().getParent();
      toolbar.scrollTo(Math.max(0,slider.getLeft()-20),0);
      slider.setProgress(31);
      int before=slider.getProgress(), initialScroll=toolbar.getScrollX();
      float track=slider.getWidth()-slider.getPaddingLeft()-slider.getPaddingRight();
      float x=slider.getLeft()-initialScroll+slider.getPaddingLeft()+track*before/slider.getMax();
      float y=slider.getTop()+slider.getHeight()/2f;
      long now=android.os.SystemClock.uptimeMillis();
      for(int step=0;step<5;step++) {
        int action=step==0 ? android.view.MotionEvent.ACTION_DOWN :
            step==4 ? android.view.MotionEvent.ACTION_UP : android.view.MotionEvent.ACTION_MOVE;
        android.view.MotionEvent event=android.view.MotionEvent.obtain(now,now+step*20,action,x+step*10,y,0);
        try { toolbar.dispatchTouchEvent(event); } finally { event.recycle(); }
      }
      assertTrue("Width slider must consume horizontal drag",slider.getProgress()>before);
      assertEquals(initialScroll,toolbar.getScrollX());
      assertEquals((slider.getProgress()+1)/2f,drawing().brushRadius(),.001f);
    });
  }

  @Test
  public void brushWidthSliderStaysInPlaceAcrossDigitChanges() {
    instrumentation.runOnMainSync(() -> {
      android.widget.SeekBar slider=findBrushWidth(activity.getWindow().getDecorView());
      assertNotNull(slider);
      assertEquals(299,slider.getMax());
      ViewGroup row=(ViewGroup)slider.getParent();
      int width=row.getWidth(),height=row.getHeight();
      int left=slider.getLeft(), sliderWidth=slider.getWidth();
      for(int progress:new int[]{0,1,8,9,98,99,254,255,298,299,0}) {
        slider.setProgress(progress);
        row.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));
        row.layout(row.getLeft(),row.getTop(),row.getLeft()+width,row.getTop()+height);
        assertEquals("Slider position must stay fixed",left,slider.getLeft());
        assertEquals("Slider track width must stay fixed",sliderWidth,slider.getWidth());
        assertEquals((progress+1)/2f,drawing().brushRadius(),.001f);
      }
    });
  }

  @Test
  public void toolModulesKeepSelectionAndPixelMovementIndependent() throws Exception {
    instrumentation.runOnMainSync(
        () -> {
          DrawingView v = drawing();
          Bitmap blank = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888);
          try {
            assertTrue(v.loadBitmap(blank));
          } finally {
            blank.recycle();
          }
          v.setZoomPercent(100);
          v.setTool(DrawingView.SELECT_RECTANGLE);
          gesture(v, 8, 8, 24, 24, false);
          assertTrue(v.hasSelection());
          v.setColor(0xffff0000);
          v.configureBrush(1, 1, false, false);
          v.setTool(DrawingView.FILLED_RECTANGLE);
          v.setToolOpacityPercent(100);
          gesture(v, 4, 4, 28, 28, false);
          long revision = v.revision();
          v.setTool(DrawingView.MOVE_SELECTION);
          gesture(v, 12, 12, 32, 32, false);
          assertEquals("Moving contour must not edit pixels", revision, v.revision());
          assertTrue(v.selectionContains(32, 32));
          assertFalse(v.selectionContains(12, 12));
          v.setTool(DrawingView.SELECT_RECTANGLE);
          gesture(v, 8, 8, 24, 24, false);
          v.setTool(DrawingView.MOVE_PIXELS);
          gesture(v, 12, 12, 32, 32, false);
        });
    LargeImportResponsivenessTests.waitResponsive(instrumentation,activity);
    instrumentation.runOnMainSync(() -> {
          DrawingView v=drawing();
          Bitmap image = v.snapshot();
          try {
            assertEquals(0, image.getPixel(12, 12));
            assertEquals(0xffff0000, image.getPixel(32, 32));
          } finally {
            image.recycle();
          }
          v.undo();
          image = v.snapshot();
          try {
            assertEquals(0xffff0000, image.getPixel(12, 12));
            assertEquals(0, image.getPixel(32, 32));
          } finally {
            image.recycle();
          }
        });
  }

  @Test
  public void everyToolIconRendersFromItsModule() {
    instrumentation.runOnMainSync(
        () -> {
          for (int tool = DrawingView.BRUSH; tool <= DrawingView.RECOLOR; tool++) {
            android.graphics.drawable.Drawable icon = Tools.icon(tool);
            icon.setBounds(0, 0, 48, 48);
            Bitmap image = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888);
            try {
              icon.draw(new android.graphics.Canvas(image));
              int[] pixels = new int[48 * 48];
              image.getPixels(pixels, 0, 48, 0, 0, 48, 48);
              boolean visible = false;
              for (int color : pixels) visible |= (color >>> 24) != 0;
              assertTrue("Tool must expose a visible icon " + tool, visible);
            } finally {
              image.recycle();
            }
          }
        });
  }
}
