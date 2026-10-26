package art.velyntora.core;

import android.graphics.*;
import android.text.*;
import android.net.Uri;
import java.io.*;

/** Raster cache in strips; never allocates a full canvas-sized text bitmap. */
final class TextSourceRenderer {
  static PlacedImageSource render(MainActivity host,TextSpec spec)throws IOException {
    TextGlyphLayout glyphs=new TextGlyphLayout(spec);int width=glyphs.width,height=glyphs.height;
    if(height<1 || height>6000)throw new IOException("El texto es demasiado alto. Reduce el tamaño o los saltos de línea.");
    File file=File.createTempFile("text-source-",".png",host.getCacheDir());Bitmap strip=Bitmap.createBitmap(width,Math.min(128,height),Bitmap.Config.ARGB_8888);int[] top={-1};
    try{
      try(OutputStream out=new BufferedOutputStream(new FileOutputStream(file))){PngWriter.write(out,width,height,(y,pixels)->{
        if(top[0]<0 || y>=top[0]+strip.getHeight()){top[0]=y;strip.eraseColor(Color.TRANSPARENT);Canvas canvas=new Canvas(strip);canvas.translate(0,-y);glyphs.draw(canvas);}
        strip.getPixels(pixels,0,width,0,y-top[0],width,1);
      });}
      return PlacedImageSource.load(host,Uri.fromFile(file));
    }finally{strip.recycle();file.delete();}
  }
}
