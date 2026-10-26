package art.velyntora.core;

import static org.junit.Assert.*;
import android.app.Instrumentation;
import android.content.Intent;
import android.view.MotionEvent;
import android.view.Menu;
import android.widget.PopupMenu;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;
import java.util.*;

/** Exercises every packaged preset through actual touch input and native undo. */
public final class VelyntoraBrushDeviceTests {
 Instrumentation inst;MainActivity activity;
 @Before public void open(){inst=InstrumentationRegistry.getInstrumentation();activity=(MainActivity)inst.startActivitySync(new Intent(inst.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));inst.waitForIdleSync();}
 @After public void close(){inst.runOnMainSync(()->{DrawingView v=activity.readDrawing();v.brushes().select(BrushModule.CLASSIC);v.configureBrush(1,1,false,false);activity.finish();});inst.waitForIdleSync();}
 void blank(DrawingView v){assertTrue(v.newDocument(64,64));assertTrue(DrawingView.nativeImport(new int[4096]));v.refresh();v.setZoomPercent(100);v.rotateView(-v.rotationDegrees());v.configureBrush(1,1,false,false);v.setBrushRadius(8);v.setColor(0xff1234ee);v.setToolOpacityPercent(100);}
 void event(DrawingView v,int action,float x,float y){long t=android.os.SystemClock.uptimeMillis();MotionEvent e=MotionEvent.obtain(t,t,action,v.getWidth()/2f-32+x,v.getHeight()/2f-32+y,0);try{v.onTouchEvent(e);}finally{e.recycle();}}
 void stroke(DrawingView v){event(v,MotionEvent.ACTION_DOWN,12,32);event(v,MotionEvent.ACTION_MOVE,48,32);event(v,MotionEvent.ACTION_UP,48,32);}
 @Test public void allOriginalPresetsLoadPaintAndUndo(){
  inst.runOnMainSync(()->{
   DrawingView v=activity.readDrawing();assertEquals(34,v.brushes().presets.size());
   for(VelyntoraBrushCatalog.Preset p:v.brushes().presets){
    blank(v);v.brushes().select(p.id);assertEquals(p.id,v.brushes().selected());
    if(p.category.equals("Mezcla")){int[] pixels=new int[4096];for(int y=0;y<64;y++)for(int x=0;x<64;x++)pixels[y*64+x]=x<32?0xffff0000:0xff0000ff;assertTrue(DrawingView.nativeImport(pixels));v.refresh();}
    int[] before=DrawingView.nativeRemoveSource();stroke(v);int[] after=DrawingView.nativeRemoveSource();
    assertFalse("Preset must produce an actual stroke: "+p.name,Arrays.equals(before,after));
    v.undo();assertArrayEquals("Undo: "+p.name,before,DrawingView.nativeRemoveSource());
    v.redo();assertArrayEquals("Redo: "+p.name,after,DrawingView.nativeRemoveSource());
   }
  });
 }
 @Test public void dropdownHasNineCategoriesAndIndependentPresets(){
  inst.runOnMainSync(()->{
   DrawingView v=activity.readDrawing();Menu menu=new PopupMenu(activity,v).getMenu();BrushSelector.populate(menu,v);
   assertEquals(11,menu.size());Set<Integer> ids=new HashSet<>();int categories=0;
   for(int i=0;i<menu.size();i++)if(menu.getItem(i).hasSubMenu()){categories++;Menu sub=menu.getItem(i).getSubMenu();for(int j=0;j<sub.size();j++){assertTrue(ids.add(sub.getItem(j).getItemId()));assertNotNull(sub.getItem(j).getIcon());}}
   assertEquals(9,categories);assertEquals(34,ids.size());
   v.brushes().select(110);assertEquals(110,new DrawingView(activity).brushes().selected());
  });
 }
 @Test public void opacityZeroAndBlendOnTransparentDoNotPaint(){
  inst.runOnMainSync(()->{
   DrawingView v=activity.readDrawing();
   for(VelyntoraBrushCatalog.Preset p:v.brushes().presets){blank(v);v.brushes().select(p.id);v.setToolOpacityPercent(0);stroke(v);for(int pixel:DrawingView.nativeRemoveSource())assertEquals(0,pixel);}
   for(VelyntoraBrushCatalog.Preset p:v.brushes().presets)if(p.category.equals("Mezcla")){blank(v);v.brushes().select(p.id);stroke(v);for(int pixel:DrawingView.nativeRemoveSource())assertEquals("No pigment on transparent: "+p.name,0,pixel);}
  });
 }
}
