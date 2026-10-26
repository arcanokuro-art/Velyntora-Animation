package art.velyntora.core;
import static org.junit.Assert.*;
import android.graphics.*;
import org.junit.Test;
public final class TileDisplayDeviceTests {
  @Test public void exactPixelsAcrossTilesAndDirtyGutters() {
    Bitmap source=Bitmap.createBitmap(513,517,Bitmap.Config.ARGB_8888);
    for(int y=0;y<517;y++)for(int x=0;x<513;x++)source.setPixel(x,y,0xff000000|((x*3&255)<<16)|((y*7&255)<<8)|(x+y&255));
    TileDisplay tiles=new TileDisplay(source);Bitmap output=Bitmap.createBitmap(513,517,Bitmap.Config.ARGB_8888);
    try {
      tiles.draw(new Canvas(output),1);
      for(int y=0;y<517;y++)for(int x=0;x<513;x++)assertEquals(source.getPixel(x,y),output.getPixel(x,y));
      source.setPixel(256,256,0xffabcdef);tiles.invalidate(256,256,1,1);tiles.draw(new Canvas(output),1);
      assertEquals(0xffabcdef,output.getPixel(256,256));
      assertEquals(source.getPixel(255,256),output.getPixel(255,256));
      assertEquals(source.getPixel(512,516),output.getPixel(512,516));
    } finally {tiles.clear();source.recycle();output.recycle();}
  }
  @Test public void tinyChangesReuseTexturesAndReadOnlyTheirSampleFootprint() {
    Bitmap source=Bitmap.createBitmap(3033,4515,Bitmap.Config.ARGB_8888);source.eraseColor(0xffffffff);
    Bitmap output=Bitmap.createBitmap(640,960,Bitmap.Config.ARGB_8888);TileDisplay tiles=new TileDisplay(source);
    try {
      tiles.warm(.2f);Canvas canvas=new Canvas(output);canvas.scale(.2f,.2f);tiles.draw(canvas,.2f);
      long allocations=tiles.createdTiles;
      for(int edit=0;edit<40;edit++) {
        int x=1500+edit,y=2200;
        source.setPixel(x,y,0xff123456);tiles.invalidate(x,y,1,1);
        long reads=tiles.sampledPixels(),start=android.os.SystemClock.uptimeMillis();
        tiles.draw(canvas,.2f);
        assertEquals("A small edit must reuse its texture",allocations,tiles.createdTiles);
        assertTrue("A small edit must not sample a whole tile",tiles.sampledPixels()-reads<=64);
        assertTrue("Cached redraw stalled",android.os.SystemClock.uptimeMillis()-start<100);
      }
      long reads=tiles.sampledPixels();Canvas closer=new Canvas(output);closer.scale(.4f,.4f);closer.translate(-1000,-1800);tiles.draw(closer,.4f);
      assertEquals("Next zoom level was prepared before interaction",allocations,tiles.createdTiles);
      assertTrue("Zoom must reuse unchanged samples",tiles.sampledPixels()-reads<=1000);
    } finally {tiles.clear();source.recycle();output.recycle();}
  }
  @Test public void rotatedTileGuttersPreserveOpaqueAndTranslucentPixels() {
    for(int color:new int[]{0xff454545,0x80444444})for(float angle:new float[]{5.8f,-19.7f,37f}){
      Bitmap source=Bitmap.createBitmap(1025,1025,Bitmap.Config.ARGB_8888);source.eraseColor(color);
      Bitmap actual=Bitmap.createBitmap(512,512,Bitmap.Config.ARGB_8888),expected=Bitmap.createBitmap(512,512,Bitmap.Config.ARGB_8888);
      TileDisplay tiles=new TileDisplay(source);
      try {
        Canvas a=new Canvas(actual),b=new Canvas(expected);
        for(Canvas c:new Canvas[]{a,b}){c.translate(256,256);c.rotate(angle);c.scale(1.3f,1.3f);c.translate(-512,-512);}
        tiles.draw(a,1.3f);Paint paint=new Paint();b.drawBitmap(source,0,0,paint);
        for(int y=0;y<512;y++)for(int x=0;x<512;x++)assertEquals("Tile seam at "+x+","+y,expected.getPixel(x,y),actual.getPixel(x,y));
      }finally{tiles.clear();source.recycle();actual.recycle();expected.recycle();}
    }
  }

}
