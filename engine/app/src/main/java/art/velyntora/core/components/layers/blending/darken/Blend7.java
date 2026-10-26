package art.velyntora.core;
final class Blend7 {static double[] apply(double[] a,double[] b){double[] mix=new double[3];for(int c=0;c<3;c++){double x=a[c],y=b[c];mix[c]=Math.min(x,y);}return mix;}}
