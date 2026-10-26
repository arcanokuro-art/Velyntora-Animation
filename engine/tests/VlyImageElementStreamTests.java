package art.velyntora.core;
import java.io.*;
public final class VlyImageElementStreamTests {
  public static void main(String[] args)throws Exception {
    ByteArrayOutputStream buffer=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(buffer);
    VlyProjectStream.header(out,2,2,2,1);
    VlyProjectStream.layer(out,new VlyProjectStream.Layer("Imagen",true,.5f,new int[]{1,2,3,4},4),4);
    VlyProjectStream.integer(out,1);
    for(float v:new float[]{0,0,2,2,1,1,2,2,0})VlyProjectStream.integer(out,Float.floatToIntBits(v));
    VlyProjectStream.integer(out,150000);out.write(new byte[150000]);
    VlyProjectStream.layer(out,new VlyProjectStream.Layer("Superior",true,1,new int[]{5,6,7,8}),4);VlyProjectStream.integer(out,0);
    byte[] bytes=buffer.toByteArray();bytes[7]=3;
    VlyProjectStream.Reader reader=new VlyProjectStream.Reader(new ByteArrayInputStream(bytes));
    VlyProjectStream.Layer first=reader.next(),second=reader.next();reader.finish();
    if(first.blendMode!=4 || first.opacity!=.5f || first.pixels[3]!=4 || second.pixels[0]!=5 || !second.name.equals("Superior"))throw new AssertionError("Raster adapter lost layer data");
    boolean rejected=false;try{reader=new VlyProjectStream.Reader(new ByteArrayInputStream(java.util.Arrays.copyOf(bytes,bytes.length-1)));reader.next();reader.next();reader.finish();}catch(IOException expected){rejected=true;}if(!rejected)throw new AssertionError("Truncated metadata accepted");
    System.out.println("Native image-element project raster adapter passed");
  }
}
