package art.velyntora.core;

import static org.junit.Assert.*;
import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.*;
import android.os.SystemClock;
import android.view.MotionEvent;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;

public final class SelectionDragPreviewDeviceTests {
  private void event(DrawingView view,int action,float x,float y){
    long time=SystemClock.uptimeMillis();MotionEvent event=MotionEvent.obtain(time,time,action,
      (float)view.readViewport().screenX(x,y),(float)view.readViewport().screenY(x,y),0);
    try{assertTrue(view.onTouchEvent(event));}finally{event.recycle();}
  }
  @Test public void previewFollowsFingerWithoutEditingAndCancelRestoresImage() throws Exception {
    Instrumentation inst=InstrumentationRegistry.getInstrumentation();
    MainActivity host=(MainActivity)inst.startActivitySync(new Intent(inst.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    Bitmap displayed=Bitmap.createBitmap(800,800,Bitmap.Config.ARGB_8888);
    try{
      long[] revision={0};
      inst.runOnMainSync(()->{
        DrawingView view=host.readDrawing();Bitmap input=Bitmap.createBitmap(800,800,Bitmap.Config.ARGB_8888);
        input.eraseColor(Color.WHITE);Paint paint=new Paint();paint.setColor(Color.RED);
        new Canvas(input).drawRect(200,200,400,400,paint);
        try{assertTrue(view.loadBitmap(input));}finally{input.recycle();}
        view.installWandRegion(new Region(200,200,400,400));view.setTool(DrawingView.MOVE_PIXELS);
        revision[0]=view.revision();event(view,MotionEvent.ACTION_DOWN,250,250);event(view,MotionEvent.ACTION_MOVE,350,350);
      });
      boolean[] ready={false};long deadline=SystemClock.uptimeMillis()+5000;
      do{
        inst.runOnMainSync(()->{displayed.eraseColor(Color.GREEN);ready[0]=host.readDrawing().readSelectionDragPreview().draw(new Canvas(displayed));});
        if(!ready[0])Thread.sleep(20);
      }while(!ready[0] && SystemClock.uptimeMillis()<deadline);
      assertTrue("Pixel preview was never prepared",ready[0]);
      inst.runOnMainSync(()->{
        DrawingView view=host.readDrawing();assertEquals(Color.GREEN,displayed.getPixel(220,220));
        assertEquals(Color.RED,displayed.getPixel(450,450));
        assertEquals(Color.RED,DrawingView.nativeRegionPixels(220,220,1,1)[0]);
        assertEquals(Color.WHITE,DrawingView.nativeRegionPixels(450,450,1,1)[0]);
        assertEquals(revision[0],view.revision());event(view,MotionEvent.ACTION_CANCEL,350,350);
        assertFalse(view.readSelectionDragPreview().draw(new Canvas(displayed)));
        assertEquals(revision[0],view.revision());assertTrue(view.freeRegion.contains(220,220));
        event(view,MotionEvent.ACTION_DOWN,250,250);event(view,MotionEvent.ACTION_MOVE,350,350);event(view,MotionEvent.ACTION_UP,350,350);
      });
      LargeImportResponsivenessTests.waitResponsive(inst,host);
      inst.runOnMainSync(()->{
        assertEquals(0,DrawingView.nativeRegionPixels(220,220,1,1)[0]);
        assertEquals(Color.RED,DrawingView.nativeRegionPixels(450,450,1,1)[0]);
        assertFalse(host.readDrawing().readSelectionDragPreview().draw(new Canvas(displayed)));
      });
      inst.runOnMainSync(host::undo);LargeImportResponsivenessTests.waitResponsive(inst,host);
      inst.runOnMainSync(()->assertEquals(Color.RED,DrawingView.nativeRegionPixels(220,220,1,1)[0]));
      inst.runOnMainSync(host::addLayer);LargeImportResponsivenessTests.waitResponsive(inst,host);
      inst.runOnMainSync(()->{
        DrawingView view=host.readDrawing();view.installWandRegion(new Region(200,200,400,400));view.setTool(DrawingView.MOVE_PIXELS);
        event(view,MotionEvent.ACTION_DOWN,250,250);event(view,MotionEvent.ACTION_MOVE,350,350);
        assertFalse("Multilayer preview must not replace normal compositing",view.readSelectionDragPreview().draw(new Canvas(displayed)));
        event(view,MotionEvent.ACTION_CANCEL,350,350);
      });
    }finally{
      LargeImportResponsivenessTests.waitResponsive(inst,host);
      inst.runOnMainSync(()->{host.readDrawing().readSelectionDragPreview().clear();host.readDrawing().newDocument(64,64);host.finish();});
      displayed.recycle();
    }
  }
}
