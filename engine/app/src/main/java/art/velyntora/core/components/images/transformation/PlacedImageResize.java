package art.velyntora.core;

/** Resizes about the opposing handle, snapping only the handle that moves. */
final class PlacedImageResize {
  private static final int[] X={-1,0,1,1,1,0,-1,-1},Y={-1,-1,-1,0,1,1,1,0};
  private static final float LIMIT=24000f;
  private static final double EPS=0.0001;

  static void apply(PlacedImageTransform start,PlacedImageTransform value,int handle,
      float dx,float dy,boolean proportional,int canvasWidth,int canvasHeight,
      float threshold,AlignmentGuides guides){
    guides.clear();
    int hx=X[handle],hy=Y[handle];
    double angle=Math.toRadians(start.angle),c=Math.cos(angle),s=Math.sin(angle);
    double w=start.width,h=start.height;
    double minScale=Math.min(1,Math.max(1/w,1/h)),maxScale=LIMIT/Math.max(w,h);
    double dw=0,dh=0;
    if(Math.abs(dx)>0.01f || Math.abs(dy)>0.01f){
      double rw=hx==0?w:clamp(w+hx*(dx*c+dy*s),1,LIMIT);
      double rh=hy==0?h:clamp(h+hy*(-dx*s+dy*c),1,LIMIT);
      if(proportional){
        double a=rw/w,b=rh/h;
        double scale=clamp(Math.abs(a-1)>=Math.abs(b-1)?a:b,minScale,maxScale);
        rw=w*scale;rh=h*scale;
      }
      double px=start.cx+hx*(rw-w/2)*c-hy*(rh-h/2)*s;
      double py=start.cy+hx*(rw-w/2)*s+hy*(rh-h/2)*c;
      double targetX=nearest(px,canvasWidth),targetY=nearest(py,canvasHeight);
      double ex=targetX-px,ey=targetY-py;
      if(!proportional && hx!=0 && hy!=0 &&
          Math.abs(ex)<=threshold && Math.abs(ey)<=threshold &&
          valid(rw+hx*(ex*c+ey*s)) && valid(rh+hy*(-ex*s+ey*c))){
        dw=hx*(ex*c+ey*s);dh=hy*(-ex*s+ey*c);
        guides.guideX=(float)targetX;guides.guideY=(float)targetY;
      }else{
        double ax=proportional?hx*w*c-hy*h*s:hx*c;
        double ay=proportional?hx*w*s+hy*h*c:hx*s;
        double bx=proportional?0:-hy*s,by=proportional?0:hy*c;
        double best=Double.POSITIVE_INFINITY;
        for(int axis=0;axis<2;axis++){
          double delta=axis==0?ex:ey;
          if(Math.abs(delta)>threshold)continue;
          double a=axis==0?ax:ay,b=axis==0?bx:by,norm=a*a+b*b;
          if(norm<EPS)continue;
          double u=delta*a/norm,v=delta*b/norm;
          double changeW=proportional?w*u:u,changeH=proportional?h*u:v;
          double distance=Math.hypot(ax*u+bx*v,ay*u+by*v);
          if(distance>threshold+EPS || distance>=best)continue;
          if(proportional){
            double scale=(rw+changeW)/w;
            if(scale<minScale-EPS || scale>maxScale+EPS)continue;
          }else if(!valid(rw+changeW) || !valid(rh+changeH))continue;
          best=distance;dw=changeW;dh=changeH;
          guides.guideX=axis==0?(float)targetX:Float.NaN;
          guides.guideY=axis==1?(float)targetY:Float.NaN;
        }
        if(best<Double.POSITIVE_INFINITY){
          double finalX=px+hx*dw*c-hy*dh*s,finalY=py+hx*dw*s+hy*dh*c;
          if(Math.abs(finalX-targetX)<EPS && Math.abs(ax)+Math.abs(bx)>EPS)guides.guideX=(float)targetX;
          if(Math.abs(finalY-targetY)<EPS && Math.abs(ay)+Math.abs(by)>EPS)guides.guideY=(float)targetY;
        }
      }
      dw+=rw-w;dh+=rh-h;
    }
    value.width=(float)(w+dw);value.height=(float)(h+dh);
    value.cx=(float)(start.cx+(hx*dw*c-hy*dh*s)/2);
    value.cy=(float)(start.cy+(hx*dw*s+hy*dh*c)/2);
  }

  private static double clamp(double value,double low,double high){
    return Math.max(low,Math.min(high,value));
  }
  private static boolean valid(double size){return size>=1-EPS && size<=LIMIT+EPS;}
  private static double nearest(double value,int extent){
    double center=extent/2.0;
    if(Math.abs(value-center)<=Math.min(Math.abs(value),Math.abs(value-extent)))return center;
    return Math.abs(value)<=Math.abs(value-extent)?0:extent;
  }
  private PlacedImageResize(){}
}
