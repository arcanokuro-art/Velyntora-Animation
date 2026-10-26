package art.velyntora.core;

import android.graphics.*;

/** One repeated texture instead of thousands of rectangle commands per frame. */
final class Checkerboard {
  private static final Paint paint = new Paint();
  static {
    Bitmap tile=Bitmap.createBitmap(32,32,Bitmap.Config.ARGB_8888);
    Canvas c=new Canvas(tile);
    c.drawColor(0xffffffff);
    Paint p=new Paint();p.setColor(0xffd1d1d1);
    c.drawRect(0,0,16,16,p);c.drawRect(16,16,32,32,p);
    paint.setShader(new BitmapShader(tile,Shader.TileMode.REPEAT,Shader.TileMode.REPEAT));
    paint.setFilterBitmap(false);
  }
  static void draw(Canvas canvas,int width,int height) {
    canvas.drawRect(0,0,width,height,paint);
  }
}
