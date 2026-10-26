package art.velyntora.core;
final class Blend2 {static double[] apply(double[] a,double[] b){double[] mix=new double[3];for(int c=0;c<3;c++){double x=a[c],y=b[c];mix[c]=y>=1?1:x<=0?0:1-Math.min(1,(1-y)/x);}return mix;}}
