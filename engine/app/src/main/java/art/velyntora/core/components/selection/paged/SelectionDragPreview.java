package art.velyntora.core;

import android.graphics.*;

/** Disposable visual drag state. It never edits the native document or history. */
final class SelectionDragPreview {
  private final DrawingView drawing;
  private Bitmap base,piece;
  private Rect bounds;
  private boolean active,waiting;
  private int generation,dx,dy;
  SelectionDragPreview(DrawingView drawing){this.drawing=drawing;}
  void begin(){
    clear();
    if(!drawing.movingPixels || !drawing.movingSelection)return;
    active=true;bounds=new Rect((int)drawing.selectionLeft,(int)drawing.selectionTop,(int)drawing.selectionRight,(int)drawing.selectionBottom);
    // A flattened image is a faithful source only for a single opaque normal layer.
    // Other documents keep their normal compositing and use the moving contour.
    if(drawing.layerCount()!=1 || !drawing.layerVisible(0) || drawing.layerBlendMode(0)!=0
        || drawing.layerOpacity()!=1f || (long)drawing.documentWidth()*drawing.documentHeight()>1048576)return;
    Bitmap snapshot=drawing.readBitmap().copy(Bitmap.Config.ARGB_8888,true);
    if(snapshot==null)return;
    int token=generation,kind=drawing.selectionTool;
    Rect source=new Rect(bounds);Region region=new Region(drawing.freeRegion);
    new Thread(()->{
      Bitmap background=null,selected=null;
      try{
        background=snapshot;selected=Bitmap.createBitmap(source.width(),source.height(),Bitmap.Config.ARGB_8888);
        Path mask=new Path();
        if(kind==DrawingView.MAGIC_WAND || kind==DrawingView.SELECT_FREE)mask=region.getBoundaryPath();
        else if(kind==DrawingView.SELECT_ELLIPSE)mask.addOval(new RectF(source),Path.Direction.CW);
        else mask.addRect(new RectF(source),Path.Direction.CW);
        Canvas cut=new Canvas(selected);cut.translate(-source.left,-source.top);cut.clipPath(mask);cut.drawBitmap(snapshot,0,0,null);
        Canvas cleared=new Canvas(background);cleared.clipPath(mask);cleared.drawColor(Color.TRANSPARENT,PorterDuff.Mode.CLEAR);
      }catch(RuntimeException|OutOfMemoryError error){if(background!=null)background.recycle();if(selected!=null)selected.recycle();background=null;selected=null;}
      Bitmap readyBase=background,readyPiece=selected;
      drawing.post(()->{
        if(token!=generation || !active){if(readyBase!=null)readyBase.recycle();if(readyPiece!=null)readyPiece.recycle();return;}
        base=readyBase;piece=readyPiece;drawing.invalidate();
      });
    },"velyntora-selection-preview").start();
  }
  void release(float x,float y){
    if(!active)return;
    if(drawing.movingSelection)drawing.updateMovedSelection(x,y);
    dx=Math.round(drawing.selectionLeft-drawing.moveOriginalLeft);dy=Math.round(drawing.selectionTop-drawing.moveOriginalTop);waiting=true;
  }
  boolean draw(Canvas canvas){
    if(!active || base==null || piece==null)return false;
    int x=waiting?dx:Math.round(drawing.selectionLeft-drawing.moveOriginalLeft);
    int y=waiting?dy:Math.round(drawing.selectionTop-drawing.moveOriginalTop);
    drawing.paint.setFilterBitmap(drawing.readViewport().scale<1);
    canvas.drawBitmap(base,0,0,drawing.paint);
    canvas.drawBitmap(piece,bounds.left+x,bounds.top+y,drawing.paint);
    return true;
  }
  void contour(Canvas canvas,float scale){
    boolean irregular=drawing.selectionTool==DrawingView.MAGIC_WAND || drawing.selectionTool==DrawingView.SELECT_FREE;
    float x=0,y=0;
    if(active && waiting){x=dx;y=dy;}
    else if(drawing.movingPixels && drawing.movingSelection && irregular){x=drawing.selectionLeft-drawing.moveOriginalLeft;y=drawing.selectionTop-drawing.moveOriginalTop;}
    canvas.save();canvas.translate(x,y);SelectionSupport.draw(drawing,canvas,scale);canvas.restore();
  }
  void clear(){
    generation++;active=false;waiting=false;
    // Let the rendering pipeline release previously submitted bitmap draws safely.
    base=null;piece=null;bounds=null;drawing.invalidate();
  }
}
