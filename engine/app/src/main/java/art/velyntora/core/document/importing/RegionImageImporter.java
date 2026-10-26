package art.velyntora.core;
import art.velyntora.core.document.CanvasLimits;
import android.graphics.*;
import android.net.Uri;
import java.io.*;

/** One bounded decoder region and JNI stripe at a time; original dimensions are retained. */
final class RegionImageImporter {
  static void load(MainActivity host,Uri uri) throws IOException {
    ImageImportSize size=probe(host,uri);
    if(size.reduced())throw new IOException("La imagen necesita confirmación para reducirse");
    load(host,uri,size);
  }
  static ImageImportSize probe(MainActivity host,Uri uri)throws IOException {
    int orientation=1;
    try(InputStream metadata=host.getContentResolver().openInputStream(uri)){
      if(metadata!=null)orientation=ExifOrientation.read(metadata);
    }catch(IOException ignored){}
    BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;
    try(InputStream input=host.getContentResolver().openInputStream(uri)){
      if(input==null)throw new IOException("No se puede leer la imagen");
      BitmapFactory.decodeStream(input,null,bounds);
    }
    if(bounds.outWidth<=0 || bounds.outHeight<=0)throw new IOException("Dimensiones inválidas");
    return new ImageImportSize(bounds.outWidth,bounds.outHeight,orientation);
  }
  static void load(MainActivity host,Uri uri,ImageImportSize size)throws IOException {
    int orientation=size.orientation;
    try(InputStream probe=host.getContentResolver().openInputStream(uri)){
      if(probe==null)throw new IOException("No se puede leer la imagen");
      BufferedInputStream buffered=new BufferedInputStream(probe);buffered.mark(8);
      boolean png=buffered.read()==137 && buffered.read()==80 && buffered.read()==78 && buffered.read()==71;
      buffered.reset();if(png && PngStripeImporter.load(buffered,orientation,size))return;
    }
    try(InputStream input=host.getContentResolver().openInputStream(uri)){
      if(input==null)throw new IOException("No se puede leer la imagen");
      BitmapRegionDecoder decoder;
      try{decoder=BitmapRegionDecoder.newInstance(input,false);}
      catch(IOException|RuntimeException unsupported){loadBitmapFallback(host,uri,size);return;}
      if(decoder==null){loadBitmapFallback(host,uri,size);return;}
      try {
        int width=decoder.getWidth(),height=decoder.getHeight();
        if(width!=size.sourceWidth || height!=size.sourceHeight)throw new IOException("La imagen cambió durante la importación");
        if(size.reduced()){loadReduced(decoder,size);return;}
        boolean rotated=orientation>=5;int outputWidth=rotated?height:width,outputHeight=rotated?width:height;
        if(!DrawingView.nativeBeginImageImport(outputWidth,outputHeight))throw new IOException("No se pudo preparar el documento");
        BitmapFactory.Options options=new BitmapFactory.Options();options.inPreferredConfig=Bitmap.Config.ARGB_8888;
        int rows=Math.max(1,Math.min(256,1048576/width));
        for(int top=0;top<height;top+=rows){
          int count=Math.min(rows,height-top);Bitmap decoded=decoder.decodeRegion(new Rect(0,top,width,top+count),options);
          if(decoded==null || decoded.getWidth()!=width || decoded.getHeight()!=count)throw new IOException("Región incompleta");
          int[] source=new int[width*count];try{decoded.getPixels(source,0,width,0,0,width,count);}finally{decoded.recycle();}
          ImageImportStripe.write(width,height,orientation,top,count,source);
        }
        if(!DrawingView.nativeFinishImageImport())throw new IOException("No se pudo instalar el documento");
      }finally{decoder.recycle();}
    }
  }
  /** Resample bounded output stripes, with overlap for filtering at stripe boundaries. */
  private static void loadReduced(BitmapRegionDecoder decoder,ImageImportSize size)throws IOException {
    if(!DrawingView.nativeBeginImageImport(size.displayWidth(),size.displayHeight()))
      throw new IOException("No se pudo preparar el documento");
    BitmapFactory.Options options=new BitmapFactory.Options();options.inPreferredConfig=Bitmap.Config.ARGB_8888;
    int sample=1;
    while(sample<=Integer.MAX_VALUE/2 && size.sourceWidth/(long)(sample*2)>=size.width
        && size.sourceHeight/(long)(sample*2)>=size.height)sample*=2;
    options.inSampleSize=sample;
    Paint paint=new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
    int rows=Math.max(1,Math.min(64,524288/size.width));
    for(int top=0;top<size.height;top+=rows){
      int count=Math.min(rows,size.height-top);
      double sy=size.sourceHeight/(double)size.height;
      int sourceTop=Math.max(0,(int)Math.floor(top*sy)-sample*2);
      int sourceBottom=Math.min(size.sourceHeight,(int)Math.ceil((top+count)*sy)+sample*2);
      // Align sampled decoding to a stable global grid.
      sourceTop=sourceTop/sample*sample;
      Bitmap decoded=decoder.decodeRegion(new Rect(0,sourceTop,size.sourceWidth,sourceBottom),options);
      if(decoded==null)throw new IOException("Región incompleta");
      Bitmap stripe=null;
      try {
        stripe=Bitmap.createBitmap(size.width,count,Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(stripe);
        canvas.drawBitmap(decoded,null,new RectF(0,(float)(sourceTop/sy-top),size.width,
            (float)(sourceBottom/sy-top)),paint);
        int[] pixels=new int[size.width*count];stripe.getPixels(pixels,0,size.width,0,0,size.width,count);
        ImageImportStripe.write(size.width,size.height,size.orientation,top,count,pixels);
      }finally{decoded.recycle();if(stripe!=null)stripe.recycle();}
    }
    if(!DrawingView.nativeFinishImageImport())throw new IOException("No se pudo instalar el documento");
  }
  private static void loadBitmapFallback(MainActivity host,Uri uri,ImageImportSize size)throws IOException{
    int orientation=size.orientation;
    BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;
    try(InputStream source=host.getContentResolver().openInputStream(uri)){BitmapFactory.decodeStream(source,null,bounds);}
    int width=bounds.outWidth,height=bounds.outHeight;
    if(width!=size.sourceWidth || height!=size.sourceHeight || (!size.reduced() && (!CanvasLimits.accepts(width,height) || (long)width*height>16000000)))
      throw new IOException("El formato no admite lectura por regiones");
    BitmapFactory.Options options=new BitmapFactory.Options();options.inPreferredConfig=Bitmap.Config.ARGB_8888;
    options.inSampleSize=1;
    if(size.reduced())while(((long)width+options.inSampleSize-1)/Math.max(1,options.inSampleSize)
        *((long)height+options.inSampleSize-1)/Math.max(1,options.inSampleSize)>4000000L){
      options.inSampleSize=Math.max(2,options.inSampleSize*2);
    }
    Bitmap image;
    try(InputStream source=host.getContentResolver().openInputStream(uri)){image=BitmapFactory.decodeStream(source,null,options);}
    if(image==null)throw new IOException("Imagen no compatible");
    try{
      if(size.reduced()){
        if(!DrawingView.nativeBeginImageImport(size.displayWidth(),size.displayHeight()))throw new IOException("No se pudo preparar el documento");
        int rows=Math.max(1,Math.min(64,524288/size.width));Paint paint=new Paint(Paint.FILTER_BITMAP_FLAG);
        for(int top=0;top<size.height;top+=rows){
          int count=Math.min(rows,size.height-top);Bitmap stripe=Bitmap.createBitmap(size.width,count,Bitmap.Config.ARGB_8888);
          try{
            new Canvas(stripe).drawBitmap(image,null,new RectF(0,-top,size.width,size.height-top),paint);
            int[] pixels=new int[size.width*count];stripe.getPixels(pixels,0,size.width,0,0,size.width,count);
            ImageImportStripe.write(size.width,size.height,orientation,top,count,pixels);
          }finally{stripe.recycle();}
        }
        if(!DrawingView.nativeFinishImageImport())throw new IOException("No se pudo instalar el documento");return;
      }
      boolean rotated=orientation>=5;
      if(!DrawingView.nativeBeginImageImport(rotated?height:width,rotated?width:height))throw new IOException("No se pudo preparar el documento");
      int rows=Math.max(1,Math.min(256,1048576/width));
      for(int y=0;y<height;y+=rows){int n=Math.min(rows,height-y);int[] stripe=new int[width*n];
        image.getPixels(stripe,0,width,0,y,width,n);ImageImportStripe.write(width,height,orientation,y,n,stripe);
      }
      if(!DrawingView.nativeFinishImageImport())throw new IOException("No se pudo instalar el documento");
    }finally{image.recycle();}
  }
  private RegionImageImporter(){}
}
