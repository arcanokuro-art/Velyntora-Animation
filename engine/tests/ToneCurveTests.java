package art.velyntora.core;
public final class ToneCurveTests {
 static void check(boolean ok){if(!ok)throw new AssertionError();}
 public static void main(String[] args){
  ToneCurve c=new ToneCurve();int[] values=c.table();for(int i=0;i<256;++i)check(values[i]==i);
  c.set(64,32);c.set(128,192);c.set(192,224);values=c.table();check(values[64]==32&&values[128]==192&&values[192]==224);
  for(int i=1;i<256;++i)check(values[i]>=values[i-1]&&values[i]<=255);
  c.set(192,40);values=c.table();check(values[192]==40);for(int v:values)check(v>=0&&v<=255);
  c.remove(0);c.remove(255);check(c.points.containsKey(0)&&c.points.containsKey(255));c.remove(192);check(!c.points.containsKey(192));
  c.reset();values=c.table();for(int i=0;i<256;++i)check(values[i]==i);
  System.out.println("Tone curves: interpolation, control points, monotonicity, clamping and reset passed");
 }
}
