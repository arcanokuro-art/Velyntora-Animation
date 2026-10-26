package art.velyntora.core;

import static org.junit.Assert.*;
import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.*;
import android.view.View;
import android.view.ViewGroup;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;

public final class SelectionConfirmationDeviceTests {
  Instrumentation inst;MainActivity host;
  @Before public void open(){inst=InstrumentationRegistry.getInstrumentation();host=(MainActivity)inst.startActivitySync(new Intent(inst.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}
  @After public void close() throws Exception {
    LargeImportResponsivenessTests.waitResponsive(inst,host);
    inst.runOnMainSync(()->{host.readDrawing().newDocument(64,64);host.finish();});
  }
  View control(View view,String name){
    if(name.contentEquals(view.getContentDescription()==null?"":view.getContentDescription()))return view;
    if(view instanceof ViewGroup group)for(int i=0;i<group.getChildCount();i++){View found=control(group.getChildAt(i),name);if(found!=null)return found;}
    return null;
  }
  int[] prepare(){
    int[][] image={null};inst.runOnMainSync(()->{
      DrawingView v=host.readDrawing();Bitmap input=Bitmap.createBitmap(96,96,Bitmap.Config.ARGB_8888);input.eraseColor(Color.BLUE);
      Paint p=new Paint();p.setColor(Color.RED);new Canvas(input).drawRect(10,10,30,30,p);
      try{assertTrue(v.loadBitmap(input));}finally{input.recycle();}
      image[0]=DrawingView.nativeRegionPixels(0,0,96,96);
      v.installWandRegion(new Region(10,10,30,30));v.setTool(DrawingView.MOVE_PIXELS);host.syncToolState();
      assertEquals(View.VISIBLE,control(host.getWindow().getDecorView(),"Confirmar selección").getVisibility());
    });return image[0];
  }
  void move(int x,int y) throws Exception {
    inst.runOnMainSync(()->assertTrue(host.readDrawing().moveSelectedPixels(x,y)));
    LargeImportResponsivenessTests.waitResponsive(inst,host);
  }
  @Test public void cancelMultipleMovesRestoresOriginalSourceAndDestination() throws Exception {
    int[] original=prepare();move(15,0);move(0,18);move(12,8);
    inst.runOnMainSync(()->control(host.getWindow().getDecorView(),"Cancelar movimientos de selección").performClick());
    LargeImportResponsivenessTests.waitResponsive(inst,host);
    inst.runOnMainSync(()->{
      assertArrayEquals(original,DrawingView.nativeRegionPixels(0,0,96,96));assertFalse(host.readDrawing().hasSelection());
      assertEquals(View.GONE,control(host.getWindow().getDecorView(),"Confirmar selección").getVisibility());
    });
  }
  @Test public void confirmKeepsMovedPixelsAndClearsSelection() throws Exception {
    prepare();move(25,15);int[][] moved={null};
    inst.runOnMainSync(()->{
      moved[0]=DrawingView.nativeRegionPixels(0,0,96,96);
      control(host.getWindow().getDecorView(),"Confirmar selección").performClick();
      assertArrayEquals(moved[0],DrawingView.nativeRegionPixels(0,0,96,96));assertFalse(host.readDrawing().hasSelection());
      assertFalse(host.readDrawing().moveSelectedPixels(4,4));
    });
    inst.runOnMainSync(host::undo);LargeImportResponsivenessTests.waitResponsive(inst,host);
    inst.runOnMainSync(()->assertEquals(Color.RED,DrawingView.nativeRegionPixels(15,15,1,1)[0]));
  }
  @Test public void cancelWithoutMovementDoesNotEraseOrUndo() {
    int[] original=prepare();inst.runOnMainSync(()->{
      long revision=DrawingView.nativeRevision();host.readDrawing().readSelectionConfirmation().cancel();
      assertEquals(revision,DrawingView.nativeRevision());assertArrayEquals(original,DrawingView.nativeRegionPixels(0,0,96,96));
      assertFalse(host.readDrawing().hasSelection());
    });
  }
}
