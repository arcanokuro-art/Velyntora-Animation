package art.velyntora.core;

import android.graphics.*;
import android.os.ParcelFileDescriptor;
import app.velyntora.animation.model.AnimationSequence;
import app.velyntora.animation.storage.AnimationProject;
import java.io.*;

/** Worker-side bridge: native layered documents + bounded proxies; no Android Views touched. */
final class FrameDocumentAdapter {
  private final DrawingView drawing;
  static native void saveHistory(String key) throws IOException;
  static native void restoreHistory(String key) throws IOException;
  static native void dropHistory(String key);
  static native void clearHistories();

  private String historyKey(AnimationProject project, String id) {
    return project.directory.getAbsolutePath() + "/" + id;
  }
  void rememberHistory(AnimationProject project) throws IOException {
    saveHistory(historyKey(project, project.sequence.current().id));
  }
  void recallHistory(AnimationProject project) throws IOException {
    restoreHistory(historyKey(project, project.sequence.current().id));
  }
  void forgetHistory(AnimationProject project, String id) {
    dropHistory(historyKey(project, id));
  }

  FrameDocumentAdapter(DrawingView drawing) {
    this.drawing = drawing;
  }

  String capture(AnimationProject project) throws IOException {
    return capture(project,AnimationSequence.uuid());
  }

  String capture(AnimationProject project,String asset) throws IOException {
    try (ParcelFileDescriptor fd =
        ParcelFileDescriptor.open(
            project.nativeFile(asset),
            ParcelFileDescriptor.MODE_CREATE
                | ParcelFileDescriptor.MODE_TRUNCATE
                | ParcelFileDescriptor.MODE_READ_WRITE)) {
      if (!DrawingView.nativeSaveProject(fd.getFd()))
        throw new IOException("No se pudo guardar el fotograma con sus capas");
      fd.getFileDescriptor().sync();
    }
    int width = DrawingView.nativeWidth(), height = DrawingView.nativeHeight(), sample = 1;
    while ((width + sample - 1) / sample > 1024 || (height + sample - 1) / sample > 1024)
      sample *= 2;
    int w = (width + sample - 1) / sample, h = (height + sample - 1) / sample;
    int[] pixels = DrawingView.nativeSampleRegion(0, 0, sample, w, h);
    if (pixels == null || pixels.length != w * h) throw new IOException("Vista previa incompleta");
    Bitmap preview = Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888);
    try (FileOutputStream out = new FileOutputStream(project.previewFile(asset))) {
      if (!preview.compress(Bitmap.CompressFormat.PNG, 100, out))
        throw new IOException("No se pudo guardar la vista previa");
      out.getFD().sync();
    } finally {
      preview.recycle();
    }
    return asset;
  }

  void load(AnimationProject project, String asset) throws IOException {
    if(project.nativeFile(asset).length()==0&&project.rasterFile(asset).length()>0){
      BitmapFactory.Options options=new BitmapFactory.Options();options.inPreferredConfig=Bitmap.Config.ARGB_8888;
      options.inScaled=false;options.inSampleSize=1;
      Bitmap image=BitmapFactory.decodeFile(project.rasterFile(asset).getAbsolutePath(),options);
      if(image==null)throw new IOException("Fotograma PNG incompleto");
      try{importBitmap(image);}finally{image.recycle();}
      return;
    }
    try (ParcelFileDescriptor fd =
        ParcelFileDescriptor.open(project.nativeFile(asset), ParcelFileDescriptor.MODE_READ_ONLY)) {
      if (!DrawingView.nativeOpenProject(fd.getFd()))
        throw new IOException("Fotograma nativo incompatible");
    }
  }

  void importBitmap(Bitmap bitmap) throws IOException {
    int width=bitmap.getWidth(),height=bitmap.getHeight();
    int[] pixels=new int[width*height];
    bitmap.getPixels(pixels,0,width,0,0,width,height);
    if(!DrawingView.nativeLoadBitmap(width,height,pixels))
      throw new IOException("No se pudo crear el fotograma del video");
  }

  void create(int width, int height) throws IOException {
    if (!DrawingView.nativeCreate(width, height))
      throw new IOException("No hay memoria suficiente para el lienzo");
  }

  long revision() {
    return DrawingView.nativeRevision();
  }

  void present() {
    drawing.projectOpened();
  }

  Bitmap render(int width,int height,boolean opaque)throws IOException{
    int sourceWidth=DrawingView.nativeWidth(),sourceHeight=DrawingView.nativeHeight(),sample=1;
    while((sourceWidth+sample-1)/sample>width*2 || (sourceHeight+sample-1)/sample>height*2)sample*=2;
    int w=(sourceWidth+sample-1)/sample,h=(sourceHeight+sample-1)/sample;
    int[] pixels=DrawingView.nativeSampleRegion(0,0,sample,w,h);
    if(pixels==null||pixels.length!=w*h)throw new IOException("Fotograma incompleto");
    Bitmap source=Bitmap.createBitmap(pixels,w,h,Bitmap.Config.ARGB_8888);
    Bitmap target=null;
    try{
      target=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
      Canvas canvas=new Canvas(target);if(opaque)canvas.drawColor(Color.WHITE);
      float scale=Math.min(width/(float)sourceWidth,height/(float)sourceHeight);
      float dw=sourceWidth*scale,dh=sourceHeight*scale;
      canvas.drawBitmap(source,null,new RectF((width-dw)/2,(height-dh)/2,(width+dw)/2,(height+dh)/2),new Paint(3));
      return target;
    }catch(RuntimeException error){if(target!=null)target.recycle();throw error;}
    finally{source.recycle();}
  }

  void writePng(OutputStream output) throws IOException {
    int width = DrawingView.nativeWidth(), height = DrawingView.nativeHeight();
    PngWriter.write(
        output,
        width,
        height,
        (y, row) -> {
          int[] pixels = DrawingView.nativeRegionPixels(0, y, width, 1);
          if (pixels == null || pixels.length != width)
            throw new IllegalStateException("Fila incompleta");
          System.arraycopy(pixels, 0, row, 0, width);
        });
  }
}
