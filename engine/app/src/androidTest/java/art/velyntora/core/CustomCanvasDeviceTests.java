package art.velyntora.core;

import static org.junit.Assert.*;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;

/** Actual custom-size dialog, large canvas display, strokes, history and project reopening. */
public final class CustomCanvasDeviceTests {
  Instrumentation inst;MainActivity activity;
  @Before public void open(){
    inst=InstrumentationRegistry.getInstrumentation();
    activity=(MainActivity)inst.startActivitySync(new Intent(inst.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));inst.waitForIdleSync();
  }
  @After public void close(){inst.runOnMainSync(()->{activity.readDrawing().brushes().select(BrushModule.CLASSIC);activity.readDrawing().newDocument(64,64);activity.finish();});inst.waitForIdleSync();}
  void set(String label,String text){
    inst.waitForIdleSync();AccessibilityNodeInfo root=inst.getUiAutomation().getRootInActiveWindow();assertNotNull(root);
    java.util.List<AccessibilityNodeInfo> nodes=root.findAccessibilityNodeInfosByText(label);assertFalse(nodes.isEmpty());
    Bundle data=new Bundle();data.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,text);
    assertTrue(nodes.get(0).performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,data));inst.waitForIdleSync();
  }
  void apply(){
    AccessibilityNodeInfo root=inst.getUiAutomation().getRootInActiveWindow();java.util.List<AccessibilityNodeInfo> nodes=root.findAccessibilityNodeInfosByText("Aplicar");assertFalse(nodes.isEmpty());assertTrue(nodes.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK));inst.waitForIdleSync();
  }
  void tap(DrawingView v){
    long t=android.os.SystemClock.uptimeMillis();
    for(int action:new int[]{MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP}){
      MotionEvent e=MotionEvent.obtain(t,t+10,action,v.getWidth()/2f,v.getHeight()/2f,0);try{v.onTouchEvent(e);}finally{e.recycle();}
    }
  }
  @Test public void custom2560x1600CreatesDrawsAndReopens() {
    inst.runOnMainSync(()->activity.configureDimensions(0));inst.waitForIdleSync();
    set("Ancho en píxeles","2560");set("Alto en píxeles","1600");apply();
    inst.runOnMainSync(()->{
      DrawingView v=activity.readDrawing();assertEquals(2560,v.documentWidth());assertEquals(1600,v.documentHeight());
      assertTrue((long)v.readBitmap().getWidth()*v.readBitmap().getHeight()<=1048576);
      v.setTool(DrawingView.BRUSH);v.configureBrush(1,1,false,false);v.setColor(0xff123456);v.setBrushRadius(10);
      v.rotateView(-v.rotationDegrees());
      for(int id:new int[]{BrushModule.CLASSIC,BrushModule.ANTIALIASED}){
        v.brushes().select(id);tap(v);v.refresh();assertEquals(0xff123456,DrawingView.nativeRegionPixels(1280,800,1,1)[0]);
        v.undo();assertEquals(0xffffffff,DrawingView.nativeRegionPixels(1280,800,1,1)[0]);v.redo();v.undo();
      }
      android.graphics.Bitmap image=v.snapshot();java.io.ByteArrayOutputStream output=new java.io.ByteArrayOutputStream();
      assertTrue(image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,output));image.recycle();
      android.graphics.BitmapFactory.Options bounds=new android.graphics.BitmapFactory.Options();bounds.inJustDecodeBounds=true;
      byte[] png=output.toByteArray();android.graphics.BitmapFactory.decodeByteArray(png,0,png.length,bounds);
      assertEquals(2560,bounds.outWidth);assertEquals(1600,bounds.outHeight);
      java.io.File file=new java.io.File(activity.getCacheDir(),"large-canvas-test.vlycore");
      try(android.os.ParcelFileDescriptor fd=android.os.ParcelFileDescriptor.open(file,android.os.ParcelFileDescriptor.MODE_CREATE|android.os.ParcelFileDescriptor.MODE_READ_WRITE|android.os.ParcelFileDescriptor.MODE_TRUNCATE)){
        assertTrue(DrawingView.nativeSaveProject(fd.getFd()));assertTrue(v.newDocument(32,32));
        android.system.Os.lseek(fd.getFileDescriptor(),0,android.system.OsConstants.SEEK_SET);
        assertTrue(DrawingView.nativeOpenProject(fd.getFd()));v.projectOpened();
        assertEquals(2560,v.documentWidth());assertEquals(1600,v.documentHeight());
      }catch(Exception e){throw new AssertionError(e);}finally{file.delete();}
    });capture();
  }
  @Test public void fourKRefreshUsesExactDimensionsAndInvalidSizePreservesDocument(){
    inst.runOnMainSync(()->{
      DrawingView v=activity.readDrawing();assertTrue(v.newDocument(3840,2160));
      assertEquals(3840,v.documentWidth());assertEquals(2160,v.documentHeight());assertTrue(v.readBitmap().getWidth()<=3840);
      assertEquals(0xffffffff,DrawingView.nativeRegionPixels(3839,2159,1,1)[0]);
      assertFalse(v.newDocument(6001,6000));assertFalse(v.newDocument(6001,1));assertEquals(3840,v.documentWidth());
    });
  }
  @Test public void tenThousandBoundaryKeepsLayersSparseAndUndoExact(){
    inst.runOnMainSync(()->{
      DrawingView v=activity.readDrawing();assertTrue(v.newDocument(6000,6000));
      assertEquals(6000,v.documentWidth());assertEquals(6000,v.documentHeight());
      assertTrue((long)v.readBitmap().getWidth()*v.readBitmap().getHeight()<=1048576);
      assertTrue(v.readBitmap().isMutable());
      for(int i=0;i<31;i++)assertTrue(v.addLayer());
      assertEquals(32,v.layerCount());assertFalse(v.addLayer());
      DrawingView.nativeBeginEdit();
      DrawingView.nativeStyledStroke(5000.5f,5000.5f,5000.5f,5000.5f,4,0xff123456,1,1,false,false);
      assertEquals(0xff123456,DrawingView.nativeRegionPixels(5000,5000,1,1)[0]);
      assertTrue(DrawingView.nativeUndo());assertEquals(0xffffffff,DrawingView.nativeRegionPixels(5000,5000,1,1)[0]);
      assertTrue(DrawingView.nativeRedo());assertEquals(0xff123456,DrawingView.nativeRegionPixels(5000,5000,1,1)[0]);
      assertFalse(v.newDocument(6001,6000));assertEquals(6000,v.documentWidth());
      assertTrue(v.newDocument(64,64));
    });
  }
  @Test public void repeatedStrokesAndLayersOnLargeImportedDimensions(){
    inst.runOnMainSync(()->{
      DrawingView v=activity.readDrawing();assertTrue(v.newDocument(3034,4515));
      v.rotateView(-v.rotationDegrees());
      v.setBrushRadius(12);v.configureBrush(1,1,false,false);v.setColor(0xff123456);
      for(int layer=0;layer<3;layer++){
        assertTrue(v.addLayer());
        for(int tool:new int[]{DrawingView.BRUSH,DrawingView.PENCIL,DrawingView.ERASER}){
          v.setTool(tool);v.setToolOpacityPercent(tool==DrawingView.ERASER?50:100);
          for(int stroke=0;stroke<3;stroke++)tap(v);
          int[] actual=DrawingView.nativeRegionPixels(1517,2257,1,1);
          assertNotNull(actual);
          assertEquals(1,actual.length);
        }
      }
      assertEquals(4,v.layerCount());
      v.setTool(DrawingView.BRUSH);v.setToolOpacityPercent(100);v.setColor(0xffabcdef);tap(v);
      assertEquals(0xffabcdef,DrawingView.nativeRegionPixels(1517,2257,1,1)[0]);
      v.undo();v.redo();assertEquals(0xffabcdef,DrawingView.nativeRegionPixels(1517,2257,1,1)[0]);
      java.io.File file=new java.io.File(activity.getCacheDir(),"large-layers-test.vlycore");
      try(android.os.ParcelFileDescriptor fd=android.os.ParcelFileDescriptor.open(file,android.os.ParcelFileDescriptor.MODE_CREATE|android.os.ParcelFileDescriptor.MODE_READ_WRITE|android.os.ParcelFileDescriptor.MODE_TRUNCATE)){
        assertTrue(DrawingView.nativeSaveProject(fd.getFd()));assertTrue(v.newDocument(64,64));
        android.system.Os.lseek(fd.getFileDescriptor(),0,android.system.OsConstants.SEEK_SET);
        assertTrue(DrawingView.nativeOpenProject(fd.getFd()));v.projectOpened();assertEquals(4,v.layerCount());
        assertEquals(0xffabcdef,DrawingView.nativeRegionPixels(1517,2257,1,1)[0]);
      }catch(Exception e){throw new AssertionError(e);}finally{file.delete();}
      assertTrue(v.newDocument(800,800));
    });
  }
  void capture(){
    inst.waitForIdleSync();android.os.SystemClock.sleep(3000);
    String dir="/sdcard/Download/velyntora-core-workspace";
    for(String cmd:new String[]{"mkdir -p "+dir,"screencap -p "+dir+"/custom-canvas.png"}){
      try(android.os.ParcelFileDescriptor result=inst.getUiAutomation().executeShellCommand(cmd);java.io.FileInputStream input=new java.io.FileInputStream(result.getFileDescriptor())){byte[] b=new byte[1024];while(input.read(b)!=-1){}}
      catch(java.io.IOException e){throw new AssertionError(e);}
    }
  }
}
