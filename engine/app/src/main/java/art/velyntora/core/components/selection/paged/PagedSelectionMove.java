package art.velyntora.core;

import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.RegionIterator;
import android.graphics.Path;
import java.util.Arrays;

/** Large selection movement: exact mask and native edit prepared off the UI thread. */
final class PagedSelectionMove {
  static byte[] mask(Region region,Rect bounds){
    int width=bounds.width();byte[] result=new byte[width*bounds.height()];
    Region clipped=new Region(region);clipped.op(bounds,Region.Op.INTERSECT);
    RegionIterator iterator=new RegionIterator(clipped);Rect run=new Rect();
    while(iterator.next(run))for(int y=run.top;y<run.bottom;y++)
      Arrays.fill(result,(y-bounds.top)*width+run.left-bounds.left,(y-bounds.top)*width+run.right-bounds.left,(byte)1);
    return result;
  }
  static boolean run(DrawingView drawing,int dx,int dy){
    if(!drawing.hasSelection() || !(drawing.getContext() instanceof MainActivity host) || host.readProjectProgress()!=null)return false;
    int width=drawing.documentWidth(),height=drawing.documentHeight();
    Rect bounds=new Rect((int)Math.floor(drawing.selectionLeft),(int)Math.floor(drawing.selectionTop),
      (int)Math.ceil(drawing.selectionRight),(int)Math.ceil(drawing.selectionBottom));
    if(bounds.isEmpty() || bounds.left<0 || bounds.top<0 || bounds.right>width || bounds.bottom>height)return false;
    final int moveX=Math.max(-bounds.left,Math.min(width-bounds.right,dx));
    final int moveY=Math.max(-bounds.top,Math.min(height-bounds.bottom,dy));
    if(moveX==0 && moveY==0)return false;
    int kind=drawing.selectionTool;boolean masked=kind==DrawingView.MAGIC_WAND || kind==DrawingView.SELECT_FREE;
    Region selected=masked?new Region(drawing.freeRegion):null;
    Path[] outline={null};
    drawing.readSelectionConfirmation().beforeMove();
    LayerWorkQueue.run(host,()->{
      boolean ok=masked?DrawingView.nativeMoveMaskedSelection(bounds.left,bounds.top,bounds.width(),bounds.height(),mask(selected,bounds),moveX,moveY)
        :DrawingView.nativeMovePixels(kind,bounds.left,bounds.top,bounds.right,bounds.bottom,moveX,moveY);
      if(ok && masked){selected.translate(moveX,moveY);outline[0]=PagedWand.displayBoundary(selected,width,height);}
      return ok;
    },true,"Moviendo selección…","No se pudo mover la selección",()->{
      if(masked){drawing.freeRegion.set(selected);drawing.wandBoundary.set(outline[0]);
        android.graphics.Matrix shift=new android.graphics.Matrix();shift.setTranslate(moveX,moveY);drawing.freePath.transform(shift);}
      drawing.selectionLeft+=moveX;drawing.selectionRight+=moveX;drawing.selectionTop+=moveY;drawing.selectionBottom+=moveY;
      drawing.readSelectionConfirmation().moved();
      drawing.invalidate();
    },drawing.selectionDragPreview::clear);
    return true;
  }
  private PagedSelectionMove(){}
}
