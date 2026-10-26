package art.velyntora.core;
final class Blend5 {static double[] apply(double[] a,double[] b){double[] mix=new double[3];for(int c=0;c<3;c++){double x=a[c],y=b[c];mix[c]=Math.abs(y-x);}return mix;}}
