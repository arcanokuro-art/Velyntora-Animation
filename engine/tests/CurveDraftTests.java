package art.velyntora.core;
public final class CurveDraftTests {
 static void check(boolean b){if(!b)throw new AssertionError();}
 public static void main(String[] args){
  CurveDraft c=new CurveDraft(0,0,90,0);
  check(c.x.length==2 && c.evaluate(0,true)==0 && c.evaluate(1,true)==90);
  check(c.insert(45,0,2)==1 && c.x.length==3);
  c.move(1,45,60);
  check(c.evaluate(.5f,true)==45 && c.evaluate(.5f,false)==60);
  check(c.hit(45,60,2)==1 && c.hit(-10,-10,2)==-1);
  check(c.insert(500,500,2)==-1);
  CurveDraft saved=c.copy();c.move(1,0,0);check(saved.x[1]==45 && saved.y[1]==60);
  c=saved.copy();check(c.insert(20,c.evaluate(.25f,false),30)>0);
  check(c.x.length==4 && c.evaluate(1,true)==90);
  c.setTension(1,0); check(c.tangent(1,true)==0);
  try{c.move(1,Float.NaN,0);throw new AssertionError();}catch(IllegalArgumentException expected){}
  try{c.evaluate(2,true);throw new AssertionError();}catch(IllegalArgumentException expected){}
  try{c.setTension(1,2);throw new AssertionError();}catch(IllegalArgumentException expected){}
  CurveDraft deletion=new CurveDraft(0,0,100,0);
  check(deletion.insert(50,0,2)==1);deletion.setTension(1,.7f);
  CurveDraft beforeDelete=deletion.copy();
  check(!deletion.removeInterior(0) && !deletion.removeInterior(2));
  check(deletion.removeInterior(1) && deletion.x.length==2 && deletion.tension.length==2);
  check(beforeDelete.x.length==3 && beforeDelete.tension[1]==.7f);
  check(!deletion.removeInterior(1) && !deletion.removeInterior(-1));
  // Original Pinta geometry, including endpoint and insertion defaults.
  CurveDraft legacy=new CurveDraft(0,0,160,0);
  check(legacy.tension[0]==0 && legacy.tension[1]==0);
  check(!legacy.isCorner(0) && !legacy.isCorner(1));
  legacy.insert(80,0,2);legacy.move(1,80,90);
  check(legacy.tension[1]==1f/3f && !legacy.isCorner(1));
  check(Math.abs(legacy.segment(0,.25f,true)-8.75f)<.001f);
  check(Math.abs(legacy.segment(0,.5f,true)-30f)<.001f);
  check(Math.abs(legacy.segment(1,.5f,true)-130f)<.001f);
  check(legacy.segment(0,.5f,false)==45f);
  CurveDraft curved=legacy.copy();legacy.setCorner(1,true);
  check(legacy.isCorner(1) && !legacy.sameGeometry(curved));
  check(legacy.tangent(1,true)==0 && legacy.tangent(1,false)==0);
  for(int i=1;i<100;i++){
    float t=i/100f,px=legacy.segment(0,t,true),py=legacy.segment(0,t,false);
    check(Math.abs(py-px*90/80)<.001f);
  }
  legacy.setCorner(1,false);check(legacy.sameGeometry(curved));
  legacy.setCorner(1,true);
  CurveDraft group=legacy.newLine(200,200);
  check(group.others.get(0).isCorner(1));
  CurveDraft isolated=group.copy();isolated.others.get(0).setCorner(1,false);
  check(group.others.get(0).isCorner(1) && !group.sameGeometry(isolated));
  System.out.println("Cardinal spline nodes, insertion, interpolation, tension and copy passed");
 }
}
