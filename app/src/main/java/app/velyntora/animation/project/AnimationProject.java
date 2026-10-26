package app.velyntora.animation.project;
import app.velyntora.animation.frames.FrameSequence;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
/** Each frame retains its native layered .vlycore document and a PNG playback proxy. */
public final class AnimationProject {
 public final FrameSequence sequence=new FrameSequence();
 public final File directory;
 public AnimationProject(File directory)throws IOException{this.directory=directory;if(!directory.mkdirs()&&!directory.isDirectory())throw new IOException("No se pudo crear el proyecto");}
 public File frame(String id){return new File(directory,id+".vlycore");}
 public File preview(String id){return new File(directory,id+".png");}
 public void checkpoint()throws IOException{
  File tmp=new File(directory,"project.tmp");
  try(DataOutputStream out=new DataOutputStream(new FileOutputStream(tmp))){out.writeUTF("VLYANIM1");out.writeInt(sequence.fps());out.writeInt(sequence.selected());out.writeInt(sequence.size());for(String id:sequence.ids())out.writeUTF(id);}
  Files.move(tmp.toPath(),new File(directory,"project.meta").toPath(),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
 }
 public void readMetadata()throws IOException{
  try(DataInputStream in=new DataInputStream(new FileInputStream(new File(directory,"project.meta")))){
   if(!"VLYANIM1".equals(in.readUTF()))throw new IOException("Formato de animación no compatible");
   int fps=in.readInt(),selected=in.readInt(),count=in.readInt();if(count<1||count>10000)throw new IOException("Cantidad inválida");
   List<String> ids=new ArrayList<>();for(int i=0;i<count;i++)ids.add(in.readUTF());
   try{sequence.restore(ids,selected,fps);}catch(IllegalArgumentException e){throw new IOException(e);}
   for(String id:ids)if(!frame(id).isFile()||!preview(id).isFile())throw new IOException("Falta un fotograma");
  }
 }
 public void export(OutputStream output,boolean pngOnly)throws IOException{
  try(ZipOutputStream zip=new ZipOutputStream(output)){
   if(pngOnly){zip.putNextEntry(new ZipEntry("timing.txt"));zip.write(("fps="+sequence.fps()+"\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));zip.closeEntry();}
   else entry(zip,new File(directory,"project.meta"),"project.meta");
   for(int i=0;i<sequence.size();i++){String id=sequence.id(i);if(!pngOnly)entry(zip,frame(id),id+".vlycore");entry(zip,preview(id),pngOnly?String.format(Locale.ROOT,"frame-%05d.png",i+1):id+".png");}
  }
 }
 private static void entry(ZipOutputStream zip,File file,String name)throws IOException{zip.putNextEntry(new ZipEntry(name));Files.copy(file.toPath(),zip);zip.closeEntry();}
 public void importArchive(InputStream input)throws IOException{
  long total=0;int count=0;
  try(ZipInputStream zip=new ZipInputStream(input)){
   ZipEntry entry;byte[] buffer=new byte[65536];while((entry=zip.getNextEntry())!=null){
    String name=entry.getName();if(++count>20001||(!name.equals("project.meta")&&!name.matches("[a-f0-9-]{36}\\.(png|vlycore)")))throw new IOException("Entrada inválida");
    File file=new File(directory,name);if(file.exists())throw new IOException("Archivo duplicado");
    try(OutputStream out=new FileOutputStream(file)){int n;long size=0;while((n=zip.read(buffer))!=-1){total+=n;size+=n;if(total>2147483648L||size>536870912L)throw new IOException("Proyecto demasiado grande");out.write(buffer,0,n);}}
   }
  }
  readMetadata();
 }
}
