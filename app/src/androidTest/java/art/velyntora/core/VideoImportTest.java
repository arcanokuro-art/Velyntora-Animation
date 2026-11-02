package art.velyntora.core;

import static org.junit.Assert.*;
import android.content.*;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.SystemClock;
import androidx.test.platform.app.InstrumentationRegistry;
import app.velyntora.animation.video.*;
import app.velyntora.animation.storage.AnimationProject;
import java.io.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import org.junit.Test;

public final class VideoImportTest {
  private final android.app.Instrumentation i=InstrumentationRegistry.getInstrumentation();
  private AnimationSession session(AnimationActivity host) {
    try {
      java.lang.reflect.Field f=AnimationActivity.class.getDeclaredField("session");
      f.setAccessible(true);return (AnimationSession)f.get(host);
    }catch(Exception e){throw new AssertionError(e);}
  }
  private boolean busy(AnimationActivity host) {
    try {
      java.lang.reflect.Field f=AnimationActivity.class.getDeclaredField("busy");
      f.setAccessible(true);return f.getBoolean(host);
    }catch(Exception e){throw new AssertionError(e);}
  }
  private void await(BooleanSupplier check) {
    long deadline=SystemClock.uptimeMillis()+30000;
    AtomicBoolean done=new AtomicBoolean();
    do {
      i.runOnMainSync(()->done.set(check.getAsBoolean()));
      if(done.get())return;
      SystemClock.sleep(30);
    }while(SystemClock.uptimeMillis()<deadline);
    throw new AssertionError("Video import timed out");
  }
  @Test public void realVideoTrimFpsPixelsCancellationAndEditableFrames() throws Exception {
    File file=new File(i.getTargetContext().getCacheDir(),"video-fixture.mp4");
    try(InputStream in=i.getContext().getAssets().open("video-import-test.mp4");
        OutputStream out=new FileOutputStream(file)){
      byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);
    }
    AnimationActivity host=(AnimationActivity)i.startActivitySync(
        new Intent(i.getTargetContext(),AnimationActivity.class)
          .setAction(AnimationActivity.ACTION).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          .putExtra("width",96).putExtra("height",64).putExtra("fps",12));
    try {
      await(()->!busy(host)&&session(host).project()!=null);
      VideoImportRequest request=new VideoImportRequest(Uri.fromFile(file),"Video de prueba",
          250000,1250000,4,64,48);
      assertEquals(4,request.frameCount);
      assertEquals(1000000,request.timestampUs(3));
      AnimationSession session=session(host);
      AnimationProject original=session.project();
      i.runOnMainSync(()->{
        DrawingView.nativeBeginEdit();
        DrawingView.nativeStroke(10,20,60,20,3,0xff000000);
      });
      int[] originalPixels=DrawingView.nativePixels();
      AtomicBoolean canceled=new AtomicBoolean();
      try{
        session.importVideo(i.getTargetContext(),request,canceled,done->{if(done==2)canceled.set(true);});
        fail("Cancellation must stop import");
      }catch(CancellationException expected){}
      assertSame(original,session.project());
      assertArrayEquals(originalPixels,DrawingView.nativePixels());
      assertTrue((DrawingView.nativeHistoryAvailability()&1)!=0);
      // Decode expected frames independently, then test the actual Activity import command.
      int[][] expected=new int[4][];
      try(VideoFrameDecoder decoder=new VideoFrameDecoder(i.getTargetContext(),request)){
        for(int frame=0;frame<4;frame++){
          Bitmap image=decoder.frame(frame);
          expected[frame]=new int[64*48];
          image.getPixels(expected[frame],0,64,0,0,64,48);image.recycle();
        }
      }
      i.runOnMainSync(()->host.importVideo(request));
      await(()->!busy(host)&&session.project()!=original);
      assertEquals(4,session.project().sequence.size());
      assertEquals(4,session.project().sequence.fps());
      for(int frame=0;frame<4;frame++){
        final int index=frame;
        i.runOnMainSync(()->host.select(index));
        await(()->!busy(host)&&session.project().sequence.selected()==index);
        assertEquals(64,DrawingView.nativeWidth());assertEquals(48,DrawingView.nativeHeight());
        assertArrayEquals("Extracted frame "+index,expected[index],DrawingView.nativePixels());
      }
      assertTrue(new File(original.directory,"project.properties").isFile());
      // Imported frames are native editable documents, not playback-only images.
      i.runOnMainSync(()->{
        DrawingView.nativeBeginEdit();DrawingView.nativeStroke(8,12,50,12,2,0xffff00ff);
        host.undo();
      });
      await(()->host.readProjectProgress()==null);
      assertArrayEquals(expected[3],DrawingView.nativePixels());
    } finally {
      if(!host.isDestroyed()){i.runOnMainSync(host::onBackPressed);await(host::isDestroyed);}
      file.delete();
    }
  }
}
