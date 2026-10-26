package art.velyntora.core;
import java.io.*;
import java.util.*;
public final class ExifOrientationTests {
 static void check(boolean v){if(!v)throw new AssertionError();}
 static byte[] jpeg(int orientation,boolean little){byte[] b=new byte[40];b[0]=(byte)255;b[1]=(byte)216;b[2]=(byte)255;b[3]=(byte)225;b[5]=34;byte[] sig={'E','x','i','f',0,0};System.arraycopy(sig,0,b,6,6);b[12]=b[13]=(byte)(little?'I':'M');int base=little?0:1;b[14+base]=42;b[16+(little?0:3)]=8;b[20+base]=1;b[22+(little?1:0)]=1;b[22+base]=18;b[24+base]=3;b[26+(little?0:3)]=1;b[30+base]=(byte)orientation;b[38]=(byte)255;b[39]=(byte)217;return b;}
 public static void main(String[] args)throws Exception{
  for(boolean le:new boolean[]{true,false})for(int o=1;o<=8;o++)check(ExifOrientation.read(new ByteArrayInputStream(jpeg(o,le)))==o);
  for(int length=0;length<38;length++)check(ExifOrientation.read(new ByteArrayInputStream(Arrays.copyOf(jpeg(6,true),length)))==1);
  byte[] bad=jpeg(6,true);Arrays.fill(bad,16,20,(byte)255);check(ExifOrientation.read(new ByteArrayInputStream(bad))==1);
  check(ExifOrientation.read(new ByteArrayInputStream(jpeg(9,false)))==1);
  int[][] expected={{0,1,2,3,4,5},{1,0,3,2,5,4},{5,4,3,2,1,0},{4,5,2,3,0,1},{0,2,4,1,3,5},{4,2,0,5,3,1},{5,3,1,4,2,0},{1,3,5,0,2,4}};
  for(int o=1;o<=8;o++){int w=o>=5?3:2,h=o>=5?2:3;for(int y=0;y<h;y++)for(int x=0;x<w;x++)check(ExifOrientation.sourceIndex(x,y,2,3,o)==expected[o-1][y*w+x]);}
  System.out.println("JPEG EXIF endian, truncation, offsets and all eight transformations passed");
 }
}
