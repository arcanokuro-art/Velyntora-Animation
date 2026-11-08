package app.velyntora.animation.video;
import android.content.Context;import android.graphics.*;import android.media.*;import java.io.*;import java.nio.ByteBuffer;
/** One initial seek followed by sequential decode; only sampled frames are converted to RGB. */
public final class VideoFrameDecoder implements AutoCloseable {
 private final MediaExtractor extractor=new MediaExtractor();private MediaCodec codec;private MediaFormat format;
 private final VideoImportRequest request;private final int rotation;
 private boolean initialized,inputEnd,outputEnd,closed;private long lastTarget=-1;
 private Frame previous,next;private Bitmap cached;private long cachedTime=Long.MIN_VALUE;
 public int seekCount,decodedFrames,convertedFrames;
 private static final class Frame{final int slot;final long pts;final MediaFormat format;Frame(int slot,long pts,MediaFormat f){this.slot=slot;this.pts=pts;format=f;}}
 public VideoFrameDecoder(Context context,VideoImportRequest request)throws IOException{
  this.request=request;int angle=0;
  try{
   extractor.setDataSource(context,request.uri,null);int track=-1;
   for(int i=0;i<extractor.getTrackCount();i++){MediaFormat f=extractor.getTrackFormat(i);String mime=f.getString(MediaFormat.KEY_MIME);
    if(mime!=null&&mime.startsWith("video/")){track=i;format=f;break;}}
   if(track<0)throw new IOException("El archivo no contiene video");
   if(format.containsKey(MediaFormat.KEY_ROTATION))angle=format.getInteger(MediaFormat.KEY_ROTATION);
   int transfer=format.containsKey(MediaFormat.KEY_COLOR_TRANSFER)?format.getInteger(MediaFormat.KEY_COLOR_TRANSFER):0;
   int standard=format.containsKey(MediaFormat.KEY_COLOR_STANDARD)?format.getInteger(MediaFormat.KEY_COLOR_STANDARD):0;
   if(transfer==MediaFormat.COLOR_TRANSFER_ST2084||transfer==MediaFormat.COLOR_TRANSFER_HLG||standard==MediaFormat.COLOR_STANDARD_BT2020)
    throw new IOException("El video HDR/BT.2020 no se reducirá automáticamente a 8 bits");
   extractor.selectTrack(track);format.setInteger(MediaFormat.KEY_COLOR_FORMAT,MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
   // Rotation is applied explicitly after decoding, preserving dimensions and pixel values.
   format.setInteger(MediaFormat.KEY_ROTATION,0);
   codec=MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME));codec.configure(format,null,null,0);codec.start();
  }catch(Exception error){close();if(error instanceof IOException)throw (IOException)error;throw new IOException("No se pudo abrir el decodificador secuencial",error);}
  rotation=angle;
 }
 private void release(Frame frame){if(frame!=null)codec.releaseOutputBuffer(frame.slot,false);}
 private Frame decode()throws IOException{
  if(outputEnd)return null;
  MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();long deadline=android.os.SystemClock.elapsedRealtime()+30000;
  while(!outputEnd){
   if(!inputEnd){int slot=codec.dequeueInputBuffer(10000);if(slot>=0){
    ByteBuffer input=codec.getInputBuffer(slot);if(input==null)throw new IOException("Entrada de video incompleta");
    input.clear();int size=extractor.readSampleData(input,0);
    if(size<0){codec.queueInputBuffer(slot,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);inputEnd=true;}
    else{codec.queueInputBuffer(slot,0,size,extractor.getSampleTime(),0);extractor.advance();}
   }}
   int slot=codec.dequeueOutputBuffer(info,10000);
   if(slot==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED)format=codec.getOutputFormat();
   else if(slot>=0){
    outputEnd=(info.flags&MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0;
    if(info.size>0&&(info.flags&MediaCodec.BUFFER_FLAG_CODEC_CONFIG)==0){decodedFrames++;return new Frame(slot,info.presentationTimeUs,codec.getOutputFormat(slot));}
    codec.releaseOutputBuffer(slot,false);
   }
   if(android.os.SystemClock.elapsedRealtime()>deadline)throw new IOException("El decodificador dejó de responder");
  }return null;
 }
 public Bitmap frame(int index)throws IOException{
  long target=request.timestampUs(index);
  if(closed||target<lastTarget)throw new IOException("Los fotogramas deben solicitarse en orden de tiempo");
  lastTarget=target;
  try{
   if(!initialized){extractor.seekTo(target,MediaExtractor.SEEK_TO_PREVIOUS_SYNC);seekCount++;initialized=true;next=decode();}
   while(next!=null&&next.pts<target){release(previous);previous=next;next=decode();}
   Frame selected=previous==null?next:next==null?previous:
     target-previous.pts<=next.pts-target?previous:next;
   if(selected==null)throw new IOException("El video no contiene fotogramas decodificables");
   if(cached==null||cachedTime!=selected.pts){
    if(cached!=null){cached.recycle();cached=null;}
    Image image=codec.getOutputImage(selected.slot);Bitmap raw;
    try{raw=YuvFrameConverter.bitmap(image,selected.format);}finally{if(image!=null)image.close();}
    if(rotation%360!=0){Matrix m=new Matrix();m.postRotate(rotation);Bitmap rotated=Bitmap.createBitmap(raw,0,0,raw.getWidth(),raw.getHeight(),m,false);if(rotated!=raw)raw.recycle();raw=rotated;}
    cached=raw;cachedTime=selected.pts;convertedFrames++;
   }
   if(cached.getWidth()==request.width&&cached.getHeight()==request.height)return cached.copy(Bitmap.Config.ARGB_8888,false);
   Bitmap output=Bitmap.createBitmap(request.width,request.height,Bitmap.Config.ARGB_8888);
   Canvas canvas=new Canvas(output);canvas.drawColor(Color.BLACK);
   float scale=Math.min(request.width/(float)cached.getWidth(),request.height/(float)cached.getHeight());
   float w=cached.getWidth()*scale,h=cached.getHeight()*scale;
   canvas.drawBitmap(cached,null,new RectF((request.width-w)/2,(request.height-h)/2,(request.width+w)/2,(request.height+h)/2),new Paint(Paint.FILTER_BITMAP_FLAG));
   return output;
  }catch(IOException ex){throw ex;}catch(RuntimeException ex){throw new IOException("No se pudo decodificar el fotograma "+(index+1),ex);}
 }
 public void close(){
  if(closed)return;closed=true;
  if(cached!=null){cached.recycle();cached=null;}
  if(codec!=null){try{release(previous);release(next);}catch(Exception ignored){}
   try{codec.stop();}catch(Exception ignored){}try{codec.release();}catch(Exception ignored){}}
  try{extractor.release();}catch(Exception ignored){}
 }
}
