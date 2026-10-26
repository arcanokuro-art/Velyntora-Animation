package art.velyntora.core;

import static org.junit.Assert.*;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;

public final class CurvePointMenuDeviceTests {
  Instrumentation inst;MainActivity host;
  @Before public void open(){
    inst=InstrumentationRegistry.getInstrumentation();host=(MainActivity)inst.startActivitySync(new Intent(inst.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    inst.runOnMainSync(()->{
      DrawingView v=host.readDrawing();v.newDocument(256,256);v.setZoomPercent(100);v.setTool(DrawingView.LINE);
      v.curve=new CurveDraft(20,50,180,50);v.curve.insert(100,50,2);v.curveHistory.clear();
    });inst.waitForIdleSync();
  }
  @After public void close(){inst.runOnMainSync(()->{host.readDrawing().cancelToolGesture();host.readDrawing().cancelCurve();host.finish();});}
  void event(DrawingView v,long t,int action,float x,float y){
    MotionEvent e=MotionEvent.obtain(t,t+(action==MotionEvent.ACTION_DOWN?0:20),action,(float)v.readViewport().screenX(x,y),(float)v.readViewport().screenY(x,y),0);
    try{v.onTouchEvent(e);}finally{e.recycle();}
  }
  void gesture(float x,float y,float endX,float endY){
    inst.runOnMainSync(()->{DrawingView v=host.readDrawing();long t=SystemClock.uptimeMillis();event(v,t,0,x,y);
      if(x!=endX || y!=endY)event(v,t,2,endX,endY);event(v,t,1,endX,endY);});inst.waitForIdleSync();
  }
  java.util.List<AccessibilityNodeInfo> deleteNodes(){
    AccessibilityNodeInfo root=inst.getUiAutomation().getRootInActiveWindow();assertNotNull(root);return root.findAccessibilityNodeInfosByText("Borrar punto");
  }
  @Test public void tapDeletesOnlyInteriorAndSupportsUndoRedo() throws Exception {
    long[] revision={0};inst.runOnMainSync(()->revision[0]=DrawingView.nativeRevision());
    gesture(100,50,100,50);assertFalse(deleteNodes().isEmpty());
    assertTrue(deleteNodes().get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK));inst.waitForIdleSync();
    inst.runOnMainSync(()->{DrawingView v=host.readDrawing();assertEquals(2,v.curve.x.length);assertEquals(20,v.curve.x[0],.01);assertEquals(180,v.curve.x[1],.01);assertEquals(revision[0],DrawingView.nativeRevision());});
    inst.runOnMainSync(host::undo);inst.runOnMainSync(()->assertEquals(3,host.readDrawing().curve.x.length));
    inst.runOnMainSync(host::redo);inst.runOnMainSync(()->assertEquals(2,host.readDrawing().curve.x.length));
    inst.runOnMainSync(()->assertTrue(host.readDrawing().confirmCurve()));
    inst.runOnMainSync(host::undo);LargeImportResponsivenessTests.waitResponsive(inst,host);
    inst.runOnMainSync(()->assertEquals(2,host.readDrawing().curve.x.length));
  }
  @Test public void endpointsDoNotOpenMenuAndInteriorDragDoesNotOpenMenu(){
    gesture(20,50,20,50);assertTrue(deleteNodes().isEmpty());
    gesture(180,50,180,50);assertTrue(deleteNodes().isEmpty());
    gesture(100,50,100,100);assertTrue(deleteNodes().isEmpty());
    inst.runOnMainSync(()->{assertEquals(3,host.readDrawing().curve.x.length);assertEquals(100,host.readDrawing().curve.y[1],.01);});
  }
  @Test public void newlyInsertedPointRequiresSeparateTap(){
    inst.runOnMainSync(()->host.readDrawing().curve=new CurveDraft(20,50,180,50));
    gesture(100,50,100,50);assertTrue(deleteNodes().isEmpty());
    gesture(100,50,100,50);assertFalse(deleteNodes().isEmpty());
  }
}
