package app.velyntora.animation.timeline;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Resolution-independent timeline controls. */
final class TimelineIcon extends Drawable {
  private final String kind;
  private final Paint p = new Paint(3);
  TimelineIcon(String kind) { this.kind = kind; }
  public void draw(Canvas c) {
    c.save(); c.translate(getBounds().left, getBounds().top);
    c.scale(getBounds().width()/24f, getBounds().height()/24f);
    p.setColor(0xffe6e9eb); p.setStrokeWidth(1.7f); p.setStrokeCap(Paint.Cap.ROUND);
    p.setStrokeJoin(Paint.Join.ROUND); p.setStyle(Paint.Style.STROKE);
    switch(kind) {
      case "play": path(c, "play"); break;
      case "pause": c.drawLine(8,5,8,19,p); c.drawLine(16,5,16,19,p); break;
      case "first": c.drawLine(5,5,5,19,p); triangle(c,18,5,7,12,18,19); break;
      case "previous": triangle(c,17,5,6,12,17,19); break;
      case "next": triangle(c,7,5,18,12,7,19); break;
      case "add": c.drawLine(12,3,12,21,p); c.drawLine(3,12,21,12,p); break;
      case "duplicate": c.drawRoundRect(8,3,21,17,2,2,p); c.drawLine(4,7,4,21,p); c.drawLine(4,21,17,21,p); break;
      case "delete": c.drawLine(4,6,20,6,p); c.drawLine(9,3,15,3,p); c.drawRect(6,6,18,21,p); c.drawLine(10,10,10,17,p); c.drawLine(14,10,14,17,p); break;
      case "link": c.rotate(-45,12,12); c.drawRoundRect(8,1,16,13,4,4,p); c.drawRoundRect(8,11,16,23,4,4,p); break;
      case "music": c.drawLine(9,17,9,5,p); c.drawLine(9,5,20,2,p); c.drawLine(20,2,20,15,p); c.drawOval(2,16,9,21,p); c.drawOval(13,14,20,19,p); break;
      default: p.setStyle(Paint.Style.FILL); for(int x=4;x<=20;x+=8)c.drawCircle(x,12,1.5f,p);
    }
    c.restore();
  }
  private void path(Canvas c,String ignored) { triangle(c,7,4,20,12,7,20); }
  private void triangle(Canvas c,float x,float y,float a,float b,float d,float e) {
    Path q=new Path();q.moveTo(x,y);q.lineTo(a,b);q.lineTo(d,e);q.close();p.setStyle(Paint.Style.FILL);c.drawPath(q,p);
  }
  public void setAlpha(int a) { p.setAlpha(a); }
  public void setColorFilter(ColorFilter f) { p.setColorFilter(f); }
  public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
