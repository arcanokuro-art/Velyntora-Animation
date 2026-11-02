package app.velyntora.animation.video;
import android.graphics.*;import android.media.*;import java.io.*;
public final class YuvFrameConverter{
 static{System.loadLibrary("velyntora_jni");}
 static native void convert(Bitmap output,java.nio.ByteBuffer y,java.nio.ByteBuffer u,java.nio.ByteBuffer v,
  int yr,int ur,int vr,int yp,int up,int vp,int left,int top,int width,int height,int standard,int range)throws IOException;
 public static native void writePng(Bitmap bitmap,int fd)throws IOException;
 public static Bitmap bitmap(Image image,MediaFormat format)throws IOException{
  if(image==null||image.getFormat()!=ImageFormat.YUV_420_888)throw new IOException("El decodificador no entrega video SDR de 8 bits compatible sin conversión de calidad");
  int transfer=value(format,MediaFormat.KEY_COLOR_TRANSFER,MediaFormat.COLOR_TRANSFER_SDR_VIDEO);
  int standard=value(format,MediaFormat.KEY_COLOR_STANDARD,0);
  if(transfer==MediaFormat.COLOR_TRANSFER_ST2084||transfer==MediaFormat.COLOR_TRANSFER_HLG||standard==MediaFormat.COLOR_STANDARD_BT2020)
   throw new IOException("Este video HDR/BT.2020 necesita un flujo de alta profundidad de color; no se reducirá automáticamente a 8 bits");
  Rect crop=image.getCropRect();int w=crop.width(),h=crop.height();
  if(w<1||h<1||w>6000||h>6000)throw new IOException("El fotograma original supera el límite de 6000 píxeles");
  Bitmap bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
  Image.Plane[] p=image.getPlanes();
  try{
   convert(bitmap,p[0].getBuffer().slice(),p[1].getBuffer().slice(),p[2].getBuffer().slice(),
    p[0].getRowStride(),p[1].getRowStride(),p[2].getRowStride(),p[0].getPixelStride(),p[1].getPixelStride(),p[2].getPixelStride(),
    crop.left,crop.top,w,h,standard,value(format,MediaFormat.KEY_COLOR_RANGE,MediaFormat.COLOR_RANGE_LIMITED));
   return bitmap;
  }catch(IOException|RuntimeException ex){bitmap.recycle();throw new IOException("No se pudo convertir el color del fotograma",ex);}
 }
 private static int value(MediaFormat f,String key,int fallback){return f.containsKey(key)?f.getInteger(key):fallback;}
 private YuvFrameConverter(){}
}
