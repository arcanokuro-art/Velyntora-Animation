#include "TipBrushEngine.hpp"
#include <algorithm>
#include <cmath>
namespace velyntora {
namespace {
std::uint32_t mix(std::uint32_t a,std::uint32_t b,float t){
 float aa=(a>>24)/255.f,ba=(b>>24)/255.f,alpha=aa*(1-t)+ba*t;
 if(alpha<=0)return 0;
 std::uint32_t result=std::uint32_t(std::lround(alpha*255))<<24;
 for(int s:{0,8,16})result|=std::uint32_t(std::clamp(std::lround((((a>>s)&255)*aa*(1-t)+((b>>s)&255)*ba*t)/alpha),0L,255L))<<s;
 return result;
}
std::uint32_t over(std::uint32_t source,std::uint32_t dest,float coverage){
 float sa=(source>>24)/255.f*coverage,da=(dest>>24)/255.f,oa=sa+da*(1-sa);
 if(oa<=0)return 0;
 std::uint32_t result=std::uint32_t(std::lround(oa*255))<<24;
 for(int s:{0,8,16})result|=std::uint32_t(std::clamp(std::lround((((source>>s)&255)*sa+((dest>>s)&255)*da*(1-sa))/oa),0L,255L))<<s;
 return result;
}
}
float TipBrushEngine::curve(int index,float pressure) const{
 const auto& points=curves[index];
 if(points.size()<4)return pressure;
 if(pressure<=points[0])return points[1];
 for(std::size_t i=2;i+1<points.size();i+=2)if(pressure<=points[i]){
  float dx=points[i]-points[i-2];return dx>0?points[i-1]+(points[i+1]-points[i-1])*(pressure-points[i-2])/dx:points[i+1];
 }
 return points.back();
}
void TipBrushEngine::stroke(Canvas& canvas,float x0,float y0,float x1,float y1,float radius,std::uint32_t color,float opacity,float pressure,const std::vector<std::uint8_t>* mask){
 if(tips.empty() || tips.size()%16384 || !std::isfinite(x0)||!std::isfinite(y0)||!std::isfinite(x1)||!std::isfinite(y1)||!std::isfinite(radius)||!std::isfinite(opacity)||!std::isfinite(pressure)||radius<=0||opacity<=0)return;
 if(mask && mask->size()!=canvas.pixels().size())return;
 pressure=std::clamp(pressure,0.f,1.f);radius=std::clamp(radius,.5f,150.f);
 if(parameters[4]>0)radius*=std::max(.02f,curve(0,pressure));
 opacity=std::clamp(opacity*parameters[2]*(parameters[5]>0?curve(1,pressure):1.f),0.f,1.f);
 if(opacity<=0)return;
 float step=std::max(.5f,radius*2*std::clamp(parameters[0],.02f,2.f));
 float dx=x1-x0,dy=y1-y0,length=std::hypot(dx,dy);
 if(!started){started=true;dab(canvas,x0,y0,radius,color,opacity,pressure,mask);remaining=step;}
 if(length==0){if(parameters[11]>0)dab(canvas,x1,y1,radius,color,opacity,pressure,mask);return;}
 float distance=remaining;
 for(int count=0;distance<=length && count<24001;count++,distance+=step)
  dab(canvas,x0+dx*distance/length,y0+dy*distance/length,radius,color,opacity,pressure,mask);
 remaining=std::max(0.f,distance-length);
}
void TipBrushEngine::dab(Canvas& c,float cx,float cy,float radius,std::uint32_t color,float opacity,float pressure,const std::vector<std::uint8_t>* mask){
 if(c.strokeBase_.size()!=c.pixels_.size())c.beginOpacityStroke();
 float angle=parameters[1],co=std::cos(angle),si=std::sin(angle),extent=radius*(std::abs(co)+std::abs(si))+1;
 int left=int(std::clamp(std::floor(cx-extent),0.f,float(c.width_))),right=int(std::clamp(std::ceil(cx+extent),0.f,float(c.width_)));
 int top=int(std::clamp(std::floor(cy-extent),0.f,float(c.height_))),bottom=int(std::clamp(std::ceil(cy+extent),0.f,float(c.height_)));
 auto sample=[&](float x,float y)->float{
  float dx=(x-cx)/radius,dy=(y-cy)/radius;
  float u=(dx*co+dy*si+1)*64-.5f,v=(-dx*si+dy*co+1)*64-.5f;
  if(u<0||v<0||u>127||v>127)return 0;
  int ix=int(u),iy=int(v),ix1=std::min(127,ix+1),iy1=std::min(127,iy+1);float fx=u-ix,fy=v-iy;
  std::size_t frame=(dabIndex%(tips.size()/16384))*16384;
  return ((tips[frame+iy*128+ix]*(1-fx)+tips[frame+iy*128+ix1]*fx)*(1-fy)+(tips[frame+iy1*128+ix]*(1-fx)+tips[frame+iy1*128+ix1]*fx)*fy)/255.f;
 };
 if(parameters[7]>0){
  double weights=0,alpha=0,red=0,green=0,blue=0;int step=std::max(1,int(radius/5));
  for(int y=top;y<bottom;y+=step)for(int x=left;x<right;x+=step){
   auto i=std::size_t(y)*c.width_+x;if(mask && !(*mask)[i])continue;
   double w=sample(x+.5f,y+.5f);auto pixel=c.pixels_[i];double a=(pixel>>24)/255.;weights+=w;alpha+=a*w;red+=((pixel>>16)&255)*a*w;green+=((pixel>>8)&255)*a*w;blue+=(pixel&255)*a*w;
  }
  std::uint32_t under=0;
  if(alpha>0 && weights>0)under=(std::uint32_t(std::lround(alpha/weights*255))<<24)|(std::uint32_t(std::lround(red/alpha))<<16)|(std::uint32_t(std::lround(green/alpha))<<8)|std::uint32_t(std::lround(blue/alpha));
  if(!hasCarry){carry=under;hasCarry=true;}else carry=mix(carry,under,.12f*std::clamp(parameters[9],0.f,1.f));
  color=mix(carry,color,std::clamp(parameters[8]*pressure,0.f,1.f));
 }
 float flow=std::clamp(parameters[3]*(parameters[6]>0?curve(2,pressure):1.f),0.f,1.f);
 if(parameters[11]>0)flow*=.08f;
 for(int y=top;y<bottom;y++)for(int x=left;x<right;x++){
  auto i=std::size_t(y)*c.width_+x;if(mask && !(*mask)[i])continue;
  float weight=radius<1.5f?(sample(x+.25f,y+.25f)+sample(x+.75f,y+.25f)+sample(x+.25f,y+.75f)+sample(x+.75f,y+.75f))*.25f:sample(x+.5f,y+.5f);
  if(texture.size()==16384){float grain=texture[(y%128)*128+x%128]/255.f;weight*=1-std::clamp(parameters[10],0.f,1.f)*(1-grain);}
  unsigned hash=unsigned(x)*73856093u^unsigned(y)*19349663u;
  if((hash%1000)/1000.f>parameters[12])continue;
  float amount=std::clamp(weight*flow,0.f,1.f);if(amount<=0)continue;
  float& coverage=c.strokeCoverage_[i];coverage=coverage+(1-coverage)*amount;
  c.pixels_[i]=over(color,c.strokeBase_[i],opacity*coverage);
 }
 dabIndex++;
}
}
