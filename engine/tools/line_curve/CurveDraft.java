package art.velyntora.core;

import java.util.Arrays;
import java.util.ArrayList;

/** Editable cardinal spline. Every node lies on the rendered curve, as in Pinta. */
final class CurveDraft {
  float[] x, y, tension;
  final ArrayList<CurveDraft> others = new ArrayList<>();

  CurveDraft(float x0, float y0, float x1, float y1) {
    x = new float[] {x0, x1};
    y = new float[] {y0, y1};
    tension = new float[] {0, 0};
  }

  CurveDraft copy() {
    CurveDraft c = new CurveDraft(0, 0, 0, 0);
    c.x = x.clone(); c.y = y.clone(); c.tension = tension.clone();
    for (CurveDraft other : others) c.others.add(other.copy());
    return c;
  }

  boolean sameGeometry(CurveDraft other) {
    if (other == null || !Arrays.equals(x,other.x) || !Arrays.equals(y,other.y)
        || !Arrays.equals(tension,other.tension) || others.size()!=other.others.size()) return false;
    for (int i=0;i<others.size();i++) if (!others.get(i).sameGeometry(other.others.get(i))) return false;
    return true;
  }

  CurveDraft newLine(float px, float py) {
    CurveDraft result = new CurveDraft(px,py,px,py);
    for (CurveDraft other : others) result.others.add(other.copy());
    CurveDraft current = new CurveDraft(0,0,0,0);
    current.x=x.clone(); current.y=y.clone(); current.tension=tension.clone();
    result.others.add(current);
    return result;
  }

  void move(int point, float px, float py) {
    if (point < 0 || point >= x.length || !Float.isFinite(px) || !Float.isFinite(py))
      throw new IllegalArgumentException("Invalid curve node");
    x[point] = px; y[point] = py;
  }

  boolean removeInterior(int point) {
    if(point<=0 || point>=x.length-1)return false;
    x=removeAt(x,point);y=removeAt(y,point);tension=removeAt(tension,point);
    return true;
  }

  private static float[] removeAt(float[] source,int point){
    float[] result=new float[source.length-1];
    System.arraycopy(source,0,result,0,point);
    System.arraycopy(source,point+1,result,point,source.length-point-1);
    return result;
  }

  void setTension(int point, float value) {
    if (point < 0 || point >= x.length || !Float.isFinite(value) || value < 0 || value > 1)
      throw new IllegalArgumentException("Invalid tension");
    tension[point] = value;
  }

  int hit(float px, float py, float radius) {
    int point = -1;
    double best = radius;
    for (int i = 0; i < x.length; i++) {
      double d = Math.hypot(x[i] - px, y[i] - py);
      if (d <= best) { best = d; point = i; }
    }
    return point;
  }

  float tangent(int point, boolean horizontal) {
    float[] p = horizontal ? x : y;
    int last = p.length - 1;
    float factor = tension[point] * (point == 0 || point == last ? 1 : point / (float) last);
    return factor * (p[Math.min(last, point + 1)] - p[Math.max(0, point - 1)]);
  }

  float segment(int i, float t, boolean horizontal) {
    float[] p = horizontal ? x : y;
    float u = 1 - t;
    return u*u*u*p[i] + 3*u*u*t*(p[i]+tangent(i,horizontal))
        + 3*u*t*t*(p[i+1]-tangent(i+1,horizontal)) + t*t*t*p[i+1];
  }

  float evaluate(float t, boolean horizontal) {
    if (!Float.isFinite(t) || t < 0 || t > 1)
      throw new IllegalArgumentException("Invalid curve parameter");
    float position = t * (x.length - 1);
    int i = Math.min(x.length - 2, (int) position);
    return segment(i, position - i, horizontal);
  }

  /** Insert on the closest sampled curve segment; an off-curve press starts another line. */
  int insert(float px, float py, float radius) {
    int index = -1;
    double best = radius;
    for (int i = 0; i < x.length - 1; i++) {
      float ax = segment(i, 0, true), ay = segment(i, 0, false);
      for (int step = 1; step <= 40; step++) {
        float t = step / 40f, bx = segment(i,t,true), by = segment(i,t,false);
        double dx = bx-ax, dy = by-ay, length = dx*dx+dy*dy;
        double f = length == 0 ? 0 : Math.max(0, Math.min(1, ((px-ax)*dx+(py-ay)*dy)/length));
        double d = Math.hypot(px-ax-f*dx, py-ay-f*dy);
        if (d <= best) { best = d; index = i+1; }
        ax = bx; ay = by;
      }
    }
    if (index < 0) return -1;
    x = insertAt(x,index,px); y = insertAt(y,index,py);
    tension = insertAt(tension,index,1f/3f);
    return index;
  }

  private static float[] insertAt(float[] source, int index, float value) {
    float[] result = Arrays.copyOf(source,source.length+1);
    System.arraycopy(source,index,result,index+1,source.length-index);
    result[index] = value;
    return result;
  }
}
