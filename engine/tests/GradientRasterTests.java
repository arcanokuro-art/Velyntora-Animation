package art.velyntora.core;
public final class GradientRasterTests {
 static void check(boolean ok){if(!ok)throw new AssertionError();}
 public static void main(String[] args){
  int[] a=GradientRaster.create(3,1,.5,.5,2.5,.5,0xff804020);check(a[2]==0xff804020&&a[3]==0x80804020&&a[4]==0);
  int[] b=GradientRaster.create(3,1,2.5,.5,.5,.5,0x80804020);check(b[2]==0&&b[3]==0x40804020&&b[4]==0x80804020);
  int[] c=GradientRaster.create(1,3,.5,.5,.5,2.5,0xff112233);check(c[2]==0xff112233&&c[4]==0);
  int[] d=GradientRaster.create(2,2,1,1,1,1,0xff000000);for(int i=2;i<d.length;i++)check(d[i]==0);
  try{GradientRaster.create(2,2,Double.NaN,0,1,1,0);throw new AssertionError();}catch(IllegalArgumentException expected){}
  int[] radial=GradientRaster.create(3,3,1.5,1.5,2.5,1.5,0xff112233,true);
  check(radial[6]==0xff112233&&radial[3]==0&&radial[5]==0&&radial[7]==0&&radial[9]==0);
  try{GradientRaster.create(1,1,-Double.MAX_VALUE,0,Double.MAX_VALUE,0,0);throw new AssertionError();}catch(IllegalArgumentException expected){}
  int[] pair=GradientRaster.create(3,1,.5,.5,2.5,.5,0xffff0000,0xff0000ff,0);check(pair[2]==0xffff0000&&pair[3]==0xff800080&&pair[4]==0xff0000ff);
  int[] alpha=GradientRaster.create(3,1,.5,.5,2.5,.5,0x00ff0000,0xff0000ff,0);check(alpha[2]==0&&alpha[3]==0x800000ff&&alpha[4]==0xff0000ff);
  int[] reflected=GradientRaster.create(5,1,2.5,.5,4.5,.5,0xff000000,0xffffffff,2);check(reflected[2]==0xffffffff&&reflected[4]==0xff000000&&reflected[6]==0xffffffff&&reflected[3]==reflected[5]);
  int[] diamond=GradientRaster.create(3,3,1.5,1.5,2.5,1.5,0xff000000,0xffffffff,3);check(diamond[6]==0xff000000&&diamond[3]==0xffffffff&&diamond[5]==0xffffffff&&diamond[7]==0xffffffff);
  int[] conical=GradientRaster.create(3,3,1.5,1.5,2.5,1.5,0xff000000,0xffffffff,4);check(conical[7]==0xff000000&&conical[9]==0xff404040&&conical[5]==0xff808080&&conical[3]==0xffbfbfbf);
  for(int mode=0;mode<=4;mode++){int[] result=GradientRaster.create(2,2,0,0,3,3,0x40ffffff,0x80000000,mode);for(int i=2;i<result.length;i++)check((result[i]>>>24)>=64&&(result[i]>>>24)<=128);}
  try{GradientRaster.create(1,1,0,0,1,1,0,0,5);throw new AssertionError();}catch(IllegalArgumentException expected){}
  System.out.println("Gradient endpoints, alpha, direction, zero-length and invalid input passed");
 }
}
