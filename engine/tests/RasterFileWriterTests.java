package art.velyntora.core;
import java.io.*;
import java.util.Arrays;
public final class RasterFileWriterTests {
 static void check(boolean value){if(!value)throw new AssertionError();}
 static int read(byte[] data,int offset,int size){int value=0;for(int i=0;i<size;i++)value|=(data[offset+i]&255)<<(i*8);return value;}
 public static void main(String[] args)throws Exception{
  int[][] rows={{0x80ff0000,0xff00ff00,0},{0xff0000ff,0xffffffff,0xff102030}};
  ByteArrayOutputStream out=new ByteArrayOutputStream();RasterFileWriter.write(out,3,2,false,(y,p)->System.arraycopy(rows[y],0,p,0,3));byte[] bmp=out.toByteArray();
  check(bmp.length==78&&bmp[0]=='B'&&bmp[1]=='M');check(read(bmp,2,4)==78&&read(bmp,10,4)==54&&read(bmp,18,4)==3&&read(bmp,22,4)==2);check(read(bmp,28,2)==24);
  check(Arrays.equals(Arrays.copyOfRange(bmp,54,57),new byte[]{-1,0,0}));check(Arrays.equals(Arrays.copyOfRange(bmp,66,69),new byte[]{127,127,-1}));check(read(bmp,75,3)==0); // row padding
  out.reset();RasterFileWriter.write(out,3,2,true,(y,p)->System.arraycopy(rows[y],0,p,0,3));byte[] tga=out.toByteArray();check(tga.length==42&&tga[2]==2&&tga[16]==32&&tga[17]==0x28);check(read(tga,12,2)==3&&read(tga,14,2)==2);check(read(tga,18,4)==0x80ff0000&&read(tga,30,4)==0xff0000ff);
  boolean rejected=false;try{RasterFileWriter.write(out,0,2,true,(y,p)->{});}catch(IllegalArgumentException e){rejected=true;}check(rejected);
  System.out.println("BMP/TGA dimensions, row order, alpha, compositing and padding passed");
 }
}
