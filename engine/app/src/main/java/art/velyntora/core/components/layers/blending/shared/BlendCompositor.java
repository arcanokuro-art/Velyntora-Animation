package art.velyntora.core;
final class BlendCompositor {
 static int composite(int src,int dst,float opacity,int mode){
  double sa=(src>>>24)/255.*opacity,da=(dst>>>24)/255.,oa=sa+da*(1-sa);if(oa<=0)return 0;
  if(sa<=0)return da>0?dst:0;
  if(mode==0&&sa>=1)return src;
  if(da<=0)return ((int)Math.round(sa*255)<<24)|(src&0xffffff);
  double[] a=new double[3],b=new double[3],mix=new double[3];for(int c=0;c<3;c++){a[c]=((src>>>(16-8*c))&255)/255.;b[c]=((dst>>>(16-8*c))&255)/255.;}
  switch(mode){
case 0:mix=Blend0.apply(a,b);break;
case 1:mix=Blend1.apply(a,b);break;
case 2:mix=Blend2.apply(a,b);break;
case 3:mix=Blend3.apply(a,b);break;
case 4:mix=Blend4.apply(a,b);break;
case 5:mix=Blend5.apply(a,b);break;
case 6:mix=Blend6.apply(a,b);break;
case 7:mix=Blend7.apply(a,b);break;
case 8:mix=Blend8.apply(a,b);break;
case 9:mix=Blend9.apply(a,b);break;
case 10:mix=Blend10.apply(a,b);break;
case 11:mix=Blend11.apply(a,b);break;
case 12:mix=Blend12.apply(a,b);break;
case 13:mix=Blend13.apply(a,b);break;
case 14:mix=Blend14.apply(a,b);break;
case 15:mix=Blend15.apply(a,b);break;
default:throw new IllegalArgumentException("Invalid blend mode");}
  int out=(int)Math.round(oa*255)<<24;for(int c=0;c<3;c++)out|=Math.max(0,Math.min(255,(int)Math.round((sa*(1-da)*a[c]+sa*da*mix[c]+(1-sa)*da*b[c])/oa*255)))<<(16-8*c);return out;
 }
 static double lum(double[] c){return .3*c[0]+.59*c[1]+.11*c[2];}
 static double sat(double[] c){return Math.max(c[0],Math.max(c[1],c[2]))-Math.min(c[0],Math.min(c[1],c[2]));}
 static double[] setLum(double[] input,double l){double[] c=input.clone();double delta=l-lum(c);for(int i=0;i<3;i++)c[i]+=delta;double lo=Math.min(c[0],Math.min(c[1],c[2])),hi=Math.max(c[0],Math.max(c[1],c[2]));if(lo<0)for(int i=0;i<3;i++)c[i]=l+(c[i]-l)*l/(l-lo);if(hi>1)for(int i=0;i<3;i++)c[i]=l+(c[i]-l)*(1-l)/(hi-l);return c;}
 static double[] setSat(double[] input,double s){double[] c=input.clone();Integer[] order={0,1,2};java.util.Arrays.sort(order,(i,j)->Double.compare(input[i],input[j]));double lo=c[order[0]],hi=c[order[2]];c[order[1]]=hi>lo?(c[order[1]]-lo)*s/(hi-lo):0;c[order[2]]=hi>lo?s:0;c[order[0]]=0;return c;}
}
