package art.velyntora.core;
import java.io.*;
import java.util.Arrays;
public final class TgaReaderTests {
 static void check(boolean v){if(!v)throw new AssertionError();}
 static byte[] file(int type,int descriptor,int... body)throws Exception{ByteArrayOutputStream out=new ByteArrayOutputStream();out.write(new byte[]{0,0,(byte)type,0,0,0,0,0,0,0,0,0,2,0,2,0,32,(byte)descriptor});for(int v:body)out.write(v);return out.toByteArray();}
 public static void main(String[] args)throws Exception{
  int[] pixels={0x80ff0000,0xff00ff00,0xff0000ff,0};ByteArrayOutputStream out=new ByteArrayOutputStream();RasterFileWriter.write(out,2,2,true,(y,row)->System.arraycopy(pixels,y*2,row,0,2));TgaReader.Image image=TgaReader.read(new ByteArrayInputStream(out.toByteArray()));check(image.width==2&&image.height==2&&Arrays.equals(image.pixels,pixels));
  byte[] rle=file(10,0x28,129,0,0,255,128,1,0,255,0,255,255,0,0,255);image=TgaReader.read(new ByteArrayInputStream(rle));check(Arrays.equals(image.pixels,new int[]{0x80ff0000,0x80ff0000,0xff00ff00,0xff0000ff}));
  byte[] bottom=file(2,0x18,0,0,255,255,0,255,0,255,255,0,0,255,255,255,255,255);image=TgaReader.read(new ByteArrayInputStream(bottom));check(Arrays.equals(image.pixels,new int[]{0xffffffff,0xff0000ff,0xff00ff00,0xffff0000}));
  byte[] opaque=file(10,0x20,131,0,0,255,0);image=TgaReader.read(new ByteArrayInputStream(opaque));for(int pixel:image.pixels)check(pixel==0xffff0000);
  for(byte[] invalid:new byte[][]{Arrays.copyOf(rle,rle.length-1),file(10,0x28,132,0,0,0,255),file(1,0x28),file(2,0xe8)}){boolean rejected=false;try{TgaReader.read(new ByteArrayInputStream(invalid));}catch(IOException e){rejected=true;}check(rejected);}
  byte[] gray=file(3,0x20,0,85,170,255);gray[16]=8;check(Arrays.equals(TgaReader.read(new ByteArrayInputStream(gray)).pixels,new int[]{0xff000000,0xff555555,0xffaaaaaa,0xffffffff}));
  byte[] grayAlpha=file(11,0x28,131,120,64);grayAlpha[16]=16;for(int pixel:TgaReader.read(new ByteArrayInputStream(grayAlpha)).pixels)check(pixel==0x40787878);
  byte[] rgb16=file(2,0x21,0,252,224,131,31,128,0,0);rgb16[16]=16;check(Arrays.equals(TgaReader.read(new ByteArrayInputStream(rgb16)).pixels,new int[]{0xffff0000,0xff00ff00,0xff0000ff,0}));
  byte[] indexed=file(1,0x20,0,0,255,0,255,0,4,5,5,4);indexed[1]=1;indexed[3]=4;indexed[5]=2;indexed[7]=24;indexed[16]=8;check(Arrays.equals(TgaReader.read(new ByteArrayInputStream(indexed)).pixels,new int[]{0xffff0000,0xff00ff00,0xff00ff00,0xffff0000}));
  indexed[indexed.length-1]=6;boolean rejected=false;try{TgaReader.read(new ByteArrayInputStream(indexed));}catch(IOException e){rejected=true;}check(rejected);
  byte[] indexedRle=file(9,0x20,255,0,0,131,0);indexedRle[1]=1;indexedRle[5]=1;indexedRle[7]=24;indexedRle[16]=8;for(int pixel:TgaReader.read(new ByteArrayInputStream(indexedRle)).pixels)check(pixel==0xff0000ff);
  System.out.println("TGA roundtrip, RLE, origins, alpha and malformed packets passed");
 }
}
