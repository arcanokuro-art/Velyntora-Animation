package app.velyntora.animation.video.background;
import app.velyntora.animation.model.AnimationSequence;import app.velyntora.animation.storage.AnimationProject;
import java.io.*;import java.nio.*;import java.nio.charset.StandardCharsets;import java.util.*;import java.util.zip.CRC32;
/** One checksummed, fsynced record only after both frame files have been fsynced. */
public final class VideoImportJournal implements AutoCloseable{
 private final RandomAccessFile file;
 public VideoImportJournal(File folder)throws IOException{file=new RandomAccessFile(new File(folder,"frames.journal"),"rw");}
 public ArrayList<AnimationSequence.Frame> recover(VideoImportJob job,AnimationProject project)throws IOException{
  ArrayList<AnimationSequence.Frame> frames=new ArrayList<>();long valid=0;file.seek(0);byte[] bytes=new byte[44];
  while(file.length()-file.getFilePointer()>=44&&frames.size()<job.request.frameCount){
   file.readFully(bytes);CRC32 crc=new CRC32();crc.update(bytes,0,40);ByteBuffer record=ByteBuffer.wrap(bytes);
   int index=record.getInt();String asset=new String(bytes,4,36,StandardCharsets.US_ASCII);
   if(index!=frames.size()||record.getInt(40)!=(int)crc.getValue()||!asset.equals(job.frameId(index))
      ||project.nativeFile(asset).length()==0||project.previewFile(asset).length()==0)break;
   frames.add(new AnimationSequence.Frame(asset,asset,1));valid=file.getFilePointer();
  }
  file.setLength(valid);file.seek(valid);return frames;
 }
 public void append(int index,String asset)throws IOException{
  byte[] bytes=new byte[44];ByteBuffer record=ByteBuffer.wrap(bytes);record.putInt(index);record.put(asset.getBytes(StandardCharsets.US_ASCII));
  CRC32 crc=new CRC32();crc.update(bytes,0,40);record.putInt((int)crc.getValue());file.write(bytes);file.getFD().sync();
 }
 public void close()throws IOException{file.close();}
}
