package art.velyntora.core;
import java.io.*;
import java.nio.charset.StandardCharsets;
public final class VlyTextElementStreamTests {
 public static void main(String[] args)throws Exception {
  ByteArrayOutputStream buffer=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(buffer);VlyProjectStream.header(out,2,2,2,1);
  VlyProjectStream.layer(out,new VlyProjectStream.Layer("Texto",true,.5f,new int[]{1,2,3,4}),4);
  VlyProjectStream.integer(out,1);for(float v:new float[]{0,0,2,2,1,1,2,2,0})VlyProjectStream.integer(out,Float.floatToIntBits(v));VlyProjectStream.integer(out,1);out.write(1);
  VlyProjectStream.integer(out,1);byte[] text="hola 日本語 😀".getBytes(StandardCharsets.UTF_8);VlyProjectStream.integer(out,text.length);out.write(text);for(int v:new int[]{32,0xff123456,2,3,400})VlyProjectStream.integer(out,v);
  VlyProjectStream.layer(out,new VlyProjectStream.Layer("Otra",true,1,new int[]{5,6,7,8}),4);VlyProjectStream.integer(out,0);VlyProjectStream.integer(out,0);
  byte[] bytes=buffer.toByteArray();bytes[7]=4;VlyProjectStream.Reader reader=new VlyProjectStream.Reader(new ByteArrayInputStream(bytes));if(!reader.next().name.equals("Texto") || reader.next().pixels[3]!=8)throw new AssertionError("Text metadata shifted raster layers");reader.finish();
  boolean rejected=false;try{reader=new VlyProjectStream.Reader(new ByteArrayInputStream(java.util.Arrays.copyOf(bytes,bytes.length-1)));reader.next();reader.next();reader.finish();}catch(IOException expected){rejected=true;}if(!rejected)throw new AssertionError("Truncated text accepted");System.out.println("Text element raster export adapter passed");
 }
}
