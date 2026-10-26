package art.velyntora.core;
import java.io.*;
import javax.imageio.*;
import java.awt.image.BufferedImage;
public final class PngWriterTests{
 public static void main(String[] args)throws Exception{
  int[] colors={0xff123456,0x80345678,0x00010203,0xffffffff,0xff000000,0x7fabcdef};
  ByteArrayOutputStream out=new ByteArrayOutputStream();
  PngWriter.write(out,3,2,(y,row)->System.arraycopy(colors,y*3,row,0,3));
  BufferedImage image=ImageIO.read(new ByteArrayInputStream(out.toByteArray()));
  for(int y=0;y<2;y++)for(int x=0;x<3;x++)if(image.getRGB(x,y)!=colors[y*3+x])throw new AssertionError("RGBA mismatch");
  out.reset();int[] rows={0};
  PngWriter.write(out,6000,6000,(y,row)->{java.util.Arrays.fill(row,0xff123456);rows[0]++;});
  if(rows[0]!=6000 || out.size()>5000000)throw new AssertionError("Unbounded PNG output");
  try(javax.imageio.stream.ImageInputStream input=ImageIO.createImageInputStream(new ByteArrayInputStream(out.toByteArray()))){
   ImageReader reader=ImageIO.getImageReaders(input).next();try{reader.setInput(input);if(reader.getWidth(0)!=6000 || reader.getHeight(0)!=6000)throw new AssertionError("Wrong dimensions");}finally{reader.dispose();}
  }
  System.out.println("PNG RGBA round-trip and 6000x6000 streaming dimensions passed");
 }
}
