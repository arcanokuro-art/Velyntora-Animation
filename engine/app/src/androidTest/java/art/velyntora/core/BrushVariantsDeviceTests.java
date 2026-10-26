package art.velyntora.core;

import static org.junit.Assert.*;
import android.app.Instrumentation;
import android.content.Intent;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;

/** Real selector, persisted variant, touch input, native pixels and history. */
public final class BrushVariantsDeviceTests {
  Instrumentation inst;MainActivity activity;
  @Before public void open() {
    inst=InstrumentationRegistry.getInstrumentation();
    activity=(MainActivity)inst.startActivitySync(new Intent(inst.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    inst.waitForIdleSync();
    inst.runOnMainSync(()->{
      DrawingView v=activity.readDrawing();v.brushes().select(BrushModule.CLASSIC);
      v.configureBrush(1,1,false,false);activity.syncToolState();
    });
  }
  @After public void close() {
    inst.runOnMainSync(()->{activity.readDrawing().brushes().select(BrushModule.CLASSIC);activity.readDrawing().setToolOpacityPercent(100);activity.finish();});
    inst.waitForIdleSync();
  }
  void blank(DrawingView v) {
    assertTrue(v.newDocument(64,64));assertTrue(DrawingView.nativeImport(new int[4096]));
    v.refresh();v.setZoomPercent(100);v.rotateView(-v.rotationDegrees());
  }
  void event(DrawingView v,int action,float x,float y) {
    long t=android.os.SystemClock.uptimeMillis();
    MotionEvent e=MotionEvent.obtain(t,t,action,v.getWidth()/2f-32+x,v.getHeight()/2f-32+y,0);
    try{v.onTouchEvent(e);}finally{e.recycle();}
  }
  void stroke(DrawingView v) {
    event(v,MotionEvent.ACTION_DOWN,12.2f,12.4f);
    event(v,MotionEvent.ACTION_MOVE,44.2f,30.4f);
    event(v,MotionEvent.ACTION_UP,44.2f,30.4f);
  }
  @Test public void variantsProduceDistinctEdgesAndUndoExactly() {
    inst.runOnMainSync(()->{
      DrawingView v=activity.readDrawing();
      for(int id:new int[]{BrushModule.CLASSIC,BrushModule.ANTIALIASED}) {
        blank(v);v.brushes().select(id);v.setBrushRadius(4);v.setColor(0xff000000);v.setToolOpacityPercent(100);
        stroke(v);int fractional=0,opaque=0;
        for(int p:DrawingView.nativeRemoveSource()){int a=p>>>24;if(a>0&&a<255)fractional++;if(a==255)opaque++;}
        assertTrue(opaque>20);
        if(id==BrushModule.CLASSIC)assertEquals(0,fractional);else assertTrue(fractional>20);
        v.undo();for(int p:DrawingView.nativeRemoveSource())assertEquals(0,p);
        v.redo();assertTrue(DrawingView.nativeRemoveSource()[20*64+26]!=0);
      }
    });
  }
  @Test public void comparisonCaptureAndWidthEndpoints() {
    inst.runOnMainSync(()->{
      DrawingView v=activity.readDrawing();blank(v);v.setColor(0xff000000);v.setBrushRadius(3);
      v.brushes().select(BrushModule.CLASSIC);
      event(v,MotionEvent.ACTION_DOWN,8.2f,12.4f);event(v,MotionEvent.ACTION_UP,54.2f,22.4f);
      v.brushes().select(BrushModule.ANTIALIASED);
      event(v,MotionEvent.ACTION_DOWN,8.2f,36.4f);event(v,MotionEvent.ACTION_UP,54.2f,46.4f);
      v.refresh();v.setZoomPercent(500);
    });
    capture("brush-comparison");
    inst.runOnMainSync(()->{
      DrawingView v=activity.readDrawing();blank(v);v.brushes().select(BrushModule.ANTIALIASED);
      v.setBrushRadius(.5f);stroke(v);int count=0;for(int p:DrawingView.nativeRemoveSource())if(p!=0)count++;
      assertTrue(count>20);v.undo();for(int p:DrawingView.nativeRemoveSource())assertEquals(0,p);
      v.setBrushRadius(150);stroke(v);for(int p:DrawingView.nativeRemoveSource())assertEquals(0xff000000,p);
    });
  }
  @Test public void smoothStrokeOpacityAndSelectionRemainBounded() {
    inst.runOnMainSync(()->{
      DrawingView v=activity.readDrawing();blank(v);v.brushes().select(BrushModule.ANTIALIASED);
      v.setBrushRadius(4);v.setColor(0xff000000);v.setToolOpacityPercent(50);
      event(v,MotionEvent.ACTION_DOWN,20.2f,20.4f);
      for(int i=0;i<20;i++)event(v,MotionEvent.ACTION_MOVE,i%2==0?40.2f:20.2f,20.4f);
      event(v,MotionEvent.ACTION_UP,40.2f,20.4f);
      assertEquals(128,DrawingView.nativeRemoveSource()[20*64+30]>>>24);
      v.undo();for(int p:DrawingView.nativeRemoveSource())assertEquals(0,p);
      byte[] mask=new byte[4096];mask[20*64+30]=1;DrawingView.nativeSetBrushSelection(mask);
      assertTrue(DrawingView.nativeBeginOpacityStroke(true));DrawingView.nativeBeginEdit();
      DrawingView.nativeAntialiasedStroke(20.2f,20.4f,40.2f,20.4f,4,0xff000000,.5f,1,false);
      DrawingView.nativeEndOpacityStroke();int count=0;for(int p:DrawingView.nativeRemoveSource())if(p!=0)count++;
      assertEquals(1,count);DrawingView.nativeSetBrushSelection(null);
    });
  }
  void capture(String name) {
    inst.waitForIdleSync();
    String dir="/sdcard/Download/velyntora-core-workspace";
    for(String command:new String[]{"mkdir -p "+dir,"screencap -p "+dir+"/"+name+".png"}) {
      try(android.os.ParcelFileDescriptor result=inst.getUiAutomation().executeShellCommand(command);
          java.io.FileInputStream input=new java.io.FileInputStream(result.getFileDescriptor())) {
        byte[] buffer=new byte[1024];while(input.read(buffer)!=-1){}
      } catch(java.io.IOException e) {throw new AssertionError(e);}
    }
  }
  @Test public void iconDropdownSelectsAndRemembersVariant() {
    inst.runOnMainSync(()->activity.readToolButtons().get(DrawingView.BRUSH).performClick());
    inst.waitForIdleSync();
    capture("brush-selector");
    AccessibilityNodeInfo root=inst.getUiAutomation().getRootInActiveWindow();assertNotNull(root);
    java.util.List<AccessibilityNodeInfo> nodes=root.findAccessibilityNodeInfosByText("Pincel con bordes suavizados");
    assertFalse("Brush dropdown must contain smooth brush",nodes.isEmpty());
    AccessibilityNodeInfo node=nodes.get(0);while(node!=null&&!node.isClickable())node=node.getParent();
    assertNotNull(node);assertTrue(node.performAction(AccessibilityNodeInfo.ACTION_CLICK));inst.waitForIdleSync();
    inst.runOnMainSync(()->{
      DrawingView v=activity.readDrawing();assertEquals(BrushModule.ANTIALIASED,v.brushes().selected());
      assertEquals(DrawingView.BRUSH,v.currentTool());assertEquals("Pincel suavizado",activity.readSelectedTool().getText().toString());
      assertEquals(BrushModule.ANTIALIASED,new DrawingView(activity).brushes().selected());
      v.setTool(DrawingView.PENCIL);assertEquals(BrushModule.ANTIALIASED,v.brushes().selected());
      v.setTool(DrawingView.BRUSH);assertTrue(v.brushes().isAntialiased());
    });
  }
}
