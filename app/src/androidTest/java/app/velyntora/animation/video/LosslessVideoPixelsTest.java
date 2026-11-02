package app.velyntora.animation.video;
import static org.junit.Assert.*;import android.graphics.*;import java.io.*;import java.nio.*;import java.util.*;
import androidx.test.platform.app.InstrumentationRegistry;import app.velyntora.animation.storage.AnimationProject;import app.velyntora.animation.model.AnimationSequence;import org.junit.Test;
public final class LosslessVideoPixelsTest{
 @Test public void nativeColorConversionHonorsLimitedAndFullRange()throws Exception{
  Bitmap image=Bitmap.createBitmap(4,4,Bitmap.Config.ARGB_8888);
  ByteBuffer y=ByteBuffer.allocateDirect(16),u=ByteBuffer.allocateDirect(4),v=ByteBuffer.allocateDirect(4);
  for(int n=0;n<16;n++)y.put(n,(byte)(n%4<2?16:235));for(int n=0;n<4;n++){u.put(n,(byte)128);v.put(n,(byte)128);}
  YuvFrameConverter.convert(image,y,u,v,4,2,2,1,1,1,0,0,4,4,1,2);
  assertEquals(Color.BLACK,image.getPixel(0,0));assertEquals(Color.WHITE,image.getPixel(3,3));
  for(int n=0;n<16;n++)y.put(n,(byte)(n%4<2?0:255));
  YuvFrameConverter.convert(image,y,u,v,4,2,2,1,1,1,0,0,4,4,1,1);
  assertEquals(Color.BLACK,image.getPixel(0,0));assertEquals(Color.WHITE,image.getPixel(3,3));
  for(int n=0;n<16;n++)y.put(n,(byte)63);for(int n=0;n<4;n++){u.put(n,(byte)102);v.put(n,(byte)240);}
  YuvFrameConverter.convert(image,y,u,v,4,2,2,1,1,1,0,0,4,4,1,2);
  int color=image.getPixel(1,1);assertTrue(Color.red(color)>250&&Color.green(color)<4&&Color.blue(color)<4);image.recycle();
 }
 @Test public void fullResolutionPngRoundTripIsPixelExact()throws Exception{
  File folder=new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getCacheDir(),"lossless-"+AnimationSequence.uuid());
  AnimationProject project=new AnimationProject(folder,"Calidad",333,197,12);String asset=AnimationSequence.uuid();
  Bitmap image=Bitmap.createBitmap(333,197,Bitmap.Config.ARGB_8888);int[] pixels=new int[333*197];Random random=new Random(123);
  for(int n=0;n<pixels.length;n++)pixels[n]=0xff000000|random.nextInt(0x1000000);image.setPixels(pixels,0,333,0,0,333,197);
  try{
   LosslessFrameStore.sync(LosslessFrameStore.write(project,asset,image));
   Bitmap restored=BitmapFactory.decodeFile(project.rasterFile(asset).getAbsolutePath());assertNotNull(restored);
   assertEquals(333,restored.getWidth());assertEquals(197,restored.getHeight());int[] actual=new int[pixels.length];restored.getPixels(actual,0,333,0,0,333,197);
   assertArrayEquals(pixels,actual);restored.recycle();assertFalse(project.nativeFile(asset).exists());
   project.sequence.asset(asset);project.checkpoint();ByteArrayOutputStream archive=new ByteArrayOutputStream();project.exportArchive(archive);
   AnimationProject copy=new AnimationProject(new File(folder,"copy"),"x",1,1,1);
   copy.importArchive(new ByteArrayInputStream(archive.toByteArray()));assertTrue(copy.rasterFile(asset).length()>0);
   Bitmap copied=BitmapFactory.decodeFile(copy.rasterFile(asset).getAbsolutePath());copied.getPixels(actual,0,333,0,0,333,197);assertArrayEquals(pixels,actual);copied.recycle();
  }finally{image.recycle();}
 }
}
