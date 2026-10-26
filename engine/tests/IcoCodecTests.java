package art.velyntora.core;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
public final class IcoCodecTests {
 static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
 static byte[] write(int w,int h,int[] p)throws Exception{ByteArrayOutputStream out=new ByteArrayOutputStream();IcoCodec.write(out,w,h,(y,row)->System.arraycopy(p,y*w,row,0,w));return out.toByteArray();}
 static IcoCodec.Image read(byte[] b)throws Exception{return IcoCodec.read(new ByteArrayInputStream(b),png->{BufferedImage image=ImageIO.read(new ByteArrayInputStream(png));return new IcoCodec.Image(image.getWidth(),image.getHeight(),image.getRGB(0,0,image.getWidth(),image.getHeight(),null,0,image.getWidth()));});}
 static void put(byte[] b,int p,int value,int n){for(int i=0;i<n;i++)b[p+i]=(byte)(value>>>(8*i));}
 static byte[] combine(byte[] a,byte[] b){byte[] result=new byte[38+a.length-22+b.length-22];put(result,2,1,2);put(result,4,2,2);System.arraycopy(a,6,result,6,16);System.arraycopy(b,6,result,22,16);put(result,18,38,4);put(result,34,38+a.length-22,4);System.arraycopy(a,22,result,38,a.length-22);System.arraycopy(b,22,result,38+a.length-22,b.length-22);return result;}
 static void rejected(byte[] b)throws Exception{try{read(b);throw new AssertionError("malformed accepted");}catch(IOException expected){}}
 static byte[] indexed(int bits)throws Exception{
  int colors=1<<bits,stride=4,size=40+colors*4+stride+4;byte[] b=new byte[22+size];put(b,2,1,2);put(b,4,1,2);b[6]=2;b[7]=1;put(b,10,1,2);put(b,12,bits,2);put(b,14,size,4);put(b,18,22,4);put(b,22,40,4);put(b,26,2,4);put(b,30,2,4);put(b,34,1,2);put(b,36,bits,2);put(b,54,colors,4);put(b,62+4,0x00ff0000,4);int p=62+colors*4;b[p]=(byte)(bits==8?1:1<<(8-bits));b[p+stride]=(byte)0x40;return b;
 }
 public static void main(String[] args)throws Exception{
  Random random=new Random(18);for(int[] size:new int[][]{{1,1},{2,3},{17,13},{256,256}}){int[] p=new int[size[0]*size[1]];for(int i=0;i<p.length;i++)p[i]=random.nextInt();byte[] b=write(size[0],size[1],p);check(Arrays.equals(p,read(b).pixels),"ARGB roundtrip");if(size[0]==17)try(OutputStream out=new FileOutputStream(args.length==0?"/tmp/velyntora-test.ico":args[0])){out.write(b);}}
  for(int bits:new int[]{1,4,8}){IcoCodec.Image image=read(indexed(bits));check(image.pixels[0]==0xffff0000&&(image.pixels[1]>>>24)==0,"indexed/mask "+bits);}
  byte[] rgb=write(2,1,new int[]{0xffff0000,0xff00ff00});put(rgb,12,24,2);put(rgb,36,24,2);rgb[62]=0;rgb[63]=0;rgb[64]=(byte)255;rgb[65]=0;rgb[66]=(byte)255;rgb[67]=0;Arrays.fill(rgb,70,74,(byte)0);check(Arrays.equals(read(rgb).pixels,new int[]{0xffff0000,0xff00ff00}),"24-bit BGR");
  byte[] small=write(1,1,new int[]{0xffabcdef}),large=write(2,2,new int[]{-1,-1,-1,-1});check(read(combine(small,large)).width==2,"largest representation");put(large,38,1,4);check(read(combine(small,large)).width==1,"unsupported larger fallback");
  byte[] zeroAlpha=write(2,1,new int[]{0x123456,0xabcdef});zeroAlpha[70]=0x40;check(read(zeroAlpha).pixels[0]==0xff123456&&(read(zeroAlpha).pixels[1]>>>24)==0,"legacy zero alpha");
  BufferedImage png=new BufferedImage(2,3,BufferedImage.TYPE_INT_ARGB);png.setRGB(0,0,0x80aa3366);ByteArrayOutputStream encoded=new ByteArrayOutputStream();ImageIO.write(png,"PNG",encoded);byte[] payload=encoded.toByteArray(),container=new byte[22+payload.length];put(container,2,1,2);put(container,4,1,2);container[6]=2;container[7]=3;put(container,12,32,2);put(container,14,payload.length,4);put(container,18,22,4);System.arraycopy(payload,0,container,22,payload.length);check(read(container).pixels[0]==0x80aa3366,"embedded PNG");container[6]=4;rejected(container);
  byte[] valid=write(2,2,new int[]{-1,-1,-1,-1});rejected(Arrays.copyOf(valid,valid.length-1));put(valid,18,Integer.MAX_VALUE,4);rejected(valid);rejected(new byte[6]);
  try{write(257,1,new int[257]);throw new AssertionError("oversized icon");}catch(IllegalArgumentException expected){}
  System.out.println("ICO ARGB, 1/4/8-bit palettes, legacy masks, PNG and malformed-input tests passed");
 }
}
