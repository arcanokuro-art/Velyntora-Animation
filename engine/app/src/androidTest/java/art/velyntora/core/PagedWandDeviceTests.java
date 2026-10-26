package art.velyntora.core;

import static org.junit.Assert.*;
import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Region;
import android.graphics.Rect;
import android.graphics.Path;
import android.os.SystemClock;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;

public final class PagedWandDeviceTests {
  @Test public void spanMaskPreservesHolesAndExactPixels(){
    Region region=new Region(3,5,17,19);region.op(7,9,12,14,Region.Op.DIFFERENCE);
    Rect bounds=new Rect(2,4,20,21);byte[] mask=PagedSelectionMove.mask(region,bounds);
    for(int y=bounds.top;y<bounds.bottom;y++)for(int x=bounds.left;x<bounds.right;x++)
      assertEquals(region.contains(x,y)?1:0,mask[(y-bounds.top)*bounds.width()+x-bounds.left]);
    Path display=PagedWand.displayBoundary(region,4031,6000);
    assertFalse(display.isEmpty());
    assertTrue(region.contains(3,5));assertFalse(region.contains(8,10));
  }
  @Test public void smallSelectionMovementRendersContinuousFrames() throws Exception {
    Instrumentation inst=InstrumentationRegistry.getInstrumentation();
    MainActivity host=(MainActivity)inst.startActivitySync(new Intent(inst.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    try{
      inst.runOnMainSync(()->{
        DrawingView v=host.readDrawing();assertTrue(v.newDocument(800,800));
        v.installWandRegion(new Region(180,180,620,620));v.setTool(DrawingView.MOVE_PIXELS);
      });
      ContinuousFrameProbe.stroke(inst,host);
      LargeImportResponsivenessTests.waitResponsive(inst,host);
      inst.runOnMainSync(()->assertTrue(host.readDrawing().hasSelection()));
      for(int i=0;i<6;i++){
        int direction=i%2==0?-1:1;
        inst.runOnMainSync(()->assertTrue(host.readDrawing().moveSelectedPixels(direction*4,direction*3)));
        LargeImportResponsivenessTests.waitResponsive(inst,host);
      }
    }finally{
      LargeImportResponsivenessTests.waitResponsive(inst,host);
      inst.runOnMainSync(()->{host.readDrawing().newDocument(64,64);host.finish();});
    }
  }
  @Test public void largeWandAndPixelMoveKeepMainThreadResponsive() throws Exception {
    Instrumentation inst=InstrumentationRegistry.getInstrumentation();
    MainActivity host=(MainActivity)inst.startActivitySync(new Intent(inst.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    try{
      inst.runOnMainSync(()->assertTrue(host.readDrawing().newDocument(4031,6000)));
      inst.runOnMainSync(()->{
        DrawingView v=host.readDrawing();v.setWandTolerance(0);
        long start=SystemClock.uptimeMillis();v.selectMatchingRegion(2000,3000);
        assertTrue("Wand entry blocked UI",SystemClock.uptimeMillis()-start<1000);
      });
      LargeImportResponsivenessTests.waitResponsive(inst,host);
      inst.runOnMainSync(()->{
        DrawingView v=host.readDrawing();assertTrue(v.freeRegion.contains(0,0));assertTrue(v.freeRegion.contains(4030,5999));
        Region selected=new Region(0,0,4000,5900);
        v.installWandRegion(selected,PagedWand.displayBoundary(selected,4031,6000));
        long start=SystemClock.uptimeMillis();assertTrue(v.moveSelectedPixels(20,30));
        assertTrue("Movement entry blocked UI",SystemClock.uptimeMillis()-start<1000);
      });
      LargeImportResponsivenessTests.waitResponsive(inst,host);
      inst.runOnMainSync(()->{
        DrawingView v=host.readDrawing();assertFalse(v.freeRegion.contains(0,0));assertTrue(v.freeRegion.contains(20,30));
        assertEquals(0xffffffff,DrawingView.nativeRegionPixels(2000,3000,1,1)[0]);
      });
      inst.runOnMainSync(host::undo);LargeImportResponsivenessTests.waitResponsive(inst,host);
      inst.runOnMainSync(()->assertEquals(0xffffffff,DrawingView.nativeRegionPixels(0,0,1,1)[0]));
    }finally{
      LargeImportResponsivenessTests.waitResponsive(inst,host);
      inst.runOnMainSync(()->{host.readDrawing().newDocument(64,64);host.finish();});
    }
  }
}
