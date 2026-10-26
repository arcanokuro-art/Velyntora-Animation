package art.velyntora.core;
import java.io.*;import java.util.*;
public final class PcxReaderTests {
 static byte[] file(int bits,int planes,int... data)throws Exception{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] h=new byte[128];h[0]=10;h[1]=5;h[2]=1;h[3]=(byte)bits;h[8]=1;h[65]=(byte)planes;h[66]=2;h[19]=(byte)255;out.write(h);for(int n:data)out.write(n);return out.toByteArray();}
 static void check(boolean v){if(!v)throw new AssertionError();}
 static void reject(byte[] b)throws Exception{try{PcxReader.read(new ByteArrayInputStream(b));throw new AssertionError();}catch(IOException expected){}}
 public static void main(String[] args)throws Exception{
  check(Arrays.equals(PcxReader.read(new ByteArrayInputStream(file(8,3,0xc1,255,0,0,0xc1,255,0,0))).pixels,new int[]{0xffff0000,0xff00ff00}));
  check(Arrays.equals(PcxReader.read(new ByteArrayInputStream(file(1,1,0x40,0))).pixels,new int[]{0xff000000,0xffff0000}));
  check(Arrays.equals(PcxReader.read(new ByteArrayInputStream(file(1,4,0x80,0,0,0,0,0,0,0))).pixels,new int[]{0xffff0000,0xff000000}));
  ByteArrayOutputStream out=new ByteArrayOutputStream();out.write(file(8,1,1,0));out.write(12);byte[] palette=new byte[768];palette[3]=(byte)255;out.write(palette);check(Arrays.equals(PcxReader.read(new ByteArrayInputStream(out.toByteArray())).pixels,new int[]{0xffff0000,0xff000000}));
  reject(file(8,1,1,0));reject(file(8,3,0xc0,0));reject(file(8,3,0xc7,0));reject(file(1,1,0));byte[] huge=file(8,3,0);huge[9]=127;reject(huge);
  System.out.println("PCX RGB, indexed, planar, RLE and malformed inputs passed");
 }
}
