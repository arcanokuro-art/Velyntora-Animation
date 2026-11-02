package app.velyntora.animation.export.apng;
import android.graphics.Bitmap;import app.velyntora.animation.export.*;import java.io.*;import java.util.zip.*;
/** Lossless full-frame APNG with streaming zlib chunks and exact rational frame delays. */
public final class ApngExporter{
 private static void chunk(DataOutputStream out,String type,byte[] data)throws IOException{
  byte[] tag=type.getBytes("US-ASCII");out.writeInt(data.length);out.write(tag);out.write(data);
  CRC32 crc=new CRC32();crc.update(tag);crc.update(data);out.writeInt((int)crc.getValue());
 }
 private static byte[] values(int... values)throws IOException{
  ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream data=new DataOutputStream(bytes);
  for(int value:values)data.writeInt(value);return bytes.toByteArray();
 }
 public static void write(AnimationFrames frames,ExportRequest request,OutputStream target)throws IOException{
  DataOutputStream out=new DataOutputStream(target);out.write(new byte[]{(byte)137,80,78,71,13,10,26,10});
  ByteArrayOutputStream header=new ByteArrayOutputStream();DataOutputStream hd=new DataOutputStream(header);
  hd.writeInt(request.width);hd.writeInt(request.height);hd.write(new byte[]{8,6,0,0,0});
  chunk(out,"IHDR",header.toByteArray());chunk(out,"acTL",values(frames.count(),0));final int[] sequence={0};
  for(int index=0;index<frames.count();index++){
   ByteArrayOutputStream control=new ByteArrayOutputStream();DataOutputStream data=new DataOutputStream(control);
   data.writeInt(sequence[0]++);data.writeInt(request.width);data.writeInt(request.height);data.writeInt(0);data.writeInt(0);
   data.writeShort(frames.exposure(index));data.writeShort(frames.fps());data.writeByte(0);data.writeByte(0);
   chunk(out,"fcTL",control.toByteArray());final boolean first=index==0;
   OutputStream chunks=new OutputStream(){
    byte[] bytes=new byte[32768];int used;
    public void write(int value)throws IOException{bytes[used++]=(byte)value;if(used==bytes.length)flush();}
    public void write(byte[] b,int offset,int count)throws IOException{
     while(count>0){int n=Math.min(count,bytes.length-used);System.arraycopy(b,offset,bytes,used,n);used+=n;offset+=n;count-=n;if(used==bytes.length)flush();}
    }
    public void flush()throws IOException{
     if(used==0)return;
     byte[] payload;
     if(first)payload=java.util.Arrays.copyOf(bytes,used);
     else{ByteArrayOutputStream buffer=new ByteArrayOutputStream();DataOutputStream d=new DataOutputStream(buffer);d.writeInt(sequence[0]++);d.write(bytes,0,used);payload=buffer.toByteArray();}
     chunk(out,first?"IDAT":"fdAT",payload);used=0;
    }
    public void close()throws IOException{flush();}
   };
   Bitmap image=frames.render(index,request.width,request.height,false);
   try(DeflaterOutputStream compressed=new DeflaterOutputStream(chunks)){
    int[] row=new int[request.width];byte[] rgba=new byte[request.width*4+1];
    for(int y=0;y<request.height;y++){frames.check();image.getPixels(row,0,request.width,0,y,request.width,1);
     for(int x=0;x<row.length;x++){int c=row[x],at=1+x*4;rgba[at]=(byte)(c>>16);rgba[at+1]=(byte)(c>>8);rgba[at+2]=(byte)c;rgba[at+3]=(byte)(c>>>24);}
     compressed.write(rgba);
    }
   }finally{image.recycle();}
  }
  chunk(out,"IEND",new byte[0]);out.flush();
 }
 private ApngExporter(){}
}
