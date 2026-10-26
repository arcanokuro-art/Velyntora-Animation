package art.velyntora.core;

import android.graphics.*;
import java.util.*;

/** Independent GPU textures: an edited tile does not re-upload the full document. */
final class TileDisplay implements CanvasTileDisplay {
  static final int SIDE=256, CAPACITY=128;
  final Bitmap source;
  private final LinkedHashMap<Key,Tile> cache=new LinkedHashMap<>(16,.75f,true);
  private final Paint paint=new Paint();
  private record Key(int sample,int x,int y) {}
  private static final class Tile {
    final Bitmap bitmap;final Rect bounds;final int sample;
    final Rect dirty=new Rect();
    Tile(Bitmap bitmap,Rect bounds,int sample){this.bitmap=bitmap;this.bounds=bounds;this.sample=sample;dirty.set(0,0,bitmap.getWidth(),bitmap.getHeight());}
  }
  private final BitmapSampler sampler=new BitmapSampler();
  long createdTiles;
  long sampledPixels(){return sampler.pixelsRead;}
  private void update(Tile tile){
    if(tile.dirty.isEmpty())return;
    sampler.update(source,tile.bitmap,tile.bounds,tile.sample,tile.dirty);
    tile.dirty.setEmpty();
  }
  TileDisplay(Bitmap source){this.source=source;paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));}
  private int sample(float scale){
    int sample=1;
    while(sample<128 && scale*sample<.5f)sample*=2;
    return sample;
  }
  private Tile tile(Key key){
    Tile found=cache.get(key);if(found!=null){update(found);return found;}
    int span=SIDE*key.sample,left=key.x*span,top=key.y*span;
    int right=Math.min(left+span+key.sample,source.getWidth()),bottom=Math.min(top+span+key.sample,source.getHeight());
    left=Math.max(0,left-key.sample);top=Math.max(0,top-key.sample);
    int width=right-left,height=bottom-top;
    Bitmap result=Bitmap.createBitmap((width+key.sample-1)/key.sample,
        (height+key.sample-1)/key.sample,Bitmap.Config.ARGB_8888);
    Tile generated=new Tile(result,new Rect(left,top,left+result.getWidth()*key.sample,
        top+result.getHeight()*key.sample),key.sample);
    try{update(generated);}catch(RuntimeException|OutOfMemoryError error){result.recycle();throw error;}
    createdTiles++;
    cache.put(key,generated);
    while(cache.size()>CAPACITY){
      Iterator<Map.Entry<Key,Tile>> it=cache.entrySet().iterator();
      Map.Entry<Key,Tile> old=it.next();it.remove();old.getValue().bitmap.recycle();
    }
    return generated;
  }
  public void warm(float scale){
    int chosen=sample(scale);
    warmLevel(chosen);
    if(chosen>1){
      int finer=chosen/2,span=finer*SIDE;
      long count=(long)((source.getWidth()+span-1)/span)*((source.getHeight()+span-1)/span);
      int chosenSpan=chosen*SIDE;
      long coarse=(long)((source.getWidth()+chosenSpan-1)/chosenSpan)*((source.getHeight()+chosenSpan-1)/chosenSpan);
      if(count+coarse<=CAPACITY)warmLevel(finer);
    }
  }
  private void warmLevel(int sample){
    int span=sample*SIDE;
    long count=(long)((source.getWidth()+span-1)/span)*((source.getHeight()+span-1)/span);
    if(count>CAPACITY)return;
    for(int y=0;y<source.getHeight();y+=span)
      for(int x=0;x<source.getWidth();x+=span)tile(new Key(sample,x/span,y/span));
  }
  public void draw(Canvas canvas,float scale){
    Rect clip=canvas.getClipBounds();int sample=sample(scale),span=sample*SIDE;
    int left=Math.max(0,clip.left),top=Math.max(0,clip.top);
    int right=Math.min(source.getWidth(),clip.right),bottom=Math.min(source.getHeight(),clip.bottom);
    paint.setFilterBitmap(scale<1);
    int layer=canvas.saveLayer(null,null);
    for(int y=top/span;y*span<bottom;y++)for(int x=left/span;x*span<right;x++){
      Tile tile=tile(new Key(sample,x,y));
      canvas.drawBitmap(tile.bitmap,null,tile.bounds,paint);
    }
    canvas.restoreToCount(layer);
  }

  public void invalidate(int left,int top,int width,int height){
    for(Tile tile:cache.values()){
      int l=Math.max(left,tile.bounds.left),t=Math.max(top,tile.bounds.top);
      int r=Math.min(left+width,tile.bounds.right),b=Math.min(top+height,tile.bounds.bottom);
      if(l>=r||t>=b)continue;
      Rect changed=new Rect((l-tile.bounds.left)/tile.sample,(t-tile.bounds.top)/tile.sample,
          (r-tile.bounds.left+tile.sample-1)/tile.sample,(b-tile.bounds.top+tile.sample-1)/tile.sample);
      if(tile.dirty.isEmpty())tile.dirty.set(changed);else tile.dirty.union(changed);
    }
  }
  public void clear(){for(Tile t:cache.values())t.bitmap.recycle();cache.clear();}
}
