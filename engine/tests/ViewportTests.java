package art.velyntora.core;
public final class ViewportTests {
 private static void near(double a,double b){if(Math.abs(a-b)>1e-7)throw new AssertionError(a+" != "+b);}
 public static void main(String[] args){
  Viewport v=new Viewport(800,800);v.fit(1000,600);near(v.scale,.75);near(v.screenX(400,400),500);
  for(double angle:new double[]{0,15,90,-179,180})for(double scale:new double[]{.01,.75,1,70}){
   v.angle=angle;v.scale=scale;
   for(double[] point:new double[][]{{0,0},{400,400},{799,27},{-100,950}}){
    double x=v.screenX(point[0],point[1]),y=v.screenY(point[0],point[1]);
    near(v.documentX(x,y),point[0]);near(v.documentY(x,y),point[1]);
   }
  }
  v.fit(1000,600);double x=v.documentX(200,150),y=v.documentY(200,150);
  v.gesture(200,150,270,210,2,43);near(v.screenX(x,y),270);near(v.screenY(x,y),210);
  v.zoom(999,270,210);near(v.scale,70);near(v.screenX(x,y),270);near(v.screenY(x,y),210);
  v.zoom(.0001,270,210);near(v.scale,.01);near(v.screenX(x,y),270);near(v.screenY(x,y),210);
  v.gesture(0,0,1,1,Double.NaN,0);near(v.scale,.01);
  v.fit(0,0);near(v.scale,.01);
  v.documentSize(640,320);v.fit(1000,600);near(v.scale,1.5625);
  near(v.screenX(320,160),500);near(v.screenY(320,160),300);
  System.out.println("Viewport: inverse coordinates, gesture anchor, rotation and zoom limits passed");
 }
}
