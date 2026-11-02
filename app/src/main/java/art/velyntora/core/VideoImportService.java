package art.velyntora.core;
import android.app.*;import android.content.*;import android.content.pm.ServiceInfo;import android.os.*;
import app.velyntora.animation.video.background.*;import java.io.*;import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.AtomicBoolean;
/** A dedicated process keeps the import's native canvas independent of the open editor. */
public final class VideoImportService extends Service {
 private static final Object NATIVE_IMPORT_LOCK=new Object();
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private final AtomicBoolean stopping=new AtomicBoolean();private volatile boolean running;
 private VideoImportJob job;private PowerManager.WakeLock wake;
 private static final String CHANNEL="video-import";private static final int NOTICE=6101;
 public void onCreate(){
  super.onCreate();NotificationManager nm=getSystemService(NotificationManager.class);
  nm.createNotificationChannel(new NotificationChannel(CHANNEL,"Importación de video",NotificationManager.IMPORTANCE_LOW));
 }
 public IBinder onBind(Intent intent){return null;}
 private Notification notice(){
  String state="running",error="";int completed=0;
  try{Properties p=job.status();state=p.getProperty("state");completed=Integer.parseInt(p.getProperty("completed","0"));error=p.getProperty("error","");}catch(Exception ignored){}
  Intent open=new Intent(this,VideoImportStatusActivity.class).putExtra("job",job.id);
  PendingIntent content=PendingIntent.getActivity(this,0,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  boolean active=state.equals("running")||state.equals("queued");
  String text=state.equals("complete")?"Lista para abrir":state.equals("cancelled")?"Cancelada":state.equals("failed")?error:state.equals("paused")||state.equals("interrupted")?"En pausa; puedes reanudar":completed+" / "+job.request.frameCount+" fotogramas";
  Notification.Builder b=new Notification.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.stat_sys_download)
   .setContentTitle("Velyntora · Video a fotogramas").setContentText(text).setContentIntent(content)
   .setOnlyAlertOnce(true).setOngoing(active).setAutoCancel(!active).setCategory(Notification.CATEGORY_PROGRESS);
  if(active){
   b.setProgress(job.request.frameCount,completed,false);
   Intent pause=new Intent(this,VideoImportService.class).setAction("pause").putExtra("job",job.id);
   b.addAction(new Notification.Action.Builder(null,"Pausar",PendingIntent.getService(this,1,pause,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE)).build());
  }
  return b.build();
 }
 private void update(){getSystemService(NotificationManager.class).notify(NOTICE,notice());}
 public int onStartCommand(Intent intent,int flags,int startId){
  try{
   String id=intent==null?null:intent.getStringExtra("job");
   VideoImportJob requested=id==null?VideoImportJob.current(this):VideoImportJob.load(this,id);
   if(requested==null){stopSelf();return START_NOT_STICKY;}
   if(running){
    if(requested.id.equals(job.id)&&intent!=null&&("pause".equals(intent.getAction())||"cancel".equals(intent.getAction())))job.signal(intent.getAction());
    return START_STICKY;
   }
   job=requested;
   if(Build.VERSION.SDK_INT>=35)startForeground(NOTICE,notice(),ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING);
   else if(Build.VERSION.SDK_INT>=29)startForeground(NOTICE,notice(),ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
   else startForeground(NOTICE,notice());
   String action=intent==null?"start":intent.getAction();
   if("pause".equals(action)||"cancel".equals(action))job.signal(action);
   if(job.terminal()){update();stopForeground(STOP_FOREGROUND_DETACH);stopSelf();return START_NOT_STICKY;}
   running=true;stopping.set(false);final VideoImportJob current=job;
   worker.execute(()->{
    try{
     wake=((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"Velyntora:VideoImport");
     wake.acquire(6*60*60*1000L);
     if(new File(current.directory,"cancel").isFile()){current.deletePartialProject();current.status("cancelled",0,"");}
     else if(new File(current.directory,"pause").isFile()){current.status("paused",Integer.parseInt(current.status().getProperty("completed","0")),"");}
     else{
      synchronized(NATIVE_IMPORT_LOCK){
      DrawingView.nativeHistoryDirectory(getCacheDir().getAbsolutePath());
      DrawingView.nativeImageStorage(getCacheDir().getAbsolutePath());
      VideoImportRunner.run(this,current,new FrameDocumentAdapter(null),stopping,n->update());
      }
     }
    }catch(Exception|OutOfMemoryError error){
     try{current.status("failed",Integer.parseInt(current.status().getProperty("completed","0")),error.getMessage());}catch(Exception ignored){}
    }finally{
     if(wake!=null&&wake.isHeld())wake.release();
     new Handler(Looper.getMainLooper()).post(()->{running=false;update();stopForeground(STOP_FOREGROUND_DETACH);stopSelf(startId);});
    }
   });
   return START_STICKY;
  }catch(Exception e){stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();return START_NOT_STICKY;}
 }
 @Override public void onTimeout(int startId,int type){stopping.set(true);stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();}
 public void onDestroy(){stopping.set(true);worker.shutdown();super.onDestroy();}
}
