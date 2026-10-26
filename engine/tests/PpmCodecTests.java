package art.velyntora.core;
import java.io.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
public final class PpmCodecTests {
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 static PpmCodec.Image read(byte[] b)throws Exception{return PpmCodec.read(new ByteArrayInputStream(b));}
 static byte[] ascii(String s){return s.getBytes(StandardCharsets.US_ASCII);}
 static void rejected(byte[] b)throws Exception{try{read(b);throw new AssertionError("Malformed PPM accepted");}catch(IOException expected){}}
 public static void main(String[] args)throws Exception{
  check(Arrays.equals(read(ascii("P3\n# dimensions\n2 1\n15\n15 0 0 # red\n0 15 0 # green")).pixels,new int[]{0xffff0000,0xff00ff00}),"P3 comments/scaling");
  ByteArrayOutputStream binary=new ByteArrayOutputStream();binary.write(ascii("P6\r\n2 1\r\n255\r\n"));binary.write(new byte[]{10,32,35,(byte)255,0,0});check(Arrays.equals(read(binary.toByteArray()).pixels,new int[]{0xff0a2023,0xffff0000}),"binary whitespace/hash preserved");
  binary.reset();binary.write(ascii("P6\n1 1\n65535\n"));binary.write(new byte[]{(byte)255,(byte)255,(byte)128,0,0,0});check(read(binary.toByteArray()).pixels[0]==0xffff8000,"16-bit big endian");
  check(Arrays.equals(read(ascii("P1 3 1 # row\n0 1 0")).pixels,new int[]{0xffffffff,0xff000000,0xffffffff}),"PBM ASCII black polarity");
  check(Arrays.equals(read(ascii("P2 3 1 15 0 7 15")).pixels,new int[]{0xff000000,0xff777777,0xffffffff}),"PGM ASCII grayscale");
  binary.reset();binary.write(ascii("P4\n9 2\n"));binary.write(new byte[]{(byte)0x80,(byte)0x80,0x01,0});int[] packed=read(binary.toByteArray()).pixels;
  check(packed[0]==0xff000000&&packed[8]==0xff000000&&packed[9]==0xffffffff&&packed[16]==0xff000000&&packed[17]==0xffffffff,"PBM row padding");
  binary.reset();binary.write(ascii("P5\n2 1\n65535\n"));binary.write(new byte[]{(byte)128,0,(byte)255,(byte)255});check(Arrays.equals(read(binary.toByteArray()).pixels,new int[]{0xff808080,0xffffffff}),"PGM 16-bit");
  for(String bad:new String[]{"P1 1 1 2","P2 1 1 15 16","P4 9 1\nX","P5 1 1 255\n","P7 1 1 255"})rejected(ascii(bad));
  Random random=new Random(15);int[] pixels=new int[17*13],expected=new int[pixels.length];for(int i=0;i<pixels.length;i++){pixels[i]=random.nextInt();int alpha=pixels[i]>>>24;expected[i]=0xff000000;for(int shift=0;shift<=16;shift+=8)expected[i]|=(((pixels[i]>>>shift&255)*alpha+255*(255-alpha)+127)/255)<<shift;}
  binary.reset();PpmCodec.write(binary,17,13,(y,row)->System.arraycopy(pixels,y*17,row,0,17));byte[] encoded=binary.toByteArray();check(Arrays.equals(read(encoded).pixels,expected),"roundtrip white alpha composition");
  try(OutputStream output=new FileOutputStream(args.length==0?"/tmp/velyntora-test.ppm":args[0])){output.write(encoded);}
  for(String malformed:new String[]{"P3 0 1 255","P3 10001 1 255","P3 1 1 0","P3 1 1 65536","P3 1 1 2 3 0 0","P3 1 1 255 0 0","P3 1 1 255 0 0 0 0","P3 99999999999 1 255","P6 1 1 255","P6 1 1 255\nxx","P6 1 1 255\nxxxx"})rejected(ascii(malformed));
  System.out.println("PPM P3/P6 comments, scaling, binary boundaries, 16-bit, alpha composition and invalid-input tests passed");
 }
}
