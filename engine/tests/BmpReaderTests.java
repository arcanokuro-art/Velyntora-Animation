package art.velyntora.core;
import java.io.*;import java.util.*;
public final class BmpReaderTests {
 static void check(boolean b){if(!b)throw new AssertionError();}
 static void le(byte[] b,int p,int n,int value){for(int i=0;i<n;i++)b[p+i]=(byte)(value>>>(8*i));}
 static byte[] bmp(int w,int h,int depth,int compression,int[] palette,byte[] data,int[] masks){int extra=masks==null?0:masks.length*4,offset=54+extra+palette.length*4;byte[] b=new byte[offset+data.length];le(b,0,2,0x4d42);le(b,2,4,b.length);le(b,10,4,offset);le(b,14,4,40);le(b,18,4,w);le(b,22,4,h);le(b,26,2,1);le(b,28,2,depth);le(b,30,4,compression);le(b,46,4,palette.length);if(masks!=null)for(int i=0;i<masks.length;i++)le(b,54+i*4,4,masks[i]);for(int i=0;i<palette.length;i++)le(b,54+extra+i*4,4,palette[i]);System.arraycopy(data,0,b,offset,data.length);return b;}
 static BmpReader.Image read(byte[] b)throws Exception{return BmpReader.read(new ByteArrayInputStream(b));}
 static void reject(byte[] b)throws Exception{try{read(b);throw new AssertionError();}catch(IOException expected){}}
 public static void main(String[] args)throws Exception{
  int[] p={0xff000000,0xffffffff,0xffff0000,0xff00ff00};
  check(Arrays.equals(read(bmp(3,1,4,0,p,new byte[]{0x12,0x30,0,0},null)).pixels,new int[]{p[1],p[2],p[3]}));
  check(Arrays.equals(read(bmp(2,-1,24,0,new int[0],new byte[]{0,0,(byte)255,0,(byte)255,0,0,0},null)).pixels,new int[]{0xffff0000,0xff00ff00}));
  check(read(bmp(1,1,16,3,new int[0],new byte[]{(byte)0xe0,7,0,0},new int[]{0xf800,0x7e0,0x1f})).pixels[0]==0xff00ff00);
  check(read(bmp(1,1,32,0,new int[0],new byte[]{1,2,3,0},null)).pixels[0]==0xff030201);
  check(read(bmp(1,1,32,6,new int[0],new byte[]{1,2,3,0},new int[]{0xff0000,0xff00,0xff,0xff000000})).pixels[0]==0x00030201);
  check(Arrays.equals(read(bmp(3,1,8,1,p,new byte[]{0,3,1,2,3,0,0,1},null)).pixels,new int[]{p[1],p[2],p[3]}));
  check(Arrays.equals(read(bmp(3,1,4,2,p,new byte[]{3,0x12,0,1},null)).pixels,new int[]{p[1],p[2],p[1]}));
  check(Arrays.equals(read(bmp(4,2,8,1,p,new byte[]{1,1,0,2,1,1,2,2,0,1},null)).pixels,new int[]{p[0],p[0],p[2],p[2],p[1],p[0],p[0],p[0]}));
  byte[] good=bmp(3,1,4,0,p,new byte[]{0x12,0x30,0,0},null);reject(Arrays.copyOf(good,good.length-1));byte[] bad=good.clone();le(bad,10,4,1);reject(bad);reject(bmp(2,1,8,1,p,new byte[]{3,1,0,1},null));reject(bmp(1,1,16,3,new int[0],new byte[4],new int[]{0xf800,0xf800,0x1f}));
  ByteArrayOutputStream encoded=new ByteArrayOutputStream();RasterFileWriter.write(encoded,3,2,false,(y,row)->Arrays.fill(row,y==0?0xffff0000:0xff00ff00));BmpReader.Image result=read(encoded.toByteArray());check(result.pixels[0]==0xffff0000&&result.pixels[3]==0xff00ff00);
  try(FileOutputStream out=new FileOutputStream("/tmp/velyntora-bmp-independent.bmp")){out.write(encoded.toByteArray());}
  System.out.println("BMP indexed, RGB, top-down, bitfields, alpha, RLE, bounds and roundtrip passed");
 }
}
