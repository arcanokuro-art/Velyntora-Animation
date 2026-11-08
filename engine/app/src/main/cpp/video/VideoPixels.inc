// Video-only pixel conversion and fast lossless PNG. Does not mutate the drawing engine.
#include <android/bitmap.h>
#include <zlib.h>
#include <array>
namespace {
void videoError(JNIEnv* env,const char* text){env->ThrowNew(env->FindClass("java/io/IOException"),text);}
bool videoWrite(int fd,const void* pointer,std::size_t size){
 const unsigned char* at=static_cast<const unsigned char*>(pointer);
 while(size){ssize_t n=::write(fd,at,size);if(n<0&&errno==EINTR)continue;if(n<=0)return false;at+=n;size-=n;}return true;
}
void videoBe(unsigned char* out,std::uint32_t n){out[0]=n>>24;out[1]=n>>16;out[2]=n>>8;out[3]=n;}
bool videoChunk(int fd,const char* type,const unsigned char* data,std::size_t size){
 unsigned char n[4];videoBe(n,size);
 uLong crc=crc32(0,reinterpret_cast<const Bytef*>(type),4);if(size)crc=crc32(crc,data,size);
 return videoWrite(fd,n,4)&&videoWrite(fd,type,4)&&(!size||videoWrite(fd,data,size))&&(videoBe(n,crc),videoWrite(fd,n,4));
}
}
extern "C" JNIEXPORT void JNICALL Java_app_velyntora_animation_video_YuvFrameConverter_convert(
 JNIEnv* env,jclass,jobject bitmap,jobject yy,jobject uu,jobject vv,jint yr,jint ur,jint vr,jint yp,jint up,jint vp,
 jint left,jint top,jint width,jint height,jint standard,jint range){
 AndroidBitmapInfo info{};void* pixels=nullptr;
 if(AndroidBitmap_getInfo(env,bitmap,&info)!=0||info.format!=ANDROID_BITMAP_FORMAT_RGBA_8888||
   int(info.width)!=width||int(info.height)!=height||width<=0||height<=0||left<0||top<0||
   yr<=0||ur<=0||vr<=0||yp<=0||up<=0||vp<=0){videoError(env,"Fotograma YUV inválido");return;}
 auto y=static_cast<unsigned char*>(env->GetDirectBufferAddress(yy));
 auto u=static_cast<unsigned char*>(env->GetDirectBufferAddress(uu));
 auto v=static_cast<unsigned char*>(env->GetDirectBufferAddress(vv));
 auto fits=[&](jobject buffer,int row,int pixel,int x,int z){
  return std::int64_t(z)*row+std::int64_t(x)*pixel<env->GetDirectBufferCapacity(buffer);
 };
 if(!y||!u||!v||!fits(yy,yr,yp,left+width-1,top+height-1)||
   !fits(uu,ur,up,(left+width-1)/2,(top+height-1)/2)||!fits(vv,vr,vp,(left+width-1)/2,(top+height-1)/2)){
  videoError(env,"Planos YUV incompletos");return;
 }
 if(AndroidBitmap_lockPixels(env,bitmap,&pixels)!=0){videoError(env,"No se pudo acceder al bitmap");return;}
 const bool full=range==1;const bool hd=standard==1||(standard==0&&width>=1280);
 const double kr=hd?0.2126:0.299,kb=hd?0.0722:0.114,kg=1-kr-kb;
 const double chroma=full?1.0:255.0/224.0;
 const int sy=int((full?1.0:255.0/219.0)*65536+0.5);
 const int rv=int(2*(1-kr)*chroma*65536+0.5),bu=int(2*(1-kb)*chroma*65536+0.5);
 const int gu=int(2*kb*(1-kb)/kg*chroma*65536+0.5),gv=int(2*kr*(1-kr)/kg*chroma*65536+0.5);
 for(int row=0;row<height;row++){
  auto dst=static_cast<unsigned char*>(pixels)+row*info.stride;
  const int z=top+row;
  for(int col=0;col<width;col++){
   const int x=left+col,Y=(int(y[z*yr+x*yp])-(full?0:16))*sy,U=int(u[(z/2)*ur+(x/2)*up])-128,V=int(v[(z/2)*vr+(x/2)*vp])-128;
   dst[col*4]=std::clamp((Y+rv*V+32768)>>16,0,255);
   dst[col*4+1]=std::clamp((Y-gu*U-gv*V+32768)>>16,0,255);
   dst[col*4+2]=std::clamp((Y+bu*U+32768)>>16,0,255);dst[col*4+3]=255;
  }
 }
 AndroidBitmap_unlockPixels(env,bitmap);
}
extern "C" JNIEXPORT void JNICALL Java_app_velyntora_animation_video_YuvFrameConverter_writePng(
 JNIEnv* env,jclass,jobject bitmap,jint fd){
 AndroidBitmapInfo info{};void* pixels=nullptr;
 if(fd<0||AndroidBitmap_getInfo(env,bitmap,&info)!=0||info.format!=ANDROID_BITMAP_FORMAT_RGBA_8888||
   info.width>6000||info.height>6000||AndroidBitmap_lockPixels(env,bitmap,&pixels)!=0){videoError(env,"Bitmap PNG inválido");return;}
 z_stream zip{};bool initialized=false,ok=false;
 try{
  const unsigned char signature[]={137,80,78,71,13,10,26,10};unsigned char header[13]{};
  videoBe(header,info.width);videoBe(header+4,info.height);header[8]=8;header[9]=6;
  ok=videoWrite(fd,signature,8)&&videoChunk(fd,"IHDR",header,13);
  initialized=deflateInit(&zip,Z_BEST_SPEED)==Z_OK;ok=ok&&initialized;
  std::vector<unsigned char> row(info.width*4+1),rgba(info.width*4);std::array<unsigned char,65536> output{};
  for(unsigned y=0;ok&&y<info.height;y++){
   const auto src=static_cast<const unsigned char*>(pixels)+y*info.stride;
   for(unsigned x=0;x<info.width;x++){
    unsigned a=src[x*4+3];rgba[x*4+3]=a;
    for(unsigned c=0;c<3;c++)rgba[x*4+c]=a==255?src[x*4+c]:a?std::min(255u,(unsigned(src[x*4+c])*255+a/2)/a):0;
   }
   row[0]=1;for(unsigned x=0;x<info.width*4;x++)row[x+1]=rgba[x]-(x>=4?rgba[x-4]:0);
   zip.next_in=row.data();zip.avail_in=row.size();
   do{
    zip.next_out=output.data();zip.avail_out=output.size();int status=deflate(&zip,Z_NO_FLUSH);
    if(status!=Z_OK){ok=false;break;}
    std::size_t count=output.size()-zip.avail_out;if(count)ok=videoChunk(fd,"IDAT",output.data(),count);
   }while(ok&&zip.avail_in);
  }
  if(ok){
   int status;
   do{zip.next_out=output.data();zip.avail_out=output.size();status=deflate(&zip,Z_FINISH);
    std::size_t count=output.size()-zip.avail_out;if(count)ok=videoChunk(fd,"IDAT",output.data(),count);
   }while(ok&&status==Z_OK);
   ok=ok&&status==Z_STREAM_END&&videoChunk(fd,"IEND",nullptr,0);
  }
 }catch(const std::exception&){ok=false;}
 if(initialized)deflateEnd(&zip);AndroidBitmap_unlockPixels(env,bitmap);
 if(!ok)videoError(env,"No se pudo guardar el fotograma PNG");
}
