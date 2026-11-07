package art.velyntora.core;

import android.graphics.Canvas;
import android.graphics.Paint;

/** Disposable document-space guides shared by image and text transformations. */
final class AlignmentGuides {
  private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
  float guideX=Float.NaN,guideY=Float.NaN;
  boolean rotation;
  void clear(){guideX=guideY=Float.NaN;rotation=false;}

  void move(PlacedImageTransform value,int width,int height,float threshold){
    clear();
    double angle=Math.toRadians(value.angle);
    double c=Math.abs(Math.cos(angle)),s=Math.abs(Math.sin(angle));
    float halfWidth=(float)((c*value.width+s*value.height)/2);
    float halfHeight=(float)((s*value.width+c*value.height)/2);
    float originalX=value.cx,originalY=value.cy;
    value.cx=snap(originalX,halfWidth,width,threshold,true);
    value.cy=snap(originalY,halfHeight,height,threshold,false);
  }

  private float snap(float center,float halfSize,int extent,float threshold,boolean xAxis){
    float best=threshold,bestCenter=center;
    // Check the center first so that exact ties prefer centering the element.
    for(int anchor=0;anchor<3;anchor++){
      float offset=anchor==0?0:anchor==1?-halfSize:halfSize;
      for(int target=0;target<3;target++){
        float line=target==0?extent/2f:target==1?0:extent;
        float candidate=line-offset;
        float distance=Math.abs(candidate-center);
        float current=xAxis?guideX:guideY;
        if(distance<best || (distance==best && Float.isNaN(current))){
          best=distance;bestCenter=candidate;
          if(xAxis)guideX=line;else guideY=line;
        }
      }
    }
    return bestCenter;
  }
  void rotate(PlacedImageTransform value){
    clear();float nearest=Math.round(value.angle/90f)*90f;
    rotation=Math.abs(value.angle-nearest)<=3f;
    if(rotation)value.angle=nearest;
  }
  void draw(Canvas canvas,PlacedImageTransform value,int width,int height,float scale,float density){
    paint.setColor(0xFF63DDEB);
    paint.setStrokeWidth(1.25f*density/scale);
    if(!Float.isNaN(guideX))canvas.drawLine(guideX,0,guideX,height,paint);
    if(!Float.isNaN(guideY))canvas.drawLine(0,guideY,width,guideY,paint);
    if(rotation){
      canvas.drawLine(0,value.cy,width,value.cy,paint);
      canvas.drawLine(value.cx,0,value.cx,height,paint);
    }
  }
}
