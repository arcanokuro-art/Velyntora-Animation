package art.velyntora.core;
import android.graphics.Region;
import android.graphics.Path;
import android.graphics.Matrix;
import java.util.*;

/** Scanline flood selection: bounded source rows, bitset visitation, no full ARGB image. */
final class PagedWand {
  private static boolean matches(int a,int b,int tolerance){
    for(int shift=0;shift<=24;shift+=8)if(Math.abs((a>>>shift&255)-(b>>>shift&255))>tolerance)return false;
    return true;
  }
  static Region select(int width,int height,int sx,int sy,int tolerance){
    if(sx<0 || sy<0 || sx>=width || sy>=height)return new Region();
    LinkedHashMap<Integer,int[]> rows=new LinkedHashMap<Integer,int[]>(16,.75f,true){
      protected boolean removeEldestEntry(Map.Entry<Integer,int[]> e){return size()>8;}
    };
    java.util.function.IntFunction<int[]> row=y->rows.computeIfAbsent(y,key->DrawingView.nativeRegionPixels(0,key,width,1));
    int target=row.apply(sy)[sx];BitSet visited=new BitSet(width*height);
    ArrayDeque<Integer> queue=new ArrayDeque<>();queue.add(sy*width+sx);Region result=new Region();
    while(!queue.isEmpty()){
      int pos=queue.removeLast(),y=pos/width,x=pos%width;
      if(visited.get(pos))continue;
      int[] current=row.apply(y);if(!matches(current[x],target,tolerance)){visited.set(pos);continue;}
      int left=x,right=x+1;
      while(left>0 && !visited.get(y*width+left-1) && matches(current[left-1],target,tolerance))left--;
      while(right<width && !visited.get(y*width+right) && matches(current[right],target,tolerance))right++;
      visited.set(y*width+left,y*width+right);result.op(left,y,right,y+1,Region.Op.UNION);
      for(int next:new int[]{y-1,y+1}){
        if(next<0 || next>=height)continue;
        int[] neighbor=row.apply(next);boolean run=false;
        for(int col=left;col<right;col++){
          boolean accepted=!visited.get(next*width+col) && matches(neighbor[col],target,tolerance);
          if(accepted && !run)queue.add(next*width+col);
          run=accepted;
        }
      }
    }
    return result;
  }
  static void run(DrawingView drawing,int x,int y){
    if(!(drawing.getContext() instanceof MainActivity host))return;
    int width=drawing.documentWidth(),height=drawing.documentHeight(),tolerance=drawing.readWandTolerance();
    Region[] result={null};Path[] outline={null};
    LayerWorkQueue.run(host,()->{
      result[0]=select(width,height,x,y,tolerance);
      outline[0]=displayBoundary(result[0],width,height);
      return true;
    },false,"Seleccionando con varita…","No se pudo completar la selección",()->drawing.installWandRegion(result[0],outline[0]));
  }
  // The exact Region is used by editing. Only its visual contour is bounded.
  static Path displayBoundary(Region region,int width,int height){
    if((long)width*height<=1048576)return region.getBoundaryPath();
    int sample=Math.max(1,(Math.max(width,height)+127)/128);
    if(sample==1)return region.getBoundaryPath();
    int columns=(width+sample-1)/sample,rows=(height+sample-1)/sample;
    BitSet cells=new BitSet(columns*rows);
    android.graphics.RegionIterator iterator=new android.graphics.RegionIterator(region);
    android.graphics.Rect rect=new android.graphics.Rect();
    while(iterator.next(rect)){
      int left=Math.max(0,rect.left/sample),right=Math.min(columns,(rect.right+sample-1)/sample);
      if(left>=right)continue;
      for(int y=Math.max(0,rect.top/sample);y<Math.min(rows,(rect.bottom+sample-1)/sample);y++)
        cells.set(y*columns+left,y*columns+right);
    }
    Region preview=new Region();
    for(int y=0;y<rows;y++){
      int start=y*columns,end=start+columns,col=cells.nextSetBit(start);
      while(col>=0 && col<end){
        int right=Math.min(end,cells.nextClearBit(col));
        preview.op(col-start,y,right-start,y+1,Region.Op.UNION);
        col=cells.nextSetBit(right);
      }
    }
    Path path=preview.getBoundaryPath();Matrix scale=new Matrix();scale.setScale(sample,sample);path.transform(scale);
    return path;
  }
  private PagedWand(){}
}
