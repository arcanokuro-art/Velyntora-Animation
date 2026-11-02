package art.velyntora.core;
import android.content.Context;import android.graphics.Bitmap;
import app.velyntora.animation.video.*;import app.velyntora.animation.video.background.*;
import app.velyntora.animation.storage.AnimationProject;import app.velyntora.animation.model.AnimationSequence;
import java.io.*;import java.util.*;import java.util.concurrent.atomic.AtomicBoolean;import java.util.function.IntConsumer;
/** Runs only in the video-import service process, never against the editor's native canvas. */
public final class VideoImportRunner {
 public static void run(Context context,VideoImportJob job,FrameDocumentAdapter adapter,AtomicBoolean stopping,IntConsumer notify)throws IOException{
  AnimationProject project=new AnimationProject(job.projectDirectory,job.request.title,job.request.width,job.request.height,job.request.fps);
  int completed=0,durable=0;long lastStatus=android.os.SystemClock.elapsedRealtime();
  ArrayList<File> pending=new ArrayList<>();
  try(VideoImportJournal journal=new VideoImportJournal(job.directory);
      VideoFrameDecoder decoder=new VideoFrameDecoder(context,job.request)){
   ArrayList<AnimationSequence.Frame> frames=journal.recover(job,project);completed=frames.size();durable=completed;
   job.status("running",completed,"");notify.accept(completed);
   try{
   for(int index=completed;index<job.request.frameCount;index++){
    job.check(stopping);String asset=job.frameId(index);Bitmap image=decoder.frame(index);
    try{job.check(stopping);pending.addAll(LosslessFrameStore.write(project,asset,image));}finally{image.recycle();}
    journal.append(index,asset);frames.add(new AnimationSequence.Frame(asset,asset,1));completed=index+1;
    long now=android.os.SystemClock.elapsedRealtime();
    if(pending.size()>=48||now-lastStatus>=1000||completed==job.request.frameCount){LosslessFrameStore.sync(pending);journal.commit();pending.clear();durable=completed;job.status("running",completed,"");notify.accept(completed);lastStatus=now;}
   }
   }catch(InterruptedIOException pause){
     LosslessFrameStore.sync(pending);journal.commit();pending.clear();durable=completed;throw pause;
   }
   job.check(stopping);project.sequence.replace(frames,0,job.request.fps);project.checkpoint();
   job.status("complete",completed,"");notify.accept(completed);
  }catch(java.util.concurrent.CancellationException cancelled){
   job.deletePartialProject();job.status("cancelled",completed,"");notify.accept(completed);
  }catch(InterruptedIOException paused){
   job.status(new File(job.directory,"pause").isFile()?"paused":"interrupted",durable,"");notify.accept(completed);
  }catch(IOException|RuntimeException|OutOfMemoryError error){
   job.status("failed",durable,error.getMessage()==null?"No se pudo completar la importación":error.getMessage());
   notify.accept(completed);if(error instanceof IOException)throw (IOException)error;
   throw new IOException("Importación interrumpida; el avance se conserva",error);
  }
 }
 private VideoImportRunner(){}
}
