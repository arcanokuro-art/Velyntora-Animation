package art.velyntora.core;
import android.graphics.Canvas;
import android.view.MotionEvent;
/** Only paints the removal mask; inference is an explicit command. */
final class RemoveAiTool implements DrawingTool {
 public void down(DrawingView v,float x,float y,MotionEvent e){v.removeAi().down(x,y);}
 public void move(DrawingView v,float x,float y,MotionEvent e){v.removeAi().move(x,y,e);}
 public void up(DrawingView v,float x,float y,MotionEvent e){v.removeAi().move(x,y,e);v.removeAi().finishStroke();}
 public void cancel(DrawingView v){v.removeAi().cancelStroke();}
 public void preview(DrawingView v,Canvas canvas,float scale){v.removeAi().preview(canvas);}
}
