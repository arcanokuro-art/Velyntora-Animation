package art.velyntora.core;
import art.velyntora.core.document.CanvasLimits;
import java.io.*;
import java.util.zip.*;

/** Original-resolution RGBA export with one row and one compressed chunk in memory. */
final class PngWriter {
  private static void integer(OutputStream out,int n)throws IOException{
    out.write(n>>>24);out.write(n>>>16);out.write(n>>>8);out.write(n);
  }
  private static void chunk(OutputStream out,String name,byte[] data,int n)throws IOException{
    byte[] type=name.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    integer(out,n);out.write(type);out.write(data,0,n);
    CRC32 crc=new CRC32();crc.update(type);crc.update(data,0,n);integer(out,(int)crc.getValue());
  }
  static void write(OutputStream out,int width,int height,RasterFileWriter.RowSource source)throws IOException{
    if(!CanvasLimits.accepts(width,height))throw new IOException("Dimensiones inválidas");
    out.write(new byte[]{(byte)137,80,78,71,13,10,26,10});
    ByteArrayOutputStream header=new ByteArrayOutputStream(13);
    integer(header,width);integer(header,height);header.write(new byte[]{8,6,0,0,0});
    chunk(out,"IHDR",header.toByteArray(),13);
    int[] pixels=new int[width];byte[] row=new byte[width*4+1],compressed=new byte[65536];
    Deflater deflater=new Deflater(Deflater.BEST_SPEED);
    try{
      for(int y=0;y<height;y++){
        source.read(y,pixels);row[0]=1;
        int previous=0;
        for(int x=0;x<width;x++){
          int pixel=pixels[x];int at=x*4+1;
          row[at]=(byte)((pixel>>>16)-(previous>>>16));
          row[at+1]=(byte)((pixel>>>8)-(previous>>>8));
          row[at+2]=(byte)(pixel-previous);row[at+3]=(byte)((pixel>>>24)-(previous>>>24));
          previous=pixel;
        }
        deflater.setInput(row);
        while(!deflater.needsInput()){
          int n=deflater.deflate(compressed);if(n>0)chunk(out,"IDAT",compressed,n);
        }
      }
      deflater.finish();while(!deflater.finished()){
        int n=deflater.deflate(compressed);if(n>0)chunk(out,"IDAT",compressed,n);
      }
    }finally{deflater.end();}
    chunk(out,"IEND",new byte[0],0);
  }
  private PngWriter(){}
}
