package art.velyntora.core;
import java.io.*;
import java.util.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
public final class GifWriterTests {
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 static byte[] encode(int width,int height,int[] pixels)throws Exception{ByteArrayOutputStream out=new ByteArrayOutputStream();GifWriter.write(out,width,height,(y,row)->System.arraycopy(pixels,y*width,row,0,width));return out.toByteArray();}
 static void roundtrip(int width,int height,int[] pixels,boolean quantized)throws Exception{
  byte[] bytes=encode(width,height,pixels);BufferedImage image=ImageIO.read(new ByteArrayInputStream(bytes));check(image!=null&&image.getWidth()==width&&image.getHeight()==height,"dimensions");
  for(int i=0;i<pixels.length;i++){int p=pixels[i],expected;if((p>>>24)<128)expected=0;else if(quantized)expected=0xff000000|(((p>>>16&255)+25)/51*51)<<16|(((p>>>8&255)+25)/51*51)<<8|((p&255)+25)/51*51;else expected=p|0xff000000;check(image.getRGB(i%width,i/width)==expected,"pixel "+i);}
 }
 public static void main(String[] args)throws Exception{
  roundtrip(3,2,new int[]{0,0x7fff0000,0x80ff0000,0xff00ff00,0xff0000ff,0xffffffff},false);
  Random random=new Random(413);for(int size:new int[]{1,2,17,253,254,255,256,257,4096,20000,100000}){int[] p=new int[size];for(int i=0;i<size;i++)p[i]=0xff000000|random.nextInt(255)*0x010101;roundtrip(size>8192?100:size,size>8192?size/100:1,p,false);}
  int[] many=new int[129*97];for(int i=0;i<many.length;i++)many[i]=random.nextInt();roundtrip(129,97,many,true);
  int[] solid=new int[4000000];Arrays.fill(solid,0xffff00ff);byte[] compressed=encode(2000,2000,solid);check(compressed.length<10000,"LZW compression");BufferedImage decoded=ImageIO.read(new ByteArrayInputStream(compressed));check(decoded.getRGB(1999,1999)==0xffff00ff,"large final pixel");
  try{encode(0,1,new int[0]);throw new AssertionError("invalid size");}catch(IllegalArgumentException expected){}
  System.out.println("GIF independent decoder: exact palettes, quantization, transparency, LZW width/reset boundaries and 4M pixels passed");
 }
}
