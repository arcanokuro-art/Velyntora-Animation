package art.velyntora.core;

import static org.junit.Assert.*;
import android.app.Instrumentation;
import android.content.Intent;
import android.view.Menu;
import android.view.MotionEvent;
import android.widget.PopupMenu;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;

public final class FigurasDeviceTests {
  @Test public void groupsSelectAllNineFiguresAndPreserveDrawingAndHistory() throws Exception {
    Instrumentation inst=InstrumentationRegistry.getInstrumentation();
    MainActivity host=(MainActivity)inst.startActivitySync(new Intent(inst.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    try{
      inst.runOnMainSync(()->{
        DrawingView view=host.readDrawing();view.setTool(DrawingView.BRUSH);
        PopupMenu popup=new PopupMenu(host,view);Menu menu=FigurasSelector.populate(popup.getMenu(),view);
        assertEquals(2,menu.size());assertEquals("Sin relleno",menu.getItem(0).getTitle());assertEquals("Con relleno",menu.getItem(1).getTitle());
        assertEquals(5,menu.getItem(0).getSubMenu().size());assertEquals(4,menu.getItem(1).getSubMenu().size());
        assertEquals("Opening the selector must keep the active tool",DrawingView.BRUSH,view.currentTool());
        assertSame(host.readToolButtons().get(DrawingView.RECTANGLE),host.readToolButtons().get(DrawingView.FILLED_TRIANGLE));
        for(int group=0;group<2;group++)for(int i=0;i<menu.getItem(group).getSubMenu().size();i++){
          android.view.MenuItem item=menu.getItem(group).getSubMenu().getItem(i);int id=item.getItemId();
          assertNotNull(item.getIcon());assertTrue(FigurasModule.contains(id));assertEquals(FigurasModule.label(id),item.getTitle());
          assertTrue(view.newDocument(96,96));view.fitCanvas();view.setColor(0xff000000);view.setBrushRadius(2);FigurasModule.select(view,id);view.setToolOpacityPercent(100);host.syncToolState();
          assertEquals(id,view.currentTool());assertEquals(id,FigurasModule.selected(view));assertTrue(host.readToolButtons().get(id).isSelected());
          assertEquals(FigurasModule.label(id),host.readSelectedTool().getText().toString());
          touch(view,MotionEvent.ACTION_DOWN,16,16);touch(view,MotionEvent.ACTION_MOVE,70,70);touch(view,MotionEvent.ACTION_UP,70,70);
          int[] pixels=DrawingView.nativeRegionPixels(0,0,96,96);int painted=0;for(int pixel:pixels)if(pixel!=0xffffffff)painted++;
          assertTrue("Figure did not draw: "+id,painted>0);
          assertEquals("Fill group must match its tool",group==1?0xff000000:0xffffffff,pixels[43*96+43]);
          assertTrue(DrawingView.nativeUndo());assertEquals(0xffffffff,DrawingView.nativeRegionPixels(43,43,1,1)[0]);
          assertTrue(DrawingView.nativeRedo());assertArrayEquals(pixels,DrawingView.nativeRegionPixels(0,0,96,96));
        }
        view.setTool(DrawingView.BRUSH);host.syncToolState();assertFalse(host.readToolButtons().get(DrawingView.RECTANGLE).isSelected());
        host.getSharedPreferences("figure-selector",0).edit().remove("selected").apply();
      });
    }finally{inst.runOnMainSync(host::finish);}
  }
  @Test public void actualDropdownNavigatesBothGroups() throws Exception {
    Instrumentation inst=InstrumentationRegistry.getInstrumentation();
    MainActivity host=(MainActivity)inst.startActivitySync(new Intent(inst.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    try{
      inst.runOnMainSync(()->{host.readDrawing().setTool(DrawingView.BRUSH);host.syncToolState();host.readToolScroll().setVisibility(android.view.View.VISIBLE);host.readToolButtons().get(DrawingView.RECTANGLE).performClick();});
      inst.waitForIdleSync();capture(inst,"figuras-grupos");click(inst,"Con relleno");inst.waitForIdleSync();capture(inst,"figuras-con-relleno");click(inst,"Elipse rellena");inst.waitForIdleSync();
      inst.runOnMainSync(()->{assertEquals(DrawingView.FILLED_ELLIPSE,host.readDrawing().currentTool());host.readToolScroll().setVisibility(android.view.View.VISIBLE);host.readToolButtons().get(DrawingView.RECTANGLE).performClick();});
      inst.waitForIdleSync();click(inst,"Sin relleno");inst.waitForIdleSync();capture(inst,"figuras-sin-relleno");click(inst,"Rectángulo redondeado");inst.waitForIdleSync();
      inst.runOnMainSync(()->{assertEquals(DrawingView.ROUNDED_RECTANGLE,host.readDrawing().currentTool());host.getSharedPreferences("figure-selector",0).edit().remove("selected").apply();});
    }finally{inst.runOnMainSync(host::finish);}
  }
  private static void click(Instrumentation inst,String text) throws Exception {
    long deadline=android.os.SystemClock.uptimeMillis()+5000;
    do{
      android.view.accessibility.AccessibilityNodeInfo root=inst.getUiAutomation().getRootInActiveWindow();
      if(root!=null)for(android.view.accessibility.AccessibilityNodeInfo node:root.findAccessibilityNodeInfosByText(text)){
        if(text.contentEquals(node.getText()==null?"":node.getText())){
          for(int i=0;i<5 && node!=null;i++,node=node.getParent())if(node.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK))return;
        }
      }
      Thread.sleep(50);
    }while(android.os.SystemClock.uptimeMillis()<deadline);
    fail("Missing figure menu entry: "+text);
  }
  private static void capture(Instrumentation inst,String name)throws Exception{
    String directory="/sdcard/Download/velyntora-core-workspace";
    for(String command:new String[]{"mkdir -p "+directory,"screencap -p "+directory+"/"+name+".png"})try(android.os.ParcelFileDescriptor result=inst.getUiAutomation().executeShellCommand(command);java.io.FileInputStream input=new java.io.FileInputStream(result.getFileDescriptor())){byte[] buffer=new byte[1024];while(input.read(buffer)!=-1){}}
  }
  private static void touch(DrawingView view,int action,float x,float y){
    MotionEvent event=MotionEvent.obtain(0,android.os.SystemClock.uptimeMillis(),action,(float)view.readViewport().screenX(x,y),(float)view.readViewport().screenY(x,y),0);
    try{view.onTouchEvent(event);}finally{event.recycle();}
  }
}
