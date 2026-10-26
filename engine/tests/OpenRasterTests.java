package art.velyntora.core;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
public final class OpenRasterTests {
 static void check(boolean value){if(!value)throw new AssertionError();}
 // Host codec substitutes raw ARGB for PNG; Android handles PNG compression separately.
 static final OpenRasterArchive.PngWriter WRITE=(w,h,p,o)->{DataOutputStream data=new DataOutputStream(o);for(int value:p)data.writeInt(value);};
 static final OpenRasterArchive.PngReader READ=(i,w,h,x,y)->{if(x!=0||y!=0)throw new IOException();DataInputStream data=new DataInputStream(i);int[] pixels=new int[w*h];for(int p=0;p<pixels.length;p++)pixels[p]=data.readInt();return pixels;};
 static void reject(String xml)throws Exception{boolean rejected=false;try{OpenRasterArchive.metadata(xml.getBytes(StandardCharsets.UTF_8));}catch(IOException e){rejected=true;}check(rejected);}
 public static void main(String[] args)throws Exception{
  ByteArrayOutputStream source=new ByteArrayOutputStream();DataOutputStream data=new DataOutputStream(source);VlyProjectStream.header(data,2,1,3,1);
  VlyProjectStream.layer(data,new VlyProjectStream.Layer("Fondo & <azul>",true,1,new int[]{0xff0000ff,0}),2);
  VlyProjectStream.layer(data,new VlyProjectStream.Layer("Rojo \"半\"",true,.5f,new int[]{0xffff0000,0x80ff0000}),2);
  VlyProjectStream.layer(data,new VlyProjectStream.Layer("Oculta",false,.2f,new int[]{-1,-1}),2);
  ByteArrayOutputStream archive=new ByteArrayOutputStream();OpenRasterArchive.write(new ByteArrayInputStream(source.toByteArray()),archive,WRITE);File file=File.createTempFile("velyntora-ora-",".ora");
  try{try(FileOutputStream out=new FileOutputStream(file)){out.write(archive.toByteArray());}try(ZipFile zip=new ZipFile(file)){ZipEntry first=zip.entries().nextElement();check(first.getName().equals("mimetype")&&first.getMethod()==ZipEntry.STORED);String xml=new String(OpenRasterArchive.bounded(zip.getInputStream(zip.getEntry("stack.xml")),1048576),StandardCharsets.UTF_8);OpenRasterArchive.Stack stack=OpenRasterArchive.metadata(xml.getBytes(StandardCharsets.UTF_8));check(stack.layers.get(0).name.equals("Oculta")&&stack.layers.get(2).name.equals("Fondo & <azul>"));DataInputStream merged=new DataInputStream(zip.getInputStream(zip.getEntry("mergedimage.png")));check(merged.readInt()==0xff800080&&merged.readInt()==0x40ff0000);}
   ByteArrayOutputStream restored=new ByteArrayOutputStream();OpenRasterArchive.read(file,restored,READ);check(Arrays.equals(source.toByteArray(),restored.toByteArray()));
  }finally{file.delete();}
  String start="<image w='2' h='1'><stack>",end="</stack></image>";reject(start+"<stack><layer src='data/l.png'/></stack>"+end);reject(start+"<layer src='data/../l.png'/>"+end);reject(start+"<layer src='data/l.png' composite-op='svg:unknown'/>"+end);reject(start+"<layer src='data/l.png' opacity='NaN'/>"+end);reject("<!DOCTYPE image [<!ENTITY a 'test'>]>"+start+"<layer src='data/l.png'/>"+end);reject("<image w='10001' h='10001'><stack><layer src='data/l.png'/></stack></image>");
  OpenRasterArchive.Stack stack=OpenRasterArchive.metadata((start+"<layer src='data/l.png' x='-2' y='3'/>"+end).getBytes(StandardCharsets.UTF_8));check(stack.layers.get(0).x==-2&&stack.layers.get(0).y==3);
  check(OpenRasterArchive.metadata(("\uFEFF"+start+"<layer src='data/l.png'/>"+end).getBytes(StandardCharsets.UTF_8)).layers.size()==1);
  boolean invalidUtf8=false;try{OpenRasterArchive.metadata(new byte[]{(byte)0xff});}catch(IOException e){invalidUtf8=true;}check(invalidUtf8);
  // Exercise real PNG entries as well as the exact streamed VLY representation.
  OpenRasterArchive.PngWriter pngWriter=(w,h,p,o)->{BufferedImage image=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);image.setRGB(0,0,w,h,p,0,w);if(!ImageIO.write(image,"png",o))throw new IOException();};
  OpenRasterArchive.PngReader pngReader=(i,w,h,x,y)->{BufferedImage image=ImageIO.read(i);if(image==null||image.getWidth()!=w||image.getHeight()!=h)throw new IOException();return image.getRGB(0,0,w,h,null,0,w);};
  archive.reset();OpenRasterArchive.write(new ByteArrayInputStream(source.toByteArray()),archive,pngWriter);file=File.createTempFile("velyntora-real-png-",".ora");try{try(FileOutputStream out=new FileOutputStream(file)){out.write(archive.toByteArray());}ByteArrayOutputStream restored=new ByteArrayOutputStream();OpenRasterArchive.read(file,restored,pngReader);check(Arrays.equals(source.toByteArray(),restored.toByteArray()));try(ZipFile zip=new ZipFile(file)){BufferedImage preview=ImageIO.read(zip.getInputStream(zip.getEntry("Thumbnails/thumbnail.png")));check(preview.getWidth()==2&&preview.getHeight()==1);BufferedImage merged=ImageIO.read(zip.getInputStream(zip.getEntry("mergedimage.png")));check(merged.getRGB(0,0)==0xff800080&&merged.getRGB(1,0)==0x40ff0000);}}finally{file.delete();}
  System.out.println("OpenRaster stack order, names, opacity, visibility, active layer, merged alpha, roundtrip and invalid metadata passed");
 }
}
