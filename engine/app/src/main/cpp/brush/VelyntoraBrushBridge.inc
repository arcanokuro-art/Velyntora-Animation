#include "../../../../../tools/brush/velyntora_brush/runtime/TipBrushEngine.hpp"
namespace {std::unique_ptr<velyntora::TipBrushEngine> velyntoraBrush;}
extern "C" JNIEXPORT jboolean JNICALL Java_art_velyntora_core_DrawingView_nativeConfigureTipBrush(
 JNIEnv* env,jclass,jbyteArray tips,jbyteArray texture,jfloatArray parameters,jfloatArray sizeCurve,jfloatArray opacityCurve,jfloatArray flowCurve){
 std::lock_guard<std::mutex> lock(guard);
 velyntoraBrush.reset();if(!tips||!texture||!parameters||env->GetArrayLength(parameters)!=13)return JNI_FALSE;
 int n=env->GetArrayLength(tips);if(n<16384 || n>32*16384 || n%16384 || env->GetArrayLength(texture)!=16384)return JNI_FALSE;
 try{
  auto next=std::make_unique<velyntora::TipBrushEngine>();next->tips.resize(n);next->texture.resize(16384);
  env->GetByteArrayRegion(tips,0,n,reinterpret_cast<jbyte*>(next->tips.data()));env->GetByteArrayRegion(texture,0,16384,reinterpret_cast<jbyte*>(next->texture.data()));env->GetFloatArrayRegion(parameters,0,13,next->parameters.data());
  for(float f:next->parameters)if(!std::isfinite(f))return JNI_FALSE;
  jfloatArray arrays[]={sizeCurve,opacityCurve,flowCurve};
  for(int k=0;k<3;k++){if(!arrays[k])return JNI_FALSE;int count=env->GetArrayLength(arrays[k]);if(count<4 || count>128 || count%2)return JNI_FALSE;next->curves[k].resize(count);env->GetFloatArrayRegion(arrays[k],0,count,next->curves[k].data());for(float f:next->curves[k])if(!std::isfinite(f))return JNI_FALSE;}
  if(env->ExceptionCheck())return JNI_FALSE;velyntoraBrush=std::move(next);return JNI_TRUE;
 }catch(const std::exception&){return JNI_FALSE;}
}
extern "C" JNIEXPORT void JNICALL Java_art_velyntora_core_DrawingView_nativeTipBrushStroke(
 JNIEnv*,jclass,jfloat x0,jfloat y0,jfloat x1,jfloat y1,jfloat radius,jint color,jfloat opacity,jfloat pressure){
 std::lock_guard<std::mutex> lock(guard);if(!canvas||!velyntoraBrush)return;
 velyntoraBrush->stroke(*canvas,x0,y0,x1,y1,radius,std::uint32_t(color),opacity,pressure,brushMask.size()==canvas->pixels().size()?&brushMask:nullptr);
 storeStrokeRegion(x0,y0,x1,y1,radius*1.415f+1);
}
