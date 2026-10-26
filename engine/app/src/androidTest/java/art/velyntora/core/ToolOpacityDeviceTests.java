package art.velyntora.core;

import static org.junit.Assert.*;
import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;

/** Exercises all 19 tools against real Android rasterization, input and JNI. */
public final class ToolOpacityDeviceTests {
  Instrumentation inst; MainActivity activity;
  @Before public void open() {
    inst=InstrumentationRegistry.getInstrumentation();
    activity=(MainActivity)inst.startActivitySync(new Intent(inst.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    inst.waitForIdleSync();
    inst.runOnMainSync(()->{
      DrawingView v=activity.readDrawing();
      for(int id=0;id<29;id++) if(ToolOpacity.supports(id)){v.setTool(id);v.setToolOpacityPercent(100);}
      v.setTool(DrawingView.BRUSH);v.setBrushRadius(3);v.configureBrush(1,1,false,false);
    });
    inst.waitForIdleSync();
  }
  @After public void close() {
    inst.runOnMainSync(()->{
      DrawingView v=activity.readDrawing();v.cancelCurve();
      for(int id=0;id<29;id++) if(ToolOpacity.supports(id)){v.setTool(id);v.setToolOpacityPercent(100);}
      activity.finish();
    });inst.waitForIdleSync();
  }
  void blank(DrawingView v,int color) {
    assertTrue(v.newDocument(64,64));
    int[] pixels=new int[4096];java.util.Arrays.fill(pixels,color);
    assertTrue(DrawingView.nativeImport(pixels));v.refresh();v.setZoomPercent(100);
    v.rotateView(-v.rotationDegrees());
  }
  void event(DrawingView v,int action,float x,float y,long time) {
    MotionEvent e=MotionEvent.obtain(time,time+10,action,v.getWidth()/2f-32+x,v.getHeight()/2f-32+y,0);
    try{v.onTouchEvent(e);}finally{e.recycle();}
  }
  void gesture(DrawingView v) {
    long t=android.os.SystemClock.uptimeMillis();
    event(v,MotionEvent.ACTION_DOWN,12.5f,12.5f,t);
    event(v,MotionEvent.ACTION_MOVE,44.5f,44.5f,t);
    event(v,MotionEvent.ACTION_UP,44.5f,44.5f,t);
  }
  void paint(DrawingView v,int tool) {
    if(tool==DrawingView.TEXT) assertTrue(v.insertText("Ab",8,8,24,false,false,"sans-serif"));
    else {gesture(v);if(tool==DrawingView.LINE) assertTrue(v.confirmCurve());}
  }
  @Test public void allPaintToolsApplyHalfOpacityAndUndo() {
    inst.runOnMainSync(()->{
      DrawingView v=activity.readDrawing();
      for(int tool:new int[]{0,1,2,3,4,7,8,15,16,17,18,19,20,23,24,25}) {
        blank(v,0);v.setTool(tool);v.setToolOpacityPercent(50);v.setColor(0xffff0000);
        v.setSecondaryColor(0xff0000ff);v.configureGradient(0,false);
        paint(v,tool);int max=0;int[] pixels=DrawingView.nativeRemoveSource();
        for(int pixel:pixels) max=Math.max(max,pixel>>>24);
        assertTrue("Tool "+tool+" must paint",max>100);
        assertTrue("Tool "+tool+" must cap alpha at half",max<=128);
        if(tool==DrawingView.LINE) {
          v.setToolOpacityPercent(90);v.undo();
          assertTrue(v.hasPendingCurve());assertEquals(50,v.toolOpacityPercent());v.cancelCurve();
        } else v.undo();
        for(int pixel:DrawingView.nativeRemoveSource()) assertEquals("Undo tool "+tool,0,pixel);
      }
    });
  }
  @Test public void continuousStrokeIsUniformAndSeparateStrokeAccumulates() {
    inst.runOnMainSync(()->{
      DrawingView v=activity.readDrawing();
      for(int tool:new int[]{DrawingView.BRUSH,DrawingView.PENCIL,DrawingView.ERASER}) {
        blank(v,tool==DrawingView.ERASER?0xff123456:0);v.setTool(tool);v.setToolOpacityPercent(50);v.setColor(0xffff0000);
        long t=android.os.SystemClock.uptimeMillis();event(v,MotionEvent.ACTION_DOWN,24.5f,24.5f,t);
        for(int i=0;i<20;i++) event(v,MotionEvent.ACTION_MOVE,i%2==0?40.5f:24.5f,24.5f,t);
        event(v,MotionEvent.ACTION_UP,40.5f,24.5f,t);
        assertEquals("Uniform stroke "+tool,128,DrawingView.nativeRemoveSource()[24*64+32]>>>24);
        event(v,MotionEvent.ACTION_DOWN,24.5f,24.5f,t);event(v,MotionEvent.ACTION_UP,40.5f,24.5f,t);
        int alpha=DrawingView.nativeRemoveSource()[24*64+32]>>>24;
        assertEquals(tool==DrawingView.ERASER?64:192,alpha);
        v.undo();assertEquals(128,DrawingView.nativeRemoveSource()[24*64+32]>>>24);
      }
    });
  }
  @Test public void sampledToolsHaveHalfIntensityAndDoNotAccumulateWithinGesture() {
    inst.runOnMainSync(()->{
      DrawingView v=activity.readDrawing();
      for(int tool:new int[]{DrawingView.CLONE,DrawingView.RECOLOR}) {
        blank(v,0xff0000ff);v.setTool(tool);v.setToolOpacityPercent(50);v.setColor(0xffff0000);
        if(tool==DrawingView.CLONE) {
          int[] pixels=DrawingView.nativeRemoveSource();for(int y=0;y<64;y++) for(int x=0;x<16;x++) pixels[y*64+x]=0xffff0000;
          assertTrue(DrawingView.nativeImport(pixels));v.refresh();
          long t=android.os.SystemClock.uptimeMillis();event(v,MotionEvent.ACTION_DOWN,8.5f,24.5f,t);event(v,MotionEvent.ACTION_UP,8.5f,24.5f,t);
        }
        long t=android.os.SystemClock.uptimeMillis();event(v,MotionEvent.ACTION_DOWN,32.5f,24.5f,t);
        for(int i=0;i<20;i++) event(v,MotionEvent.ACTION_MOVE,i%2==0?34.5f:32.5f,24.5f,t);
        event(v,MotionEvent.ACTION_UP,34.5f,24.5f,t);
        assertEquals(0xff800080,DrawingView.nativeRemoveSource()[24*64+33]);
        v.undo();assertEquals(0xff0000ff,DrawingView.nativeRemoveSource()[24*64+33]);
      }
    });
  }
  @Test public void zeroOpacityNeverEditsPixelsOrHistory() {
    inst.runOnMainSync(()->{
      DrawingView v=activity.readDrawing();
      for(int tool=0;tool<29;tool++) if(ToolOpacity.supports(tool)) {
        blank(v,0xff123456);v.setTool(tool);v.setToolOpacityPercent(0);
        long revision=v.revision();int[] before=DrawingView.nativeRemoveSource();
        gesture(v);if(tool==DrawingView.TEXT) assertFalse(v.insertText("A",8,8,24,false,false,"sans-serif"));
        assertArrayEquals("Zero tool "+tool,before,DrawingView.nativeRemoveSource());assertEquals(revision,v.revision());
      }
    });
  }
  SeekBar slider(View view) {
    if(view instanceof SeekBar && "Opacidad de la herramienta".contentEquals(view.getContentDescription()==null?"":view.getContentDescription()))return (SeekBar)view;
    if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){SeekBar found=slider(((ViewGroup)view).getChildAt(i));if(found!=null)return found;}
    return null;
  }
  @Test public void colorAlphaAndSelectionCombineWithToolOpacity() {
    inst.runOnMainSync(()->{
      DrawingView v=activity.readDrawing();blank(v,0);v.setTool(DrawingView.SELECT_RECTANGLE);
      long t=android.os.SystemClock.uptimeMillis();
      event(v,MotionEvent.ACTION_DOWN,20,20,t);event(v,MotionEvent.ACTION_UP,30,30,t);
      assertTrue(v.hasSelection());v.setTool(DrawingView.BRUSH);v.setToolOpacityPercent(50);v.setColor(0x80ff0000);
      event(v,MotionEvent.ACTION_DOWN,12.5f,24.5f,t);event(v,MotionEvent.ACTION_UP,44.5f,24.5f,t);
      int[] pixels=DrawingView.nativeRemoveSource();assertEquals(0x40ff0000,pixels[24*64+24]);
      assertEquals(0,pixels[24*64+12]);assertEquals(0,pixels[24*64+40]);
      v.undo();for(int pixel:DrawingView.nativeRemoveSource())assertEquals(0,pixel);
    });
  }
  @Test public void opacitySliderConsumesDragInsideScrollingToolbar() {
    inst.runOnMainSync(()->{
      activity.readDrawing().setTool(DrawingView.BRUSH);
      SeekBar slider=slider(activity.getWindow().getDecorView());assertNotNull(slider);
      android.widget.HorizontalScrollView toolbar=(android.widget.HorizontalScrollView)slider.getParent().getParent();
      toolbar.scrollTo(Math.max(0,slider.getLeft()-20),0);slider.setProgress(30);
      int initialScroll=toolbar.getScrollX();
      float track=slider.getWidth()-slider.getPaddingLeft()-slider.getPaddingRight();
      float x=slider.getLeft()-initialScroll+slider.getPaddingLeft()+track*.3f;
      float y=slider.getTop()+slider.getHeight()/2f;long now=android.os.SystemClock.uptimeMillis();
      for(int i=0;i<5;i++) {
        MotionEvent e=MotionEvent.obtain(now,now+i*20,i==0?MotionEvent.ACTION_DOWN:i==4?MotionEvent.ACTION_UP:MotionEvent.ACTION_MOVE,x+i*10,y,0);
        try{toolbar.dispatchTouchEvent(e);}finally{e.recycle();}
      }
      assertTrue(slider.getProgress()>30);assertEquals(initialScroll,toolbar.getScrollX());
      assertEquals(slider.getProgress(),activity.readDrawing().toolOpacityPercent());
    });
  }
  @Test public void brushWidthEndpointsApplyToBrushPencilAndSampledTools() {
    inst.runOnMainSync(()->{
      DrawingView v=activity.readDrawing();
      for(int tool:new int[]{DrawingView.BRUSH,DrawingView.PENCIL,DrawingView.RECOLOR,DrawingView.ERASER}) {
        assertTrue(v.newDocument(400,400));v.setZoomPercent(100);v.setTool(tool);
        v.setColor(0xffff0000);v.setToolOpacityPercent(100);v.configureBrush(1,1,false,false);
        int[] source=new int[160000];java.util.Arrays.fill(source,0xff0000ff);
        assertTrue(DrawingView.nativeImport(source));v.refresh();v.setBrushRadius(150);
        long t=android.os.SystemClock.uptimeMillis();
        for(int action:new int[]{MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP}) {
          MotionEvent e=MotionEvent.obtain(t,t+10,action,v.getWidth()/2f+.5f,v.getHeight()/2f+.5f,0);
          try{v.onTouchEvent(e);}finally{e.recycle();}
        }
        int[] painted=DrawingView.nativeRemoveSource();
        assertTrue("300 px must reach 149 pixels from center: "+tool,painted[200*400+349]!=source[200*400+349]);
        assertEquals("300 px must stop outside its radius: "+tool,source[200*400+351],painted[200*400+351]);
        v.undo();assertArrayEquals(source,DrawingView.nativeRemoveSource());
        v.setBrushRadius(.5f);
        for(int action:new int[]{MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP}) {
          MotionEvent e=MotionEvent.obtain(t,t+10,action,v.getWidth()/2f,v.getHeight()/2f,0);
          try{v.onTouchEvent(e);}finally{e.recycle();}
        }
        painted=DrawingView.nativeRemoveSource();assertTrue(painted[200*400+200]!=source[200*400+200]);
        assertEquals(source[200*400+201],painted[200*400+201]);
      }
      v.setBrushRadius(4);
    });
  }
  @Test public void toolbarRemembersIndependentValuesAcrossRelayoutAndNewView() {
    inst.runOnMainSync(()->{
      DrawingView v=activity.readDrawing();SeekBar control=slider(activity.getWindow().getDecorView());assertNotNull(control);
      v.setTool(DrawingView.BRUSH);control.setProgress(35);assertEquals(35,v.toolOpacityPercent());
      v.setTool(DrawingView.LINE);control.setProgress(60);assertEquals(60,v.toolOpacityPercent());
      v.setTool(DrawingView.BRUSH);assertEquals(35,control.getProgress());assertEquals(1,v.layerOpacity(),.001);
      activity.layoutWorkspace();control=slider(activity.getWindow().getDecorView());assertEquals(35,control.getProgress());
      v.setTool(DrawingView.PENCIL);control.setProgress(25);assertEquals(25,v.toolOpacityPercent());
      v.setTool(DrawingView.REMOVE_AI);assertEquals(View.GONE,control.getVisibility());
      v.setTool(DrawingView.SELECT_RECTANGLE);assertEquals(View.GONE,control.getVisibility());
      DrawingView reopened=new DrawingView(activity);reopened.setTool(DrawingView.BRUSH);assertEquals(35,reopened.toolOpacityPercent());
      reopened.setTool(DrawingView.LINE);assertEquals(60,reopened.toolOpacityPercent());
    });
  }
}
