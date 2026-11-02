package app.velyntora.animation.video;
import android.graphics.Bitmap;import app.velyntora.animation.storage.AnimationProject;
import java.io.*;import java.nio.file.*;import java.util.*;
/** Full-size lossless image plus a small UI-only preview. Native documents are created lazily. */
public final class LosslessFrameStore {
 public static java.util.List<File> write(AnimationProject project,String asset,Bitmap image)throws IOException{
  File full=project.rasterFile(asset),preview=project.previewFile(asset);
  File temporary=new File(full.getPath()+".part");
  try{
   try(android.os.ParcelFileDescriptor out=android.os.ParcelFileDescriptor.open(temporary,
     android.os.ParcelFileDescriptor.MODE_CREATE|android.os.ParcelFileDescriptor.MODE_TRUNCATE|android.os.ParcelFileDescriptor.MODE_WRITE_ONLY)){
      YuvFrameConverter.writePng(image,out.getFd());
   }
   Files.move(temporary.toPath(),full.toPath(),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
  }finally{temporary.delete();}
  float scale=Math.min(1,256f/Math.max(image.getWidth(),image.getHeight()));
  Bitmap thumbnail=scale==1?image:Bitmap.createScaledBitmap(image,Math.max(1,Math.round(image.getWidth()*scale)),Math.max(1,Math.round(image.getHeight()*scale)),true);
  try(FileOutputStream out=new FileOutputStream(preview)){
   if(!thumbnail.compress(Bitmap.CompressFormat.PNG,100,out))throw new IOException("No se pudo guardar la miniatura");
  }finally{if(thumbnail!=image)thumbnail.recycle();}
  return Arrays.asList(full,preview);
 }
 public static void sync(java.util.List<File> files)throws IOException{
  for(File file:files)try(RandomAccessFile out=new RandomAccessFile(file,"rw")){out.getFD().sync();}
 }
 private LosslessFrameStore(){}
}
