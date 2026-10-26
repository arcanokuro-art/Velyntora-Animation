package art.velyntora.core;
import static org.junit.Assert.*;
import android.graphics.*;
import android.app.Instrumentation;
import android.content.Intent;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;

/** Cold native tile caches must never expose artificial checkerboard gaps. */
public final class NativeRenderingDeviceTests {
  @Test public void rotatedHistoryPreservesMeasuredDetailAndRapidCommands() throws Exception {
    Instrumentation inst=InstrumentationRegistry.getInstrumentation();
    MainActivity host=(MainActivity)inst.startActivitySync(new Intent(inst.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    try {
      inst.runOnMainSync(()->{
        assertTrue(host.readDrawing().newDocument(4031,6000));DrawingView.nativeBeginEdit();
        for(int x=1700;x<2200;x+=17)DrawingView.nativeStroke(x,1200,x+50,1700,1,0xff112233);
      });
      CanvasRendering.Frame before=CanvasRendering.prepareFrame(4f,new Rect(1200,800,2800,2400));
      Bitmap baseline=Bitmap.createBitmap(512,320,Bitmap.Config.ARGB_8888),actual=Bitmap.createBitmap(512,320,Bitmap.Config.ARGB_8888);
      Viewport viewport=new Viewport(4031,6000);viewport.scale=.49;viewport.angle=66.5;viewport.centerX=256;viewport.centerY=160;
      viewport.centerX-=viewport.screenX(2000,1500)-256;viewport.centerY-=viewport.screenY(2000,1500)-160;
      try {
        Canvas base=new Canvas(baseline);transform(base,viewport);before.display.draw(base,.49f);
        NativeTileDisplay.Preparation measured=((NativeTileDisplay)before.display).preparation();assertNotNull(measured);assertEquals(1,measured.sample());
        DrawingView.nativeBeginEdit();DrawingView.nativeStroke(2000,1500,2000,1500,8,0xff000000);assertTrue(DrawingView.nativeUndo());
        CanvasRendering.Frame restored=CanvasRendering.prepareFrame(.49f,new Rect(1200,800,2800,2400),measured);
        try {
          Canvas canvas=new Canvas(actual);transform(canvas,viewport);restored.display.draw(canvas,.49f);
          int[] expected=new int[512*320],pixels=new int[expected.length];baseline.getPixels(expected,0,512,0,0,512,320);actual.getPixels(pixels,0,512,0,0,512,320);
          assertArrayEquals("History reduced the measured resolution at 66.5 degrees",expected,pixels);
        }finally{restored.dispose();}
      }finally{before.dispose();baseline.recycle();actual.recycle();}
      inst.runOnMainSync(()->{
        for(int i=0;i<4;i++){DrawingView.nativeBeginEdit();DrawingView.nativeStroke(2400+i*20,2200,2400+i*20,2200,3,0xff000000);}
        android.view.View undo=findCommand(host.getWindow().getDecorView(),"Deshacer");assertNotNull(undo);
        for(int i=0;i<4;i++)undo.performClick();
      });
      waitMarkers(inst,host,false);
      inst.runOnMainSync(()->{
        android.view.View redo=findCommand(host.getWindow().getDecorView(),"Rehacer");assertNotNull(redo);
        for(int i=0;i<4;i++)redo.performClick();
      });
      waitMarkers(inst,host,true);
    }finally{LargeImportResponsivenessTests.waitResponsive(inst,host);inst.runOnMainSync(()->{host.readDrawing().newDocument(64,64);host.finish();});}
  }
  private static void transform(Canvas canvas,Viewport viewport){
    canvas.translate((float)viewport.centerX,(float)viewport.centerY);canvas.rotate((float)viewport.angle);canvas.scale((float)viewport.scale,(float)viewport.scale);canvas.translate(-4031/2f,-6000/2f);canvas.clipRect(0,0,4031,6000);
  }
  private static android.view.View findCommand(android.view.View view,String label){
    if(label.contentEquals(view.getContentDescription()==null?"":view.getContentDescription()))return view;
    if(view instanceof android.view.ViewGroup group)for(int i=0;i<group.getChildCount();i++){android.view.View found=findCommand(group.getChildAt(i),label);if(found!=null)return found;}
    return null;
  }
  private static void waitMarkers(Instrumentation inst,MainActivity host,boolean drawn) throws Exception {
    long deadline=android.os.SystemClock.uptimeMillis()+30000;boolean[] ready={false};
    while(!ready[0]){
      inst.runOnMainSync(()->{
        ready[0]=host.readProjectProgress()==null;
        for(int i=0;i<4;i++)ready[0]&=(DrawingView.nativeRegionPixels(2400+i*20,2200,1,1)[0]==(drawn?0xff000000:0xffffffff));
      });
      assertTrue("Rapid history commands were dropped",android.os.SystemClock.uptimeMillis()<deadline);
      if(!ready[0])Thread.sleep(40);
    }
  }
  @Test public void navigationNeverMixesPartialDetailAndRetainsSharpZoomOut() throws Exception {
    Instrumentation inst=InstrumentationRegistry.getInstrumentation();
    MainActivity host=(MainActivity)inst.startActivitySync(new Intent(inst.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    try {
      inst.runOnMainSync(()->{
        assertTrue(host.readDrawing().newDocument(6000,6000));
        DrawingView.nativeBeginEdit();
        for(int x=2650;x<3350;x+=17)DrawingView.nativeStroke(x,2650,x+100,3350,1,0xff112233);
      });
      CanvasRendering.Frame partial=CanvasRendering.prepareFrame(.8f,new Rect(2990,2990,3010,3010));
      Bitmap actual=Bitmap.createBitmap(512,512,Bitmap.Config.ARGB_8888),expected=Bitmap.createBitmap(512,512,Bitmap.Config.ARGB_8888);
      try {
        inst.runOnMainSync(()->{
          Canvas a=new Canvas(actual),e=new Canvas(expected);
          for(Canvas c:new Canvas[]{a,e}){c.translate(256,256);c.rotate(-28);c.scale(.8f,.8f);c.translate(-3000,-3000);}
          partial.display.draw(a,.8f);
          Paint paint=new Paint(Paint.FILTER_BITMAP_FLAG);
          e.drawBitmap(partial.bitmap,null,new Rect(0,0,partial.bitmap.getWidth()*8,partial.bitmap.getHeight()*8),paint);
          int[] ap=new int[512*512],ep=new int[ap.length];actual.getPixels(ap,0,512,0,0,512,512);expected.getPixels(ep,0,512,0,0,512,512);
          assertArrayEquals("Partial sharp blocks were exposed",ep,ap);
        });
      }finally{inst.runOnMainSync(partial::dispose);actual.recycle();expected.recycle();}
      CanvasRendering.Frame sharp=CanvasRendering.prepareFrame(4f,new Rect(2600,2600,3400,3400));
      Bitmap output=Bitmap.createBitmap(128,128,Bitmap.Config.ARGB_8888),reference=Bitmap.createBitmap(128,128,Bitmap.Config.ARGB_8888);
      Bitmap source=Bitmap.createBitmap(DrawingView.nativeSampleRegion(2600,2600,1,800,800),800,800,Bitmap.Config.ARGB_8888);
      try {
        inst.runOnMainSync(()->{
          Canvas a=new Canvas(output),e=new Canvas(reference);
          for(Canvas c:new Canvas[]{a,e}){c.translate(64,64);c.rotate(-28);c.scale(.49f,.49f);c.translate(-3000,-3000);}
          sharp.display.draw(a,.49f);
          e.drawBitmap(source,null,new Rect(2600,2600,3400,3400),new Paint(Paint.FILTER_BITMAP_FLAG));
          int[] ap=new int[128*128],ep=new int[ap.length];output.getPixels(ap,0,128,0,0,128,128);reference.getPixels(ep,0,128,0,0,128,128);
          // Rotated filtered tiles and a monolithic bitmap can round each color
          // channel differently by one or two levels; coarse resampling differs far more.
          for(int i=0;i<ap.length;i++){
            assertEquals("Zoom-out alpha at "+i,ep[i]>>>24,ap[i]>>>24);
            for(int shift:new int[]{0,8,16})assertTrue("Zoom-out lost detail at "+i+", channel "+shift,
                Math.abs(((ep[i]>>>shift)&255)-((ap[i]>>>shift)&255))<=2);
          }
        });
      }finally{inst.runOnMainSync(sharp::dispose);source.recycle();output.recycle();reference.recycle();}
    }finally{inst.runOnMainSync(()->{host.readDrawing().newDocument(64,64);host.finish();});}
  }
  @Test public void preparedHistoryFrameIsSharpOnItsFirstDraw() throws Exception {
    Instrumentation inst=InstrumentationRegistry.getInstrumentation();
    MainActivity host=(MainActivity)inst.startActivitySync(new Intent(inst.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    try {
      inst.runOnMainSync(()->{
        assertTrue(host.readDrawing().newDocument(6000,6000));
        DrawingView.nativeBeginEdit();DrawingView.nativeStroke(2960,2960,3040,3040,1,0xff112233);
      });
      // Preparation, including detailed blocks, runs off the UI just like history operations.
      for(int step=0;step<3;step++){
        if(step==1)assertTrue(DrawingView.nativeUndo());
        if(step==2)assertTrue(DrawingView.nativeRedo());
        CanvasRendering.Frame frame=CanvasRendering.prepareFrame(4f,new Rect(2920,2920,3080,3080));
        Bitmap actual=Bitmap.createBitmap(256,256,Bitmap.Config.ARGB_8888);
        Bitmap expected=Bitmap.createBitmap(256,256,Bitmap.Config.ARGB_8888);
        Bitmap source=Bitmap.createBitmap(DrawingView.nativeSampleRegion(2920,2920,1,160,160),160,160,Bitmap.Config.ARGB_8888);
        try {
          Canvas a=new Canvas(actual),e=new Canvas(expected);
          for(Canvas c:new Canvas[]{a,e}){c.translate(128,128);c.rotate(-8);c.scale(4,4);c.translate(-3000,-3000);}
          frame.display.draw(a,4f);
          Paint exact=new Paint();exact.setFilterBitmap(false);
          e.drawBitmap(source,null,new Rect(2920,2920,3080,3080),exact);
          for(int y=0;y<256;y++)for(int x=0;x<256;x++)assertEquals("Coarse history flash, step "+step+" at "+x+","+y,expected.getPixel(x,y),actual.getPixel(x,y));
        }finally{frame.dispose();actual.recycle();expected.recycle();source.recycle();}
      }
    }finally{inst.runOnMainSync(()->{host.readDrawing().newDocument(64,64);host.finish();});}
  }
  @Test public void largeColdCacheCoversZoomAndRotationAndHistoryBoundaries() throws Exception {
    Instrumentation inst=InstrumentationRegistry.getInstrumentation();
    MainActivity host=(MainActivity)inst.startActivitySync(new Intent(inst.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    try {
      inst.runOnMainSync(()->{
        assertTrue(host.readDrawing().newDocument(6000,6000));
        assertEquals(0,DrawingView.nativeHistoryAvailability());
        host.undo();host.redo();assertNull(host.readProjectProgress());
        for(float zoom:new float[]{.15f,.8f,1.3f,4f}){
          CanvasRendering.Frame frame=CanvasRendering.prepareFrame(zoom);
          Bitmap output=Bitmap.createBitmap(512,512,Bitmap.Config.ARGB_8888);
          try {
            output.eraseColor(0xffff00ff);Canvas canvas=new Canvas(output);
            canvas.translate(256,256);canvas.rotate(19.7f);canvas.scale(zoom,zoom);canvas.translate(-3000,-3000);
            canvas.clipRect(0,0,6000,6000);frame.display.draw(canvas,zoom);
            for(int y=0;y<512;y++)for(int x=0;x<512;x++)assertEquals("Missing overview at zoom "+zoom,0xffffffff,output.getPixel(x,y));
          }finally{frame.dispose();output.recycle();}
        }
        DrawingView.nativeBeginEdit();DrawingView.nativeStroke(3000,3000,3000,3000,8,0xff111111);
        assertEquals(1,DrawingView.nativeHistoryAvailability());assertTrue(DrawingView.nativeUndo());
        assertEquals(2,DrawingView.nativeHistoryAvailability());assertFalse(DrawingView.nativeUndo());
        assertTrue(DrawingView.nativeRedo());assertEquals(1,DrawingView.nativeHistoryAvailability());
      });
    }finally{inst.runOnMainSync(()->{host.readDrawing().newDocument(64,64);host.finish();});}
  }
}
