package art.velyntora.core;
public final class NavigationToolTests {
 static void check(boolean ok){if(!ok)throw new AssertionError();}
 public static void main(String[] args){Viewport v=new Viewport(100,100);v.fit(100,100);NavigationTool tool=new NavigationTool(4);tool.begin(10,20);tool.move(v,40,60,false);check(v.centerX==80&&v.centerY==90&&v.scale==1);tool.finish(v,false,false);check(v.scale==1);
  double x=v.documentX(30,30),y=v.documentY(30,30);tool.begin(30,30);tool.finish(v,true,false);check(Math.abs(v.scale-1.25)<1e-9&&Math.abs(v.documentX(30,30)-x)<1e-9&&Math.abs(v.documentY(30,30)-y)<1e-9);tool.begin(30,30);tool.finish(v,true,true);check(Math.abs(v.scale-1)<1e-9);
  tool.begin(30,30);tool.move(v,30,-30,true);double scale=v.scale;check(scale>1);tool.finish(v,true,false);check(v.scale==scale&&Math.abs(v.documentX(30,30)-x)<1e-9);System.out.println("Dedicated pan and anchored tap/drag zoom passed");}
}
