package art.velyntora.core;

import android.graphics.Canvas;
import android.graphics.Paint;

/** Disposable document-space guides shared by image and text transformations. */
final class AlignmentGuides {
  boolean vertical,horizontal,rotation;
  void clear(){vertical=horizontal=rotation=false;}
  void move(PlacedImageTransform value,int width,int height,float threshold){
    clear();
    vertical=Math.abs(value.cx-width/2f)<=threshold;
    horizontal=Math.abs(value.cy-height/2f)<=threshold;
    if(vertical)value.cx=width/2f;
    if(horizontal)value.cy=height/2f;
  }
  void rotate(PlacedImageTransform value){
    clear();float nearest=Math.round(value.angle/90f)*90f;
    rotation=Math.abs(value.angle-nearest)<=3f;
    if(rotation)value.angle=nearest;
  }
  void draw(Canvas canvas,PlacedImageTransform value,int width,int height,float scale,float density){
    Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(0xFF63DDEB);
    paint.setStrokeWidth(1.25f*density/scale);
    if(vertical)canvas.drawLine(width/2f,0,width/2f,height,paint);
    if(horizontal)canvas.drawLine(0,height/2f,width,height/2f,paint);
    if(rotation){
      canvas.drawLine(0,value.cy,width,value.cy,paint);
      canvas.drawLine(value.cx,0,value.cx,height,paint);
    }
  }
}
