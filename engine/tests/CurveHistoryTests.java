package art.velyntora.core;
public final class CurveHistoryTests {
 static void check(boolean b){if(!b)throw new AssertionError();}
 public static void main(String[] args){
  CurveHistory h=new CurveHistory();CurveDraft a=new CurveDraft(0,0,90,0),b=a.copy();b.insert(30,0,2);b.move(1,30,50);h.record(a,b);a.move(0,100,100);
  CurveDraft original=h.undo(b);check(original.x[0]==0&&original.y[1]==0);CurveDraft restored=h.redo(original);check(restored.y[1]==50);
  original=h.undo(restored);CurveDraft different=original.copy();different.move(1,60,70);h.record(original,different);check(h.redo(different)==null);
  h.clear();h.record(different,different);check(h.undo(different)==null);
  CurveDraft c=new CurveDraft(0,0,10,10);for(int i=0;i<120;i++){CurveDraft next=c.copy();next.move(1,i,0);h.record(c,next);c=next;}int count=0;while((c=h.undo(c))!=null)count++;check(count==120);
  CurveDraft group=new CurveDraft(0,0,90,0);CurveDraft second=group.newLine(10,40);second.move(1,80,40);
  h.clear();h.record(group,second);check(h.undo(second).others.isEmpty());
  CurveDraft again=h.redo(group);check(again.others.size()==1 && again.others.get(0).x[1]==90);
  CurveDraft changed=again.copy();changed.others.get(0).move(1,120,0);check(!changed.sameGeometry(again));
  h.record(again,changed);check(h.undo(changed).others.get(0).x[1]==90);
  CurveDraft tensionChange=again.copy();tensionChange.setTension(0,1);h.record(again,tensionChange);check(h.undo(tensionChange).tension[0]==0);
  System.out.println("Curve drag undo/redo, isolation, branching, no-op and 120 steps back to the initial draft passed");
 }
}
