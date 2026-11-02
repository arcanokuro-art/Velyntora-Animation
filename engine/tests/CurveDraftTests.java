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
  // Mirroring a draft reverses its curve without changing its shape.
  CurveDraft smooth=new CurveDraft(0,0,160,0);
  smooth.insert(80,0,2);smooth.move(1,80,90);
  check(!smooth.isCorner(1));
  check(smooth.tangent(1,true)>160f/6f); // more rounded than the old three-node draft
  check(Math.abs(smooth.segment(0,.5f,false)-smooth.segment(1,.5f,false))<.001f);
  CurveDraft reversed=smooth.copy();
  for(int i=0;i<3;i++){reversed.x[i]=smooth.x[2-i];reversed.y[i]=smooth.y[2-i];reversed.tension[i]=smooth.tension[2-i];}
  for(int i=0;i<=100;i++){
    float t=i/100f;
    check(Math.abs(smooth.evaluate(t,true)-reversed.evaluate(1-t,true))<.001f);
    check(Math.abs(smooth.evaluate(t,false)-reversed.evaluate(1-t,false))<.001f);
  }
  CurveDraft rounded=smooth.copy();smooth.setCorner(1,true);
  check(smooth.isCorner(1) && !smooth.sameGeometry(rounded));
  // Straight segments meet exactly at the corner when its neighbours are endpoints.
  for(int i=1;i<100;i++){
    float t=i/100f;
    float px=smooth.segment(0,t,true),py=smooth.segment(0,t,false);
    check(Math.abs(py-px*90/80)<.001f);
  }
  check(smooth.evaluate(.5f,true)==80 && smooth.evaluate(.5f,false)==90);
  smooth.setCorner(1,false);check(smooth.sameGeometry(rounded));
  smooth.setCorner(1,true);
  CurveDraft grouped=smooth.newLine(200,200);check(grouped.others.get(0).isCorner(1));
  CurveDraft isolated=grouped.copy();isolated.others.get(0).setCorner(1,false);
  check(grouped.others.get(0).isCorner(1) && !grouped.sameGeometry(isolated));
  check(smooth.removeInterior(1) && smooth.tension.length==smooth.x.length);
  CurveDraft coincident=new CurveDraft(10,10,10,10);
  for(int i=0;i<=100;i++)check(Float.isFinite(coincident.evaluate(i/100f,true)));
  System.out.println("Cardinal spline nodes, insertion, interpolation, tension and copy passed");
 }
}

