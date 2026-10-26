package art.velyntora.core;
import java.io.*;import java.util.*;
public final class IconContainersTests {
 static void check(boolean b){if(!b)throw new AssertionError();}
 static void integer(ByteArrayOutputStream out,int n,boolean le){for(int i=0;i<4;i++)out.write(n>>>(le?8*i:8*(3-i)));}
 static byte[] block(String tag,byte[] b,boolean le)throws Exception{ByteArrayOutputStream out=new ByteArrayOutputStream();if(!le&&tag.equals("idat"))integer(out,b.length+8,false);out.write(tag.getBytes("US-ASCII"));if(le)integer(out,b.length,true);else if(!tag.equals("idat"))integer(out,b.length+8,false);out.write(b);if(le&&(b.length&1)!=0)out.write(0);return out.toByteArray();}
 static IconContainers.Image decode(byte[] b)throws IOException{if(!Arrays.equals(b,new byte[]{1,2,3}))throw new IOException();return new IconContainers.Image(1,1,new int[]{0xff123456});}
 static IconContainers.Image read(byte[] b)throws Exception{return IconContainers.read(new ByteArrayInputStream(b),IconContainersTests::decode);}
 static void reject(byte[] b)throws Exception{try{read(b);throw new AssertionError();}catch(IOException expected){}}
 public static void main(String[] args)throws Exception{
  check(read(block("idat",new byte[]{1,2,3},false)).pixels[0]==0xff123456);
  ByteArrayOutputStream body=new ByteArrayOutputStream();body.write("fram".getBytes("US-ASCII"));body.write(block("icon",new byte[]{1,2,3},true));ByteArrayOutputStream ani=new ByteArrayOutputStream();ani.write("ACON".getBytes("US-ASCII"));ani.write(block("LIST",body.toByteArray(),true));byte[] riff=block("RIFF",ani.toByteArray(),true);check(read(riff).pixels[0]==0xff123456);
  check(read(block("icns",block("ic08",new byte[]{1,2,3},false),false)).width==1);
  byte[] rgb=new byte[256*4],alpha=new byte[256];for(int i=0;i<256;i++){rgb[i*4+1]=(byte)255;alpha[i]=(byte)128;}body.reset();body.write(block("is32",rgb,false));body.write(block("s8mk",alpha,false));IconContainers.Image image=read(block("icns",body.toByteArray(),false));check(image.width==16&&image.pixels[0]==0x80ff0000);
  body.reset();ByteArrayOutputStream rle=new ByteArrayOutputStream();for(int color:new int[]{255,0,0}){rle.write(255);rle.write(color);rle.write(251);rle.write(color);}body.write(block("is32",rle.toByteArray(),false));body.write(block("s8mk",alpha,false));check(read(block("icns",body.toByteArray(),false)).pixels[255]==0x80ff0000);
  reject(Arrays.copyOf(riff,riff.length-1));reject(block("idat",new byte[]{0},false));reject(block("icns",new byte[]{0},false));
  System.out.println("ANI first frame, QuickTime atoms and ICNS PNG/ARGB/RLE, masks and limits passed");
 }
}
