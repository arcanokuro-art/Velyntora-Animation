package art.velyntora.core;

import android.graphics.*;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.net.Uri;
import java.io.*;
import java.util.concurrent.*;

/** Reopens a retained image source and composes bounded previews off the UI thread. */
final class PlacedImageElementEditor {
  static final class Prepared {
    TextVectorPreview vector;TextSpec text;PlacedImageSource source;float[] transform;float opacity;int[] preview;
  }
  static Prepared prepare(MainActivity host,int index)throws IOException {
    File file=File.createTempFile("image-element-",".source",host.getCacheDir());
    try{
      Prepared ready=new Prepared();
      try(ParcelFileDescriptor fd=ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_WRITE_ONLY|ParcelFileDescriptor.MODE_TRUNCATE)){
        ready.preview=DrawingView.nativePrepareImageEdit(index,fd.getFd());
      }
      if(ready.preview==null)throw new IOException("Image source unavailable");
      ready.transform=DrawingView.nativeImageTransform(index);ready.opacity=DrawingView.nativeIndexedLayerOpacity(index);
      if(ready.transform==null)throw new IOException("Image transform unavailable");
      ready.text=TextSpec.read(index);if(ready.text!=null)ready.vector=TextVectorPreview.prepare(ready.preview);ready.source=PlacedImageSource.load(host,Uri.fromFile(file));return ready;
    }finally{file.delete();}
  }
  final PlacedImageSession session;final int width,height,sample;
  final ExecutorService worker=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"velyntora-image-preview");t.setDaemon(true);return t;});
  final Handler ui=new Handler(Looper.getMainLooper());
  TextVectorPreview vector;Bitmap frame;boolean running,pending;volatile boolean alive=true;
  PlacedImageElementEditor(PlacedImageSession session,int[] info){this.session=session;width=info[0];height=info[1];sample=info[2];}
  void request(){
    if(!alive)return;if(vector!=null){frame=vector.lower;session.drawing.invalidate();return;}if(running){pending=true;return;}running=true;pending=false;
    PlacedImageTransform state=session.state.copy();PlacedImageSource source=session.source;TextSpec text=session.text;
    worker.execute(()->{
      Bitmap composed=null;
      try{
        if(!alive)return;
        Bitmap moving=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
        try{Canvas canvas=new Canvas(moving);canvas.scale(1f/sample,1f/sample);canvas.concat(state.matrix());canvas.clipRect(state.crop);
          if(text!=null)new TextGlyphLayout(text).draw(canvas);else canvas.drawBitmap(source.preview,null,new RectF(0,0,source.size.displayWidth(),source.size.displayHeight()),new Paint(Paint.FILTER_BITMAP_FLAG));
          int[] pixels=new int[width*height];moving.getPixels(pixels,0,width,0,0,width,height);
          if(alive){int[] output=DrawingView.nativeComposeImageEdit(pixels,state.opacity);if(output!=null)composed=Bitmap.createBitmap(output,width,height,Bitmap.Config.ARGB_8888);}
        }finally{moving.recycle();}
      }catch(RuntimeException|OutOfMemoryError error){android.util.Log.e("ImageElement","Preview failed",error);}
      finally{Bitmap ready=composed;ui.post(()->{running=false;if(!alive){if(ready!=null)ready.recycle();return;}if(ready!=null){frame=ready;session.drawing.invalidate();}if(pending)request();});}
    });
  }
  boolean draw(Canvas canvas){if(vector!=null){vector.draw(canvas,session.text,session.state);return true;}if(frame==null)return false;canvas.drawBitmap(frame,null,new RectF(0,0,width*sample,height*sample),new Paint(Paint.FILTER_BITMAP_FLAG));return true;}
  void close(){alive=false;worker.shutdownNow();frame=null;DrawingView.nativeCloseImageEdit();}
}
