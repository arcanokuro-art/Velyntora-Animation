package art.velyntora.core;
import android.graphics.Canvas;
interface CanvasTileDisplay {
  void warm(float scale);
  void draw(Canvas canvas,float scale);
  void invalidate(int left,int top,int width,int height);
  void clear();
}
