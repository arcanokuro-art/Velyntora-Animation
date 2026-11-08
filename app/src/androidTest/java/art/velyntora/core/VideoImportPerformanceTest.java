package art.velyntora.core;
import static org.junit.Assert.*;import android.content.Context;import android.graphics.*;import android.media.MediaMetadataRetriever;import android.net.Uri;import android.os.SystemClock;
import androidx.test.platform.app.InstrumentationRegistry;import app.velyntora.animation.video.*;import app.velyntora.animation.video.background.*;import app.velyntora.animation.model.*;import app.velyntora.animation.storage.*;
import java.io.*;import java.util.*;import java.util.concurrent.atomic.AtomicBoolean;import org.junit.Test;
public final class VideoImportPerformanceTest{
 @Test public void sequentialImportIsLosslessAndMeasuredAgainstPreviousPipeline()throws Exception{
  Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();File source=new File(context.getFilesDir(),"performance-video.mp4");
  try(InputStream in=InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("import-performance.mp4");OutputStream out=new FileOutputStream(source)){
   byte[] data=new byte[32768];int n;while((n=in.read(data))!=-1)out.write(data,0,n);
  }
  VideoImportRequest request=new VideoImportRequest(Uri.fromFile(source),"Rendimiento",0,6000000,12,640,360);
  // Independent platform reference: dimensions and flat-region color agree before PNG storage.
  try(VideoFrameDecoder decoder=new VideoFrameDecoder(context,request)){
   MediaMetadataRetriever reference=new MediaMetadataRetriever();
   try{
    reference.setDataSource(context,request.uri);Bitmap actual=decoder.frame(0),expected=reference.getFrameAtTime(0,MediaMetadataRetriever.OPTION_CLOSEST);
    assertEquals(expected.getWidth(),actual.getWidth());assertEquals(expected.getHeight(),actual.getHeight());
    for(int[] point:new int[][]{{53,90},{269,180},{587,270}}){
     int a=actual.getPixel(point[0],point[1]),b=expected.getPixel(point[0],point[1]);
     assertTrue("Color rojo",Math.abs(Color.red(a)-Color.red(b))<=5);
     assertTrue("Color verde",Math.abs(Color.green(a)-Color.green(b))<=5);
     assertTrue("Color azul",Math.abs(Color.blue(a)-Color.blue(b))<=5);
    }actual.recycle();expected.recycle();
    for(int index=1;index<request.frameCount;index++){Bitmap frame=decoder.frame(index);frame.recycle();}
    assertEquals(1,decoder.seekCount);assertTrue(decoder.decodedFrames<=181);assertTrue(decoder.convertedFrames<=request.frameCount);
   }finally{reference.release();}
  }
  DrawingView.nativeHistoryDirectory(context.getCacheDir().getAbsolutePath());DrawingView.nativeImageStorage(context.getCacheDir().getAbsolutePath());
  AnimationProject old=new AnimationProject(new File(context.getCacheDir(),"old-import-"+AnimationSequence.uuid()),"Anterior",640,360,12);
  FrameDocumentAdapter adapter=new FrameDocumentAdapter(null);ArrayList<AnimationSequence.Frame> frames=new ArrayList<>();
  long before=SystemClock.elapsedRealtime();MediaMetadataRetriever legacy=new MediaMetadataRetriever();
  try{
   legacy.setDataSource(context,request.uri);
   for(int index=0;index<request.frameCount;index++){
    Bitmap bitmap=legacy.getScaledFrameAtTime(request.timestampUs(index),MediaMetadataRetriever.OPTION_CLOSEST,640,360);
    assertNotNull(bitmap);try{adapter.importBitmap(bitmap);}finally{bitmap.recycle();}
    String asset=adapter.capture(old);frames.add(new AnimationSequence.Frame(asset,asset,1));
    try(RandomAccessFile checkpoint=new RandomAccessFile(new File(old.directory,"legacy-journal"),"rw")){checkpoint.seek(checkpoint.length());checkpoint.writeInt(index);checkpoint.getFD().sync();}
   }
   old.sequence.replace(frames,0,12);old.checkpoint();
  }finally{legacy.release();}
  long legacyMs=SystemClock.elapsedRealtime()-before;
  VideoImportJob job=VideoImportJob.create(context,request);before=SystemClock.elapsedRealtime();
  try{
   VideoImportRunner.run(context,job,adapter,new AtomicBoolean(),n->{});
   long optimizedMs=SystemClock.elapsedRealtime()-before;
   System.out.println("VIDEO_IMPORT_BENCHMARK source=6s_640x360_30fps extract=12fps frames=72 legacy_ms="+legacyMs+" optimized_ms="+optimizedMs);
   android.util.Log.i("VIDEO_IMPORT_BENCHMARK","legacy_ms="+legacyMs+" optimized_ms="+optimizedMs);
   assertEquals("complete",job.status().getProperty("state"));
   assertTrue("Optimización más lenta: "+optimizedMs+" >= "+legacyMs,optimizedMs<legacyMs);
   AnimationProject imported=new AnimationProject(job.projectDirectory,"x",1,1,1);imported.read();assertEquals(72,imported.sequence.size());
   assertFalse(imported.nativeFile(imported.sequence.current().asset).exists());
   adapter.load(imported,imported.sequence.current().asset);assertEquals(640,DrawingView.nativeWidth());assertEquals(360,DrawingView.nativeHeight());
  }finally{VideoImportJob.active(context).delete();source.delete();}
 }
}
