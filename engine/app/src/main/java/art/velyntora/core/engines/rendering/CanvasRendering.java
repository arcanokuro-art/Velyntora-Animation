package art.velyntora.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;


/** Owns engines/rendering behavior; the host is the workspace composition facade. */
final class CanvasRendering {
  private final DrawingView host;
  private CanvasTileDisplay display;
  private double drawnScale,drawnAngle,drawnX,drawnY;
  private int drawnWidth,drawnHeight;
  NativeTileDisplay.Preparation captureDetail(){
    Viewport v=host.readViewport();
    if(display instanceof NativeTileDisplay tiled && drawnScale==v.scale && drawnAngle==v.angle
        && drawnX==v.centerX && drawnY==v.centerY && drawnWidth==host.getWidth() && drawnHeight==host.getHeight())return tiled.preparation();
    return null;
  }
  CanvasRendering(DrawingView host) { this.host = host; }
  static final class Frame {
    final Bitmap bitmap;final CanvasTileDisplay display;final int width,height;
    Frame(Bitmap bitmap,float scale){this(bitmap,bitmap.getWidth(),bitmap.getHeight(),scale);}
    Frame(Bitmap bitmap,int width,int height,float scale){
      this.bitmap=bitmap;this.width=width;this.height=height;
      display=bitmap.getWidth()==width && bitmap.getHeight()==height?new TileDisplay(bitmap):new NativeTileDisplay(width,height,bitmap);
      try{display.warm(scale);}catch(RuntimeException|OutOfMemoryError error){display.clear();throw error;}
    }
    void dispose(){display.clear();bitmap.recycle();}
  }
  static Frame prepareFrame(float fitScale){
    int width=DrawingView.nativeWidth(),height=DrawingView.nativeHeight(),sample=1;
    while((long)((width+sample-1)/sample)*((height+sample-1)/sample)>1048576)sample*=2;
    int w=(width+sample-1)/sample,h=(height+sample-1)/sample;
    int[] pixels=DrawingView.nativeSampleRegion(0,0,sample,w,h);
    if(pixels==null || pixels.length!=w*h)throw new IllegalStateException("Incomplete frame");
    Bitmap bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
    bitmap.setPixels(pixels,0,w,0,0,w,h);
    try{return new Frame(bitmap,width,height,fitScale);}catch(RuntimeException|OutOfMemoryError error){bitmap.recycle();throw error;}
  }
  /** Capture on the UI thread before a worker changes the document. */
  static Rect visibleBounds(DrawingView drawing){
    Viewport viewport=drawing.readViewport();
    double left=Double.POSITIVE_INFINITY,top=left,right=Double.NEGATIVE_INFINITY,bottom=right;
    for(int y:new int[]{0,drawing.getHeight()})for(int x:new int[]{0,drawing.getWidth()}){
      double dx=viewport.documentX(x,y),dy=viewport.documentY(x,y);
      left=Math.min(left,dx);top=Math.min(top,dy);right=Math.max(right,dx);bottom=Math.max(bottom,dy);
    }
    return new Rect((int)Math.floor(left)-2,(int)Math.floor(top)-2,(int)Math.ceil(right)+2,(int)Math.ceil(bottom)+2);
  }
  static Frame prepareFrame(float scale,Rect visible){
    Frame frame=prepareFrame(scale);
    try{
      if(frame.display instanceof NativeTileDisplay tiled)tiled.warmVisible(scale,visible);
      return frame;
    }catch(RuntimeException|OutOfMemoryError error){frame.dispose();throw error;}
  }
  static Frame prepareFrame(float scale,Rect visible,NativeTileDisplay.Preparation detail){
    if(detail==null)return prepareFrame(scale,visible);
    Frame frame=prepareFrame(scale);
    try{if(frame.display instanceof NativeTileDisplay tiled)tiled.warmVisible(detail);return frame;}
    catch(RuntimeException|OutOfMemoryError error){frame.dispose();throw error;}
  }
  void installFrame(Frame frame){
    host.removeCallbacks(host.readStrokeRefresh());host.writeStrokeRefreshPending(false);
    if(display!=null)display.clear();
    Bitmap old=host.readBitmap();host.writeBitmap(frame.bitmap);display=frame.display;
    host.writeCanvasWidth(frame.width);host.writeCanvasHeight(frame.height);
    host.readViewport().documentSize(frame.width,frame.height);
    if(display instanceof NativeTileDisplay tiled)tiled.redraw(host::invalidate);
    old.recycle();host.writeDirtyLeft(host.writeDirtyTop(Integer.MAX_VALUE));host.writeDirtyRight(host.writeDirtyBottom(0));
    host.invalidate();if(host.readCanvasChangedListener()!=null)host.readCanvasChangedListener().run();host.viewportChanged();
  }
  void drawPixels(Canvas canvas){
    if(display==null){
      display=host.readBitmap().getWidth()==host.readCanvasWidth() && host.readBitmap().getHeight()==host.readCanvasHeight()
          ?new TileDisplay(host.readBitmap()):new NativeTileDisplay(host.readCanvasWidth(),host.readCanvasHeight(),host.readBitmap());
      if(display instanceof NativeTileDisplay tiled)tiled.redraw(host::invalidate);
    }
    display.draw(canvas,(float)host.readViewport().scale);
    Viewport v=host.readViewport();drawnScale=v.scale;drawnAngle=v.angle;drawnX=v.centerX;drawnY=v.centerY;
    drawnWidth=host.getWidth();drawnHeight=host.getHeight();
  }

  void noteStrokeBounds(float x0,float y0,float x1,float y1,float radius) {
    float margin=Math.min(radius,2048)+2;
    host.writeDirtyLeft(Math.min(host.readDirtyLeft(),Math.max(0,(int)Math.floor(Math.min(x0,x1)-margin))));
    host.writeDirtyTop(Math.min(host.readDirtyTop(),Math.max(0,(int)Math.floor(Math.min(y0,y1)-margin))));
    host.writeDirtyRight(Math.max(host.readDirtyRight(),Math.min(host.readCanvasWidth(),(int)Math.ceil(Math.max(x0,x1)+margin))));
    host.writeDirtyBottom(Math.max(host.readDirtyBottom(),Math.min(host.readCanvasHeight(),(int)Math.ceil(Math.max(y0,y1)+margin))));
  }

  void refreshStrokePixels() {
    int w=host.readDirtyRight()-host.readDirtyLeft(),h=host.readDirtyBottom()-host.readDirtyTop();
    if(w>0 && h>0) {
      int[] pixels=display instanceof NativeTileDisplay?null:host.nativeRegionPixels(host.readDirtyLeft(),host.readDirtyTop(),w,h);
      if(pixels!=null && pixels.length==w*h)
        host.readBitmap().setPixels(pixels,0,w,host.readDirtyLeft(),host.readDirtyTop(),w,h);
      if(display!=null)display.invalidate(host.readDirtyLeft(),host.readDirtyTop(),w,h);
    }
    host.writeDirtyLeft(host.writeDirtyTop(Integer.MAX_VALUE));host.writeDirtyRight(host.writeDirtyBottom(0));
    host.invalidate();
  }

  void requestStrokeRefresh() {
    if (!host.readStrokeRefreshPending()) {
      host.writeStrokeRefreshPending(true);
      host.postOnAnimation(host.readStrokeRefresh());
    }
  }

  void finishStrokeRefresh() {
    host.removeCallbacks(host.readStrokeRefresh());
    host.writeStrokeRefreshPending(false);
    refreshStrokePixels();
    if (host.readCanvasChangedListener() != null) host.readCanvasChangedListener().run();
    host.viewportChanged();
  }

  void refresh() {
    host.removeCallbacks(host.readStrokeRefresh());
    host.writeStrokeRefreshPending(false);
    host.writeDirtyLeft(host.writeDirtyTop(Integer.MAX_VALUE));host.writeDirtyRight(host.writeDirtyBottom(0));
    host.refreshPixels();
    if (host.readCanvasChangedListener() != null) host.readCanvasChangedListener().run();
    host.viewportChanged();
  }

  void refreshPixels() {
    int width=host.nativeWidth(),height=host.nativeHeight();if(width<=0 || height<=0)return;
    if((long)width*height>1048576){
      if(width==host.readCanvasWidth() && height==host.readCanvasHeight() && display instanceof NativeTileDisplay){
        display.invalidate(0,0,width,height);host.invalidate();return;
      }
      Frame frame=prepareFrame((float)host.readViewport().scale);installFrame(frame);return;
    }
    if(display!=null)display.clear();display=null;
    if(width!=host.readBitmap().getWidth() || height!=host.readBitmap().getHeight()){
      Bitmap next=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);host.readBitmap().recycle();host.writeBitmap(next);
    }
    host.writeCanvasWidth(width);host.writeCanvasHeight(height);host.readViewport().documentSize(width,height);
    int[] pixels=host.nativeRegionPixels(0,0,width,height);
    if(pixels!=null)host.readBitmap().setPixels(pixels,0,width,0,0,width,height);host.invalidate();
  }

  void onDraw(Canvas canvas) {
    host.defaultDraw(canvas);
    WorkspaceRenderer.draw(host, canvas);
  }
}
