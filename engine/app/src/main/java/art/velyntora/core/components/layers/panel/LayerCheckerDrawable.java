package art.velyntora.core;
import android.graphics.*;
import android.graphics.drawable.Drawable;
final class LayerCheckerDrawable extends Drawable {
 private final Paint paint=new Paint();
 public void draw(Canvas c){Rect b=getBounds();for(int y=b.top;y<b.bottom;y+=8)for(int x=b.left;x<b.right;x+=8){paint.setColor(((x-b.left)/8+(y-b.top)/8)%2==0?0xFFB0B0B0:0xFF777777);c.drawRect(x,y,Math.min(x+8,b.right),Math.min(y+8,b.bottom),paint);}}
 public void setAlpha(int a){}public void setColorFilter(ColorFilter f){}public int getOpacity(){return PixelFormat.OPAQUE;}
}
