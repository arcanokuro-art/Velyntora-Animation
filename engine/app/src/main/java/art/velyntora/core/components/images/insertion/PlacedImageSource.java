package art.velyntora.core;

import android.graphics.*;
import android.net.Uri;
import java.io.*;

/** Private source file, bounded preview and region decoding for full-resolution tiles. */
final class PlacedImageSource implements AutoCloseable {
  final File file;final ImageImportSize size;final Bitmap preview;final String name;
  final Matrix rawToLogical;
  private PlacedImageSource(File file,ImageImportSize size,Bitmap preview,String name,Matrix matrix){this.file=file;this.size=size;this.preview=preview;this.name=name;this.rawToLogical=matrix;}
  static Matrix orientation(ImageImportSize s){
    float w=s.width,h=s.height;float[] src={0,0,s.sourceWidth,0,0,s.sourceHeight};float[] dst;
    switch(s.orientation){
      case 2:dst=new float[]{w,0,0,0,w,h};break;
      case 3:dst=new float[]{w,h,0,h,w,0};break;
      case 4:dst=new float[]{0,h,w,h,0,0};break;
      case 5:dst=new float[]{0,0,0,w,h,0};break;
      case 6:dst=new float[]{h,0,h,w,0,0};break;
      case 7:dst=new float[]{h,w,h,0,0,w};break;
      case 8:dst=new float[]{0,w,0,0,h,w};break;
      default:dst=new float[]{0,0,w,0,0,h};
    }
    Matrix m=new Matrix();m.setPolyToPoly(src,0,dst,0,3);return m;
  }
  static PlacedImageSource load(MainActivity host,Uri uri)throws IOException {
    File file=File.createTempFile("placed-image-",".source",host.getCacheDir());Bitmap preview=null;
    try{
      try(InputStream in=host.getContentResolver().openInputStream(uri);OutputStream out=new FileOutputStream(file)){
        if(in==null)throw new IOException("No se puede leer la imagen");byte[] buffer=new byte[65536];int count;long total=0;while((count=in.read(buffer))!=-1){total+=count;if(total>256L*1024*1024)throw new IOException("Imagen mayor al límite de archivo");out.write(buffer,0,count);}
      }
      ImageImportSize size=RegionImageImporter.probe(host,Uri.fromFile(file));
      // Validate region decoding before opening an editable session.
      BitmapRegionDecoder decoder=BitmapRegionDecoder.newInstance(file.getPath(),false);
      if(decoder==null)throw new IOException("Usa una imagen PNG, JPEG o WebP");decoder.recycle();
      BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=1;options.inPreferredConfig=Bitmap.Config.ARGB_8888;
      while((long)((size.sourceWidth+options.inSampleSize-1)/options.inSampleSize)*((size.sourceHeight+options.inSampleSize-1)/options.inSampleSize)>1048576)options.inSampleSize*=2;
      Bitmap raw=BitmapFactory.decodeFile(file.getPath(),options);if(raw==null)throw new IOException("Imagen no compatible");
      Matrix orientation=orientation(size);
      float scale=Math.min(1,768f/Math.max(size.displayWidth(),size.displayHeight()));
      int pw=Math.max(1,Math.round(size.displayWidth()*scale)),ph=Math.max(1,Math.round(size.displayHeight()*scale));
      try{
        preview=Bitmap.createBitmap(pw,ph,Bitmap.Config.ARGB_8888);Matrix m=new Matrix(orientation);
        m.preScale(size.sourceWidth/(float)raw.getWidth(),size.sourceHeight/(float)raw.getHeight());
        m.postScale(pw/(float)size.displayWidth(),ph/(float)size.displayHeight());
        new Canvas(preview).drawBitmap(raw,m,new Paint(Paint.FILTER_BITMAP_FLAG));
      }finally{raw.recycle();}
      String name=host.fileName(uri);if(name==null)name="Imagen";if(name.length()>200)name=name.substring(0,200);
      return new PlacedImageSource(file,size,preview,name,orientation);
    }catch(IOException|RuntimeException|OutOfMemoryError error){file.delete();if(preview!=null)preview.recycle();throw error;}
  }
  Bitmap tile(BitmapRegionDecoder decoder,PlacedImageTransform transform,Rect tile)throws IOException {
    Matrix rawToDocument=new Matrix();rawToDocument.setConcat(transform.matrix(),rawToLogical);
    Matrix inverse=new Matrix();if(!rawToDocument.invert(inverse))throw new IOException("Transformación inválida");
    RectF needed=new RectF(tile);inverse.mapRect(needed);
    int left=Math.max(0,(int)Math.floor(needed.left)-4),top=Math.max(0,(int)Math.floor(needed.top)-4);
    int right=Math.min(size.sourceWidth,(int)Math.ceil(needed.right)+4),bottom=Math.min(size.sourceHeight,(int)Math.ceil(needed.bottom)+4);
    Bitmap result=Bitmap.createBitmap(tile.width(),tile.height(),Bitmap.Config.ARGB_8888);
    if(left>=right || top>=bottom)return result;
    int sample=1;
    while((long)((right-left+sample-1)/sample)*((bottom-top+sample-1)/sample)>1048576)sample*=2;
    left=Math.max(0,(left/sample-2)*sample);top=Math.max(0,(top/sample-2)*sample);
    right=Math.min(size.sourceWidth,((right+sample-1)/sample+2)*sample);
    bottom=Math.min(size.sourceHeight,((bottom+sample-1)/sample+2)*sample);
    BitmapFactory.Options options=new BitmapFactory.Options();options.inPreferredConfig=Bitmap.Config.ARGB_8888;options.inSampleSize=sample;
    Bitmap region=null;
    try{
      region=decoder.decodeRegion(new Rect(left,top,right,bottom),options);if(region==null)throw new IOException("No se pudo decodificar la región");
      Matrix local=new Matrix();local.setScale((right-left)/(float)region.getWidth(),(bottom-top)/(float)region.getHeight());local.postTranslate(left,top);local.postConcat(rawToDocument);
      Canvas canvas=new Canvas(result);canvas.translate(-tile.left,-tile.top);
      Path clip=new Path();clip.addRect(transform.crop,Path.Direction.CW);clip.transform(transform.matrix());canvas.clipPath(clip);
      canvas.drawBitmap(region,local,new Paint(Paint.FILTER_BITMAP_FLAG));return result;
    }catch(IOException|RuntimeException|OutOfMemoryError error){result.recycle();throw error;}
    finally{if(region!=null)region.recycle();}
  }
  public void close(){file.delete();/* Render-thread bitmap references are released by GC. */}
}
