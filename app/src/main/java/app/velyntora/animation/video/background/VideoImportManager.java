package app.velyntora.animation.video.background;
import android.app.*;import android.content.*;import android.net.Uri;import android.os.Build;import android.widget.Toast;
import app.velyntora.animation.video.VideoImportRequest;import java.io.*;
public final class VideoImportManager {
 public static void start(Activity host,VideoImportRequest request){
  try{
   if("content".equals(request.uri.getScheme())){
    try{host.getContentResolver().takePersistableUriPermission(request.uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}
    catch(SecurityException denied){throw new IOException("Elige el video desde Archivos para conservar el acceso y poder reanudarlo después de reiniciar.");}
   }
   if(Build.VERSION.SDK_INT>=33&&host.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)
    host.requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},9101);
   VideoImportJob job=VideoImportJob.create(host,request);resume(host,job,false);show(host,job);
  }catch(Exception e){new AlertDialog.Builder(host).setMessage(e.getMessage()).setPositiveButton("Aceptar",null).show();}
 }
 public static void resume(Context host,VideoImportJob job,boolean explicit){
  try{
   if(explicit)new File(job.directory,"pause").delete();
   Intent service=new Intent(host,art.velyntora.core.VideoImportService.class).setAction("start").putExtra("job",job.id);
   host.startForegroundService(service);
  }catch(RuntimeException e){Toast.makeText(host,"No se pudo iniciar el trabajo. Abre la app y pulsa Reanudar.",Toast.LENGTH_LONG).show();}
 }
 public static void signal(Context host,VideoImportJob job,String action)throws IOException{
  job.signal(action);
  host.startForegroundService(new Intent(host,art.velyntora.core.VideoImportService.class).setAction(action).putExtra("job",job.id));
 }
 public static void show(Context host,VideoImportJob job){
  host.startActivity(new Intent(host,art.velyntora.core.VideoImportStatusActivity.class).putExtra("job",job.id));
 }
 public static void showCurrent(Activity host){
  try{VideoImportJob job=VideoImportJob.current(host);
   if(job==null)Toast.makeText(host,"No hay importaciones de video",Toast.LENGTH_SHORT).show();else show(host,job);
  }catch(IOException error){Toast.makeText(host,"No se pudo leer la importación",Toast.LENGTH_LONG).show();}
 }
 public static void restore(Activity host){
  try{
   VideoImportJob job=VideoImportJob.current(host);if(job==null||job.terminal())return;
   String state=job.status().getProperty("state");
   if(state.equals("queued")||state.equals("running")||state.equals("interrupted"))resume(host,job,false);
   show(host,job);
  }catch(IOException ignored){}
 }
 private VideoImportManager(){}
}
