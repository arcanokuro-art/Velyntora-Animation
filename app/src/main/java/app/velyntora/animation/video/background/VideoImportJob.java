package app.velyntora.animation.video.background;
import android.content.Context;import android.net.Uri;
import app.velyntora.animation.video.VideoImportRequest;import app.velyntora.animation.model.AnimationSequence;
import java.io.*;import java.nio.file.*;import java.util.*;
/** Disk-owned state shared by the UI and the isolated import process. No SharedPreferences caches. */
public final class VideoImportJob {
 public final File directory,projectDirectory;public final String id;public final VideoImportRequest request;
 private VideoImportJob(Context context,String id,VideoImportRequest request){
  this.id=id;this.request=request;directory=new File(root(context),id);
  projectDirectory=new File(new File(context.getFilesDir(),"animations"),id);
 }
 private static File root(Context c){return new File(c.getFilesDir(),"video-imports");}
 public static File active(Context c){return new File(root(c),"active.properties");}
 public static Properties read(File file)throws IOException{
  Properties p=new Properties();try(InputStream in=new FileInputStream(file)){p.load(in);}return p;
 }
 public static void write(File file,Properties p)throws IOException{
  File parent=file.getParentFile();if(!parent.mkdirs()&&!parent.isDirectory())throw new IOException("No se pudo crear la carpeta del trabajo");
  File temporary=new File(parent,file.getName()+".next");
  try(FileOutputStream out=new FileOutputStream(temporary)){p.store(out,"Velyntora video import");out.getFD().sync();}
  Files.move(temporary.toPath(),file.toPath(),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
 }
 public static VideoImportJob create(Context c,VideoImportRequest request)throws IOException{
  VideoImportJob existing=current(c);
  if(existing!=null&&!existing.terminal())throw new IOException("Ya hay una importación pendiente. Reanúdala o cancélala primero.");
  String id=AnimationSequence.uuid();VideoImportJob job=new VideoImportJob(c,id,request);
  Properties p=new Properties();p.setProperty("uri",request.uri.toString());p.setProperty("title",request.title);
  p.setProperty("start",""+request.startUs);p.setProperty("end",""+request.endUs);p.setProperty("fps",""+request.fps);
  p.setProperty("width",""+request.width);p.setProperty("height",""+request.height);
  write(new File(job.directory,"request.properties"),p);job.status("queued",0,"");
  Properties marker=new Properties();marker.setProperty("id",id);write(active(c),marker);return job;
 }
 public static VideoImportJob load(Context c,String id)throws IOException{
  if(!AnimationSequence.validId(id))throw new IOException("Identificador de importación inválido");
  File directory=new File(root(c),id);Properties p=read(new File(directory,"request.properties"));
  try{return new VideoImportJob(c,id,new VideoImportRequest(Uri.parse(p.getProperty("uri")),p.getProperty("title","Video importado"),
   Long.parseLong(p.getProperty("start")),Long.parseLong(p.getProperty("end")),Integer.parseInt(p.getProperty("fps")),
   Integer.parseInt(p.getProperty("width")),Integer.parseInt(p.getProperty("height"))));}
  catch(RuntimeException e){throw new IOException("Trabajo de importación inválido",e);}
 }
 public static VideoImportJob current(Context c)throws IOException{
  if(!active(c).isFile())return null;return load(c,read(active(c)).getProperty("id"));
 }
 public Properties status()throws IOException{return read(new File(directory,"status.properties"));}
 public boolean terminal()throws IOException{String s=status().getProperty("state");return s.equals("complete")||s.equals("cancelled");}
 public void status(String state,int completed,String error)throws IOException{
  Properties p=new Properties();p.setProperty("state",state);p.setProperty("completed",""+completed);
  p.setProperty("total",""+request.frameCount);p.setProperty("error",error==null?"":error);write(new File(directory,"status.properties"),p);
 }
 public void signal(String action)throws IOException{
  if(!action.equals("pause")&&!action.equals("cancel"))throw new IOException("Acción inválida");
  try(FileOutputStream out=new FileOutputStream(new File(directory,action))){out.write(1);out.getFD().sync();}
 }
 public String frameId(int index){return java.util.UUID.nameUUIDFromBytes((id+"/"+index).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();}
 public void check(java.util.concurrent.atomic.AtomicBoolean stopping)throws IOException{
  if(new File(directory,"cancel").isFile())throw new java.util.concurrent.CancellationException();
  if(stopping.get()||new File(directory,"pause").isFile())throw new InterruptedIOException("Importación pausada");
 }
 public void deletePartialProject(){
  File[] files=new File(projectDirectory,"objects").listFiles();if(files!=null)for(File file:files)file.delete();
  new File(projectDirectory,"objects").delete();new File(projectDirectory,"project.next").delete();
  new File(projectDirectory,"project.properties").delete();projectDirectory.delete();
 }
}
