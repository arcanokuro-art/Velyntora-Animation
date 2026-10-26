package art.velyntora.core;
import java.io.*;
public final class PngImportTests {
 static void check(boolean value){if(!value)throw new AssertionError();}
 public static void main(String[] args)throws Exception{
  for(int orientation=1;orientation<=8;orientation++){
   ByteArrayOutputStream encoded=new ByteArrayOutputStream();
   PngWriter.write(encoded,37,29,(y,row)->{for(int x=0;x<row.length;x++)row[x]=0xff000000|(x<<16)|(y<<8)|123;});
   check(PngStripeImporter.load(new ByteArrayInputStream(encoded.toByteArray()),orientation));DrawingView.nativeAcceptImageImport();
   int w=orientation>=5?29:37,h=orientation>=5?37:29;
   check(DrawingView.nativeWidth()==w && DrawingView.nativeHeight()==h);
   for(int y=0;y<h;y++)for(int x=0;x<w;x++){
    int source=ExifOrientation.sourceIndex(x,y,37,29,orientation);
    check(DrawingView.nativeRegionPixels(x,y,1,1)[0]==(0xff000000|((source%37)<<16)|((source/37)<<8)|123));
   }
  }
  ByteArrayOutputStream encoded=new ByteArrayOutputStream();
  PngWriter.write(encoded,6000,6000,(y,row)->java.util.Arrays.fill(row,0xff000000|y));
  check(PngStripeImporter.load(new ByteArrayInputStream(encoded.toByteArray()),1));DrawingView.nativeAcceptImageImport();
  check(DrawingView.nativeWidth()==6000 && DrawingView.nativeHeight()==6000);
  for(int y:new int[]{0,1,103,5999})check(DrawingView.nativeRegionPixels(5999,y,1,1)[0]==(0xff000000|y));
  byte[] broken=encoded.toByteArray();broken[broken.length-13]^=1;
  boolean rejected=false;try{PngStripeImporter.load(new ByteArrayInputStream(broken),1);}catch(IOException error){rejected=true;}finally{DrawingView.nativeCancelImageImport();}
  check(rejected);check(DrawingView.nativeWidth()==6000);check(DrawingView.nativeRegionPixels(5999,5999,1,1)[0]==(0xff000000|5999));
  ByteArrayOutputStream oversized=new ByteArrayOutputStream();
  java.awt.image.BufferedImage fixture=new java.awt.image.BufferedImage(9030,7,java.awt.image.BufferedImage.TYPE_INT_ARGB);
  for(int y=0;y<7;y++)for(int x=0;x<9030;x++)fixture.setRGB(x,y,x<4500?0xffff0000:0xff0000ff);
  javax.imageio.ImageIO.write(fixture,"png",oversized);
  for(int orientation=1;orientation<=8;orientation++){
    ImageImportSize plan=new ImageImportSize(9030,7,orientation);
    check(PngStripeImporter.load(new ByteArrayInputStream(oversized.toByteArray()),orientation,plan));DrawingView.nativeAcceptImageImport();
    check(DrawingView.nativeWidth()==plan.displayWidth() && DrawingView.nativeHeight()==plan.displayHeight());
    for(int[] point:new int[][]{{0,0},{plan.displayWidth()-1,plan.displayHeight()-1}}){
      int source=ExifOrientation.sourceIndex(point[0],point[1],plan.width,plan.height,orientation);
      check(DrawingView.nativeRegionPixels(point[0],point[1],1,1)[0]==(source%plan.width<3000?0xffff0000:0xff0000ff));
    }
  }
  check(DrawingView.nativeCreate(64,64));System.out.println("Original 6000x6000 PNG import, all EXIF orientations and corrupt-import rollback passed");
 }
}
