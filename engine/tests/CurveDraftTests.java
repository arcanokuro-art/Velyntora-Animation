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
  System.out.println("Cardinal spline nodes, insertion, interpolation, tension and copy passed");
 }
}
