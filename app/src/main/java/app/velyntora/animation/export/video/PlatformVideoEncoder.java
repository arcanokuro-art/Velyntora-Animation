package app.velyntora.animation.export.video;
import android.graphics.Bitmap;import android.media.*;import app.velyntora.animation.export.*;
import java.io.*;import java.nio.*;
/** Android codec pipeline shared by separately registered container modules. */
public final class PlatformVideoEncoder {
 private static int clip(int value){return Math.max(0,Math.min(255,value));}
 private static void plane(Image.Plane plane,int x,int y,int value){
  ByteBuffer b=plane.getBuffer();b.put(b.position()+y*plane.getRowStride()+x*plane.getPixelStride(),(byte)value);
 }
 private static void yuv(Image image,int[] rgb,int w,int h)throws IOException{
  if(image==null)throw new IOException("El codificador no permite entrada de imágenes YUV");
  Image.Plane[] p=image.getPlanes();
  for(int y=0;y<h;y++)for(int x=0;x<w;x++){
   int c=rgb[y*w+x],r=c>>16&255,g=c>>8&255,b=c&255;
   plane(p[0],x,y,clip(((66*r+129*g+25*b+128)>>8)+16));
  }
  for(int y=0;y<h;y+=2)for(int x=0;x<w;x+=2){
   int r=0,g=0,b=0;
   for(int dy=0;dy<2;dy++)for(int dx=0;dx<2;dx++){int c=rgb[(y+dy)*w+x+dx];r+=c>>16&255;g+=c>>8&255;b+=c&255;}
   r/=4;g/=4;b/=4;
   plane(p[1],x/2,y/2,clip(((-38*r-74*g+112*b+128)>>8)+128));
   plane(p[2],x/2,y/2,clip(((112*r-94*g-18*b+128)>>8)+128));
  }
 }
 public static void write(AnimationFrames frames,ExportRequest request,File file,String mime,int container)throws IOException{
  MediaFormat format=MediaFormat.createVideoFormat(mime,request.width,request.height);
  format.setInteger(MediaFormat.KEY_COLOR_FORMAT,MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
  format.setInteger(MediaFormat.KEY_BIT_RATE,request.bitrate);format.setInteger(MediaFormat.KEY_FRAME_RATE,frames.fps());
  format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,1);
  String name=new MediaCodecList(MediaCodecList.REGULAR_CODECS).findEncoderForFormat(format);
  if(name==null)throw new IOException("El dispositivo no puede exportar ese formato a esta resolución. Prueba una resolución menor.");
  MediaCodec codec=null;MediaMuxer muxer=null;boolean started=false,codecStarted=false;
  try{
   codec=MediaCodec.createByCodecName(name);codec.configure(format,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);
   muxer=new MediaMuxer(file.getAbsolutePath(),container);codec.start();codecStarted=true;
   int tick=0,frame=0,left=frames.exposure(0),track=-1;boolean sentEnd=false,finished=false;
   int[] rgb=new int[request.width*request.height];
   MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();long lastProgress=android.os.SystemClock.uptimeMillis();
   while(!finished){
    frames.check();
    if(!sentEnd){
     int slot=codec.dequeueInputBuffer(10000);
     if(slot>=0){
      if(tick==frames.ticks()){
       codec.queueInputBuffer(slot,0,0,tick*1000000L/frames.fps(),MediaCodec.BUFFER_FLAG_END_OF_STREAM);sentEnd=true;
      }else{
       if(left==frames.exposure(frame)){
        Bitmap bitmap=frames.render(frame,request.width,request.height,true);
        try{bitmap.getPixels(rgb,0,request.width,0,0,request.width,request.height);}finally{bitmap.recycle();}
       }
       Image image=codec.getInputImage(slot);
       try{yuv(image,rgb,request.width,request.height);}finally{if(image!=null)image.close();}
       codec.queueInputBuffer(slot,0,rgb.length*3/2,tick*1000000L/frames.fps(),0);
       tick++;left--;if(left==0&&frame+1<frames.count()){frame++;left=frames.exposure(frame);}
      }
      lastProgress=android.os.SystemClock.uptimeMillis();
     }
    }
    int output=codec.dequeueOutputBuffer(info,10000);
    if(output==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
     if(started)throw new IOException("El codificador cambió de formato inesperadamente");
     track=muxer.addTrack(codec.getOutputFormat());muxer.start();started=true;
    }else if(output>=0){
     ByteBuffer data=codec.getOutputBuffer(output);
     if((info.flags&MediaCodec.BUFFER_FLAG_CODEC_CONFIG)==0&&info.size>0){
      if(!started||data==null)throw new IOException("Salida de video incompleta");
      data.position(info.offset);data.limit(info.offset+info.size);muxer.writeSampleData(track,data,info);
     }
     finished=(info.flags&MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0;codec.releaseOutputBuffer(output,false);
     lastProgress=android.os.SystemClock.uptimeMillis();
    }
    if(android.os.SystemClock.uptimeMillis()-lastProgress>30000)throw new IOException("El codificador de video dejó de responder");
   }
   if(!started)throw new IOException("No se generó video");
   muxer.stop();started=false;
  }catch(java.util.concurrent.CancellationException e){throw e;}
   catch(RuntimeException e){throw new IOException("No se pudo codificar el video: "+e.getMessage(),e);}
  finally{
   if(codec!=null){if(codecStarted)try{codec.stop();}catch(Exception ignored){}codec.release();}
   if(muxer!=null){if(started)try{muxer.stop();}catch(Exception ignored){}muxer.release();}
  }
 }
 private PlatformVideoEncoder(){}
}
