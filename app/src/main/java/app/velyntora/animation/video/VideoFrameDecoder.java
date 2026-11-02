package app.velyntora.animation.video;

import android.content.Context;
import android.graphics.*;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import java.io.IOException;

/** Bounded, one-frame-at-a-time platform decoder. No external codec package. */
public final class VideoFrameDecoder implements AutoCloseable {
  private final MediaMetadataRetriever reader = new MediaMetadataRetriever();
  private final VideoImportRequest request;

  public VideoFrameDecoder(Context context, VideoImportRequest request) throws IOException {
    this.request=request;
    try { reader.setDataSource(context,request.uri); }
    catch(RuntimeException e){close();throw new IOException("No se puede abrir este video",e);}
  }
  public Bitmap frame(int index) throws IOException {
    Bitmap source=null, output=null;
    try {
      long time=request.timestampUs(index);
      if(Build.VERSION.SDK_INT>=27)
        source=reader.getScaledFrameAtTime(time,MediaMetadataRetriever.OPTION_CLOSEST,
            request.width,request.height);
      else source=reader.getFrameAtTime(time,MediaMetadataRetriever.OPTION_CLOSEST);
      if(source==null)throw new IOException("No se pudo decodificar el fotograma "+(index+1));
      output=Bitmap.createBitmap(request.width,request.height,Bitmap.Config.ARGB_8888);
      Canvas canvas=new Canvas(output);canvas.drawColor(Color.BLACK);
      float scale=Math.min(request.width/(float)source.getWidth(),request.height/(float)source.getHeight());
      float w=source.getWidth()*scale,h=source.getHeight()*scale;
      canvas.drawBitmap(source,null,new RectF((request.width-w)/2,(request.height-h)/2,
          (request.width+w)/2,(request.height+h)/2),new Paint(Paint.FILTER_BITMAP_FLAG));
      return output;
    } catch(RuntimeException e) {
      if(output!=null)output.recycle();
      throw new IOException("No se pudo extraer el fotograma "+(index+1),e);
    } finally {if(source!=null)source.recycle();}
  }
  public void close(){try{reader.release();}catch(Exception ignored){}}
}
