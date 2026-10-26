package art.velyntora.core;
import android.content.Context;
import ai.onnxruntime.*;
import java.io.*;
import java.nio.*;
import java.security.MessageDigest;
import java.util.*;
/** Official MI-GAN Places2-512 uint8 NCHW pipeline, CPU/offline. */
final class RemoveAiBackend implements AutoCloseable {
 static final String HASH="6f1f3530a1a2324b19752018ce756088b07973cda8d7d890034ace5c8a48c40b";
 private static final Object INFERENCE_LOCK=new Object();
 private OrtSession session;
 private OrtEnvironment environment;
 private void load(Context context)throws Exception{
  if(session!=null)return;
  environment=OrtEnvironment.getEnvironment();
  File model=new File(context.getNoBackupFilesDir(),"migan-"+HASH+".onnx");
  if(!model.exists()||!HASH.equals(digest(new FileInputStream(model)))){
   File temporary=new File(model.getPath()+".tmp");
   try(InputStream in=context.getAssets().open("remove_ai/migan_pipeline_v2.onnx");OutputStream out=new FileOutputStream(temporary)){
    byte[] buffer=new byte[65536];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);
   }
   if(!HASH.equals(digest(new FileInputStream(temporary)))){temporary.delete();throw new IOException("El modelo MI-GAN no pasó la verificación");}
   if(model.exists()&&!model.delete())throw new IOException("No se puede sustituir el modelo");
   if(!temporary.renameTo(model))throw new IOException("No se puede instalar el modelo");
  }
  try(OrtSession.SessionOptions options=new OrtSession.SessionOptions()){
   options.setIntraOpNumThreads(Math.max(1,Math.min(2,Runtime.getRuntime().availableProcessors())));
   options.setInterOpNumThreads(1);
   // Dynamic pipeline shapes otherwise retain a growing arena across repeated runs.
   options.setCPUArenaAllocator(false);
   options.setMemoryPatternOptimization(false);
   options.addConfigEntry("session.intra_op.allow_spinning","0");
   session=environment.createSession(model.getPath(),options);
  }
 }
 private static String digest(InputStream source)throws Exception{
  try(InputStream in=source){MessageDigest hash=MessageDigest.getInstance("SHA-256");byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)hash.update(b,0,n);StringBuilder s=new StringBuilder();for(byte v:hash.digest())s.append(String.format(Locale.ROOT,"%02x",v&255));return s.toString();}
 }
 synchronized int[] run(Context context,int[] source,byte[] removal,int width,int height)throws Exception{
  synchronized(INFERENCE_LOCK){return runExclusive(context,source,removal,width,height);}
 }
 private int[] runExclusive(Context context,int[] source,byte[] removal,int width,int height)throws Exception{
  int count=width*height;
  if(count<=0||source.length!=count||removal.length!=count)throw new IllegalArgumentException("Dimensiones inválidas");
  boolean any=false,known=false;for(byte b:removal){any|=b!=0;known|=b==0;}
  if(!any||!known)throw new IllegalArgumentException("Marca una zona y conserva parte del fondo alrededor");
  load(context);
  ByteBuffer rgb=ByteBuffer.allocateDirect(count*3),mask=ByteBuffer.allocateDirect(count);
  for(int channel=0;channel<3;channel++)for(int pixel:source)rgb.put((byte)(pixel>>(16-channel*8)));
  for(byte b:removal)mask.put(b==0?(byte)255:0);
  rgb.rewind();mask.rewind();
  try(OnnxTensor image=OnnxTensor.createTensor(environment,rgb,new long[]{1,3,height,width},OnnxJavaType.UINT8);
      OnnxTensor region=OnnxTensor.createTensor(environment,mask,new long[]{1,1,height,width},OnnxJavaType.UINT8)){
   Map<String,OnnxTensor> inputs=new HashMap<>();inputs.put("image",image);inputs.put("mask",region);
   try(OrtSession.Result result=session.run(inputs)){
    OnnxTensor tensor=(OnnxTensor)result.get(0);long[] shape=tensor.getInfo().getShape();
    if(shape.length!=4||shape[0]!=1||shape[1]!=3||shape[2]!=height||shape[3]!=width)throw new IOException("Salida MI-GAN incompatible");
    ByteBuffer output=tensor.getByteBuffer();
    if(output.remaining()!=count*3)throw new IOException("Salida MI-GAN incompleta");
    int[] composed=source.clone();
    for(int i=0;i<count;i++)if(removal[i]!=0)composed[i]=(source[i]&0xff000000)|((output.get(i)&255)<<16)|((output.get(count+i)&255)<<8)|(output.get(count*2+i)&255);
    return composed;
   }
  }
 }
 public synchronized void close(){if(session!=null){try{session.close();}catch(OrtException ignored){}session=null;}}
}
