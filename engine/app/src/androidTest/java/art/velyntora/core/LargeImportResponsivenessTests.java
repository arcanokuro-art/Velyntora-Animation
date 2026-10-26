package art.velyntora.core;

import static org.junit.Assert.*;
import android.app.Instrumentation;
import android.content.Intent;
import android.net.Uri;
import android.os.SystemClock;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.*;
import java.util.zip.*;
import org.junit.Test;

/** Exercises the actual file importer, rather than a blank document of similar dimensions. */
public final class LargeImportResponsivenessTests {
  @Test public void confirmedReductionAndLayerOperationsKeepMainThreadResponsive() throws Exception {
    Instrumentation inst=InstrumentationRegistry.getInstrumentation();
    MainActivity host=(MainActivity)inst.startActivitySync(new Intent(inst.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    File input=new File(host.getCacheDir(),"original-6067x9030.png");
    try {
      png(input,6067,9030);
      inst.runOnMainSync(()->host.importImage(Uri.fromFile(input)));
      waitResponsive(inst,host);
      inst.runOnMainSync(()->{
        android.app.AlertDialog dialog=host.readImportConfirmation();assertNotNull(dialog);
        android.widget.TextView message=dialog.findViewById(android.R.id.message);
        assertTrue(message.getText().toString().contains("4031 × 6000"));
        dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick();
      });
      waitResponsive(inst,host);
      inst.runOnMainSync(()->{
        DrawingView view=host.readDrawing();
        assertEquals(4031,view.documentWidth());assertEquals(6000,view.documentHeight());
        assertTrue((long)view.readBitmap().getWidth()*view.readBitmap().getHeight()<=1048576);
        assertTrue(view.isEnabled());assertEquals(1,view.layerCount());
        int color=DrawingView.nativeRegionPixels(1000,1700,1,1)[0];
        assertEquals(255,color>>>24);assertEquals(120,color&255);
        assertTrue(Math.abs(((color>>16)&255)-(int)(1000*6067.0/4031/32))<=1);
        assertTrue(Math.abs(((color>>8)&255)-(int)(1700*9030.0/6000/32))<=1);
        view.configureBrush(1,1,false,false);view.setTool(DrawingView.BRUSH);view.setColor(0xff123456);view.setBrushRadius(12);view.setToolOpacityPercent(100);
        for(int stroke=0;stroke<3;stroke++){
          long time=SystemClock.uptimeMillis();
          for(int action:new int[]{android.view.MotionEvent.ACTION_DOWN,android.view.MotionEvent.ACTION_UP}){
            android.view.MotionEvent event=android.view.MotionEvent.obtain(time,time+10,action,view.getWidth()/2f,view.getHeight()/2f,0);
            try{view.onTouchEvent(event);}finally{event.recycle();}
          }
          assertTrue("Repeated stroke blocked the UI",SystemClock.uptimeMillis()-time<1500);
        }
        assertEquals(0xff123456,DrawingView.nativeRegionPixels(2015,3000,1,1)[0]);

      });
      ContinuousFrameProbe.stroke(inst,host);
      for(int step=0;step<4;step++){
        inst.runOnMainSync(host::undo);waitResponsive(inst,host);
        inst.runOnMainSync(()->{
          DrawingView view=host.readDrawing();
          android.graphics.Bitmap visible=android.graphics.Bitmap.createBitmap(320,480,android.graphics.Bitmap.Config.ARGB_8888);
          try{
            android.graphics.Canvas canvas=new android.graphics.Canvas(visible);
            canvas.scale(320f/view.getWidth(),480f/view.getHeight());view.draw(canvas);
            // The native imported image itself remains opaque after each history step.
            for(int x:new int[]{0,1000,2015,4030})assertEquals(255,DrawingView.nativeRegionPixels(x,3000,1,1)[0]>>>24);
          }finally{visible.recycle();}
        });
      }
      inst.runOnMainSync(host::undo);assertEquals(0,DrawingView.nativeHistoryAvailability()&1);
      inst.runOnMainSync(()->host.undo());inst.runOnMainSync(()->assertNull(host.readProjectProgress()));
      for(int step=0;step<4;step++){inst.runOnMainSync(host::redo);waitResponsive(inst,host);}
      inst.runOnMainSync(host::addLayer);
      waitResponsive(inst,host);
      inst.runOnMainSync(()->{assertEquals(2,host.readDrawing().layerCount());host.toggleLayer();});
      waitResponsive(inst,host);
      inst.runOnMainSync(()->{assertFalse(host.readDrawing().layerVisible(1));host.moveLayer(-1);});
      waitResponsive(inst,host);
      inst.runOnMainSync(()->{assertEquals(0,host.readDrawing().activeLayer());host.deleteLayer();});
      waitResponsive(inst,host);
      inst.runOnMainSync(()->assertEquals(1,host.readDrawing().layerCount()));
    } finally {
      waitResponsive(inst,host);
      inst.runOnMainSync(()->{host.readDrawing().newDocument(64,64);host.finish();});input.delete();
    }
  }
  @Test public void cancellingOversizedImportPreservesDocument() throws Exception {
    Instrumentation inst=InstrumentationRegistry.getInstrumentation();
    MainActivity host=(MainActivity)inst.startActivitySync(new Intent(inst.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    File input=new File(host.getCacheDir(),"oversized-cancel.png");
    try {
      png(input,6001,2);
      inst.runOnMainSync(()->{assertTrue(host.readDrawing().newDocument(37,29));host.importImage(Uri.fromFile(input));});
      waitResponsive(inst,host);
      inst.runOnMainSync(()->{
        android.app.AlertDialog dialog=host.readImportConfirmation();assertNotNull(dialog);
        assertEquals(37,DrawingView.nativeWidth());assertEquals(29,DrawingView.nativeHeight());
        dialog.getButton(android.content.DialogInterface.BUTTON_NEGATIVE).performClick();
        assertEquals(37,host.readDrawing().documentWidth());assertEquals(29,host.readDrawing().documentHeight());
        assertTrue(host.readDrawing().isEnabled());assertNull(host.readProjectProgress());
      });
    }finally{inst.runOnMainSync(host::finish);input.delete();}
  }
  @Test public void oversizedJpegUsesConfirmedRegionReduction() throws Exception {
    Instrumentation inst=InstrumentationRegistry.getInstrumentation();
    MainActivity host=(MainActivity)inst.startActivitySync(new Intent(inst.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    File input=new File(host.getCacheDir(),"oversized-region.jpg");
    try {
      android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(6001,17,android.graphics.Bitmap.Config.ARGB_8888);
      try(OutputStream out=new FileOutputStream(input)){bitmap.eraseColor(0xff239751);assertTrue(bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG,100,out));}finally{bitmap.recycle();}
      inst.runOnMainSync(()->host.importImage(Uri.fromFile(input)));waitResponsive(inst,host);
      inst.runOnMainSync(()->{android.app.AlertDialog dialog=host.readImportConfirmation();assertNotNull(dialog);dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick();});
      waitResponsive(inst,host);
      inst.runOnMainSync(()->{
        assertEquals(6000,host.readDrawing().documentWidth());assertEquals(17,host.readDrawing().documentHeight());
        for(int x:new int[]{0,3000,5999}){
          int color=DrawingView.nativeRegionPixels(x,8,1,1)[0];
          assertEquals(255,color>>>24);assertTrue(Math.abs(((color>>16)&255)-35)<=3);
          assertTrue(Math.abs(((color>>8)&255)-151)<=3);assertTrue(Math.abs((color&255)-81)<=3);
        }
      });
    }finally{waitResponsive(inst,host);inst.runOnMainSync(()->{host.readDrawing().newDocument(64,64);host.finish();});input.delete();}
  }
  static void waitResponsive(Instrumentation inst,MainActivity host) throws Exception {
    long deadline=SystemClock.uptimeMillis()+60000;boolean[] busy={true};int heartbeats=0;long worst=0;
    do {
      long started=SystemClock.uptimeMillis();
      inst.runOnMainSync(()->busy[0]=host.readProjectProgress()!=null);
      long elapsed=SystemClock.uptimeMillis()-started;worst=Math.max(worst,elapsed);heartbeats++;
      assertTrue("UI heartbeat blocked for "+elapsed+" ms",elapsed<1500);
      if(!busy[0])break;
      assertTrue("Operation timed out",SystemClock.uptimeMillis()<deadline);
      Thread.sleep(40);
    } while(true);
    android.util.Log.i("LargeImportPerformance","heartbeats="+heartbeats+", worst main-thread wait="+worst+" ms");
  }
  private static void chunk(DataOutputStream out,String name,byte[] bytes) throws IOException {
    byte[] type=name.getBytes("US-ASCII");CRC32 crc=new CRC32();crc.update(type);crc.update(bytes);
    out.writeInt(bytes.length);out.write(type);out.write(bytes);out.writeInt((int)crc.getValue());
  }
  private static void png(File path,int width,int height) throws IOException {
    ByteArrayOutputStream compressed=new ByteArrayOutputStream();
    try(DeflaterOutputStream zip=new DeflaterOutputStream(compressed)) {
      byte[] row=new byte[1+width*3];
      for(int y=0;y<height;y++) {
        for(int x=0;x<width;x++){row[1+x*3]=(byte)(x/32);row[2+x*3]=(byte)(y/32);row[3+x*3]=(byte)120;}
        zip.write(row);
      }
    }
    try(DataOutputStream out=new DataOutputStream(new BufferedOutputStream(new FileOutputStream(path)))) {
      out.writeLong(0x89504e470d0a1a0aL);ByteArrayOutputStream header=new ByteArrayOutputStream();
      DataOutputStream data=new DataOutputStream(header);data.writeInt(width);data.writeInt(height);
      data.write(new byte[]{8,2,0,0,0});chunk(out,"IHDR",header.toByteArray());
      chunk(out,"IDAT",compressed.toByteArray());chunk(out,"IEND",new byte[0]);
    }
  }
}
