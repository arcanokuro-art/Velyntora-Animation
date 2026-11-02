package art.velyntora.core;
import static org.junit.Assert.*;
import android.app.*;import android.content.*;import android.net.Uri;import android.os.*;
import androidx.test.platform.app.InstrumentationRegistry;
import app.velyntora.animation.video.*;import app.velyntora.animation.video.background.*;import app.velyntora.animation.storage.AnimationProject;
import java.io.*;import java.util.concurrent.atomic.AtomicBoolean;import java.util.function.BooleanSupplier;import org.junit.Test;
public final class BackgroundVideoImportTest {
 private final Instrumentation i=InstrumentationRegistry.getInstrumentation();
 private File fixture()throws IOException{
  File file=new File(i.getTargetContext().getFilesDir(),"background-test.mp4");
  try(InputStream in=i.getContext().getAssets().open("video-import-test.mp4");OutputStream out=new FileOutputStream(file)){
   byte[] buffer=new byte[4096];int count;while((count=in.read(buffer))!=-1)out.write(buffer,0,count);
  }return file;
 }
 private void await(BooleanSupplier check){
  long until=SystemClock.uptimeMillis()+60000;
  do{if(check.getAsBoolean())return;SystemClock.sleep(20);}while(SystemClock.uptimeMillis()<until);
  throw new AssertionError("Background import timed out");
 }
 private boolean ready(AnimationActivity host){
  try{java.lang.reflect.Field r=AnimationActivity.class.getDeclaredField("ready"),b=AnimationActivity.class.getDeclaredField("busy");
   r.setAccessible(true);b.setAccessible(true);return r.getBoolean(host)&&!b.getBoolean(host);
  }catch(Exception ignored){return false;}
 }
 private boolean state(VideoImportJob job,String state){try{return job.status().getProperty("state").equals(state);}catch(Exception e){return false;}}
 @Test public void durableJournalResumesWithoutRewritingCompletedFrames()throws Exception{
  File file=fixture();Context c=i.getTargetContext();
  VideoImportRequest request=new VideoImportRequest(Uri.fromFile(file),"Reanudación",0,2000000,4,64,48);
  VideoImportJob job=VideoImportJob.create(c,request);
  try{
   DrawingView.nativeHistoryDirectory(c.getCacheDir().getAbsolutePath());DrawingView.nativeImageStorage(c.getCacheDir().getAbsolutePath());
   AtomicBoolean stop=new AtomicBoolean();
   VideoImportRunner.run(c,job,new FrameDocumentAdapter(null),stop,n->{if(n==1)stop.set(true);});
   assertTrue(state(job,"interrupted"));
   assertFalse(new File(job.projectDirectory,"project.properties").exists());
   File first=new File(job.projectDirectory,"objects/"+job.frameId(0)+".vlycore");
   assertTrue(first.length()>0);assertTrue(first.setLastModified(1000L));
   try(FileOutputStream broken=new FileOutputStream(new File(job.directory,"frames.journal"),true)){broken.write(new byte[]{1,2,3});broken.getFD().sync();}
   VideoImportRunner.run(c,VideoImportJob.load(c,job.id),new FrameDocumentAdapter(null),new AtomicBoolean(),n->{});
   assertTrue(state(job,"complete"));assertEquals(1000L,first.lastModified());
   assertEquals(8*44,new File(job.directory,"frames.journal").length());
   AnimationProject result=new AnimationProject(job.projectDirectory,"x",1,1,1);result.read();
   assertEquals(8,result.sequence.size());assertEquals(4,result.sequence.fps());
  }finally{VideoImportJob.active(c).delete();file.delete();}
 }
 @Test public void foregroundServiceFinishesAfterHomeWithoutChangingEditorCanvas()throws Exception{
  Context c=i.getTargetContext();File file=fixture();
  if(Build.VERSION.SDK_INT>=33)i.getUiAutomation().grantRuntimePermission(c.getPackageName(),android.Manifest.permission.POST_NOTIFICATIONS);
  AnimationActivity host=(AnimationActivity)i.startActivitySync(new Intent(c,AnimationActivity.class).setAction(AnimationActivity.ACTION)
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("width",64).putExtra("height",48).putExtra("fps",30));
  VideoImportJob job=null;
  try{
   await(()->{AtomicBoolean ready=new AtomicBoolean();i.runOnMainSync(()->ready.set(ready(host)));return ready.get();});
   i.runOnMainSync(()->{DrawingView.nativeBeginEdit();DrawingView.nativeStroke(10,20,50,20,3,0xff000000);});
   int[] original=DrawingView.nativePixels();
   VideoImportRequest request=new VideoImportRequest(Uri.fromFile(file),"Segundo plano",0,2000000,30,640,480);
   i.runOnMainSync(()->host.importVideo(request));
   await(()->{try{return VideoImportJob.current(c)!=null;}catch(Exception ignored){return false;}});
   job=VideoImportJob.current(c);final VideoImportJob running=job;
   await(()->state(running,"running")||state(running,"complete"));
   i.getUiAutomation().performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME);
   await(()->state(running,"complete")||state(running,"failed"));
   assertTrue(running.status().getProperty("error"),state(running,"complete"));
   assertArrayEquals(original,DrawingView.nativePixels());
   AnimationProject result=new AnimationProject(running.projectDirectory,"x",1,1,1);result.read();
   assertEquals(60,result.sequence.size());
  }finally{
   if(job!=null&&!job.terminal())VideoImportManager.signal(c,job,"cancel");
   VideoImportJob.active(c).delete();
   if(!host.isDestroyed())i.runOnMainSync(host::onBackPressed);
   file.delete();
  }
 }
}
