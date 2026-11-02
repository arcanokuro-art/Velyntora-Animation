package app.velyntora.animation.export.png_sequence;
import app.velyntora.animation.export.AnimationFrames;
import java.io.*;import java.util.*;import java.util.zip.*;
public final class PngSequenceExporter {
 public static void write(AnimationFrames frames,OutputStream out)throws IOException{
  try(ZipOutputStream zip=new ZipOutputStream(out)){
   zip.putNextEntry(new ZipEntry("timing.csv"));StringBuilder timing=new StringBuilder("frame,exposure,fps\n");
   for(int i=0;i<frames.count();i++)timing.append(i+1).append(',').append(frames.exposure(i)).append(',').append(frames.fps()).append('\n');
   zip.write(timing.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));zip.closeEntry();
   for(int i=0;i<frames.count();i++){zip.putNextEntry(new ZipEntry(String.format(Locale.ROOT,"frame-%05d.png",i+1)));frames.png(i,zip);zip.closeEntry();}
  }
 }
 private PngSequenceExporter(){}
}
