package art.velyntora.core;
final class Blend11 {static double[] apply(double[] a,double[] b){double[] mix=new double[3];for(int c=0;c<3;c++){double x=a[c],y=b[c];mix[c]=x<=.5?y-(1-2*x)*y*(1-y):y+(2*x-1)*((y<=.25?((16*y-12)*y+4)*y:Math.sqrt(y))-y);}return mix;}}
