package art.velyntora.core;
final class Blend4 {static double[] apply(double[] a,double[] b){double[] mix=new double[3];for(int c=0;c<3;c++){double x=a[c],y=b[c];mix[c]=y<=.5?2*x*y:1-2*(1-x)*(1-y);}return mix;}}
