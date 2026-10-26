package art.velyntora.core;
import java.io.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
public final class TiffWriterTests {
 static void check(boolean value){if(!value)throw new AssertionError();}
 static int number(byte[] data,int offset,int count){int value=0;for(int i=0;i<count;i++)value|=(data[offset+i]&255)<<(8*i);return value;}
 public static void main(String[] args)throws Exception{int[] pixels={0x80ff0000,0xff00ff00,0xff0000ff,0};ByteArrayOutputStream out=new ByteArrayOutputStream();TiffWriter.write(out,2,2,(y,row)->System.arraycopy(pixels,y*2,row,0,2));byte[] data=out.toByteArray();check(data.length==182&&data[0]=='I'&&data[1]=='I'&&number(data,2,2)==42);check(number(data,8,2)==12);int previous=0;for(int i=0;i<12;i++){int offset=10+i*12,id=number(data,offset,2);check(id>previous);previous=id;if(id==338)check(number(data,offset+8,4)==2);if(id==273)check(number(data,offset+8,4)==166);}BufferedImage image=ImageIO.read(new ByteArrayInputStream(data));check(image!=null&&image.getWidth()==2&&image.getHeight()==2);for(int y=0;y<2;y++)for(int x=0;x<2;x++)check(image.getRGB(x,y)==pixels[y*2+x]);boolean rejected=false;try{TiffWriter.write(out,0,2,(y,row)->{});}catch(IllegalArgumentException e){rejected=true;}check(rejected);System.out.println("TIFF directory, strips, byte order, real decoder roundtrip and alpha passed");}
}
