package art.velyntora.core;
import java.io.*;
import java.util.*;
import javax.imageio.*;
import javax.imageio.stream.*;
import java.awt.image.BufferedImage;
public final class TiffReaderTests {
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 static byte[] encode(BufferedImage image,String compression)throws Exception{
  ImageWriter writer=ImageIO.getImageWritersByFormatName("TIFF").next();ByteArrayOutputStream out=new ByteArrayOutputStream();
  try(ImageOutputStream stream=ImageIO.createImageOutputStream(out)){writer.setOutput(stream);ImageWriteParam param=writer.getDefaultWriteParam();param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);if(compression.equals("None"))param.setCompressionMode(ImageWriteParam.MODE_DISABLED);else param.setCompressionType(compression);writer.write(null,new IIOImage(image,null,null),param);}finally{writer.dispose();}return out.toByteArray();
 }
 static void compare(byte[] file,BufferedImage reference)throws Exception{TiffReader.Image image=TiffReader.read(new ByteArrayInputStream(file));check(image.width==reference.getWidth()&&image.height==reference.getHeight(),"dimensions");for(int y=0;y<image.height;y++)for(int x=0;x<image.width;x++)check(image.pixels[y*image.width+x]==reference.getRGB(x,y),"pixel "+x+","+y);}
 static void reverse(byte[] b,int p,int length){for(int i=0;i<length/2;i++){byte value=b[p+i];b[p+i]=b[p+length-1-i];b[p+length-1-i]=value;}}
 static byte[] bigEndian(byte[] input){byte[] b=input.clone();b[0]=b[1]='M';reverse(b,2,2);reverse(b,4,4);reverse(b,8,2);for(int i=0;i<12;i++){int p=10+i*12,type=word(input,p+2),count=word(input,p+4);reverse(b,p,2);reverse(b,p+2,2);reverse(b,p+4,4);reverse(b,p+8,type==3&&count==1?2:4);}for(int p=158;p<166;p+=2)reverse(b,p,2);return b;}
 static void rejected(byte[] bytes)throws Exception{try{TiffReader.read(new ByteArrayInputStream(bytes));throw new AssertionError("accepted malformed TIFF");}catch(IOException expected){}}
 static int word(byte[] b,int p){return (b[p]&255)|(b[p+1]&255)<<8;}
 static int tag(byte[] b,int id){for(int i=0;i<word(b,8);i++)if(word(b,10+i*12)==id)return 10+i*12;throw new AssertionError("missing tag");}
 public static void main(String[] args)throws Exception{
  Random random=new Random(517);int[] types={BufferedImage.TYPE_INT_RGB,BufferedImage.TYPE_INT_ARGB,BufferedImage.TYPE_BYTE_GRAY,BufferedImage.TYPE_BYTE_INDEXED};int cases=0;
  for(int type:types){BufferedImage image=new BufferedImage(129,97,type);for(int y=0;y<97;y++)for(int x=0;x<129;x++)image.setRGB(x,y,random.nextInt());for(String compression:new String[]{"None","LZW","PackBits","Deflate","ZLib"}){byte[] file=encode(image,compression);BufferedImage reference=ImageIO.read(new ByteArrayInputStream(file));if(type==BufferedImage.TYPE_BYTE_GRAY){BufferedImage gray=new BufferedImage(129,97,BufferedImage.TYPE_INT_RGB);for(int y=0;y<97;y++)for(int x=0;x<129;x++){int value=image.getRaster().getSample(x,y,0);gray.setRGB(x,y,0xff000000|value*0x010101);}reference=gray;}compare(file,reference);cases++;}}
  int[] pixels={0x80ff0000,0xff00ff00,0xff0000ff,0x11223344,0,0xffffffff};ByteArrayOutputStream out=new ByteArrayOutputStream();TiffWriter.write(out,3,2,(y,row)->System.arraycopy(pixels,y*3,row,0,3));byte[] original=out.toByteArray();TiffReader.Image result=TiffReader.read(new ByteArrayInputStream(original));check(Arrays.equals(result.pixels,pixels),"Core alpha roundtrip");
  for(int orientation=1;orientation<=8;orientation++){byte[] oriented=original.clone();oriented[tag(oriented,274)+8]=(byte)orientation;for(byte[] file:new byte[][]{oriented,bigEndian(oriented)}){TiffReader.Image actual=TiffReader.read(new ByteArrayInputStream(file));check(actual.width==(orientation>=5?2:3)&&actual.height==(orientation>=5?3:2),"orientation dimensions");for(int y=0;y<2;y++)for(int x=0;x<3;x++){int dx=x,dy=y;switch(orientation){case 2:dx=2-x;break;case 3:dx=2-x;dy=1-y;break;case 4:dy=1-y;break;case 5:dx=y;dy=x;break;case 6:dx=1-y;dy=x;break;case 7:dx=1-y;dy=2-x;break;case 8:dx=y;dy=2-x;break;}check(actual.pixels[dy*actual.width+dx]==pixels[y*3+x],"orientation pixel");}}}
  byte[] predicted=original.clone();int predictorTag=tag(predicted,284);predicted[predictorTag]=61;predicted[predictorTag+1]=1;predicted[predictorTag+8]=2;
  for(int y=0;y<2;y++)for(int x=2;x>=1;x--)for(int channel=0;channel<4;channel++){int p=166+y*12+x*4+channel;predicted[p]=(byte)((original[p]&255)-(original[p-4]&255));}
  check(Arrays.equals(TiffReader.read(new ByteArrayInputStream(predicted)).pixels,pixels),"horizontal predictor");
  byte[] associated=original.clone();associated[tag(associated,338)+8]=1;associated[166]=(byte)128;associated[167]=associated[168]=0;check(TiffReader.read(new ByteArrayInputStream(associated)).pixels[0]==0x80ff0000,"associated alpha");
  for(String compression:new String[]{"LZW","PackBits","Deflate"}){byte[] compressed=encode(new BufferedImage(129,97,BufferedImage.TYPE_INT_RGB),compression);rejected(Arrays.copyOf(compressed,compressed.length-1));}
  byte[] broken=original.clone();int offset=tag(broken,273)+8;Arrays.fill(broken,offset,offset+4,(byte)0x7f);rejected(broken);rejected(Arrays.copyOf(original,original.length-1));broken=original.clone();broken[tag(broken,258)+8]=0;rejected(broken);rejected(new byte[]{'I','I',42,0});
  System.out.println("TIFF reader: "+cases+" independent RGB/alpha/gray/palette codec cases, dictionary transitions, roundtrip and malformed inputs passed");
 }
}
