package art.velyntora.core;

import static org.junit.Assert.*;
import android.app.Instrumentation;
import android.graphics.Rect;
import android.os.*;
import android.view.*;
import java.util.*;

/** Real input dispatch and hardware frame timings after importing a large image. */
final class ContinuousFrameProbe {
  static void stroke(Instrumentation inst,MainActivity host) throws Exception {
    if(Build.VERSION.SDK_INT<24)return;
    HandlerThread collector=new HandlerThread("frame-metrics");collector.start();
    List<Long> frames=Collections.synchronizedList(new ArrayList<>());
    Window.OnFrameMetricsAvailableListener listener=(window,metrics,dropped)->{
      if(metrics.getMetric(FrameMetrics.FIRST_DRAW_FRAME)==0)
        frames.add(metrics.getMetric(FrameMetrics.TOTAL_DURATION));
    };
    int[] location=new int[2];float[] coordinates=new float[3];
    inst.runOnMainSync(()->{
      DrawingView drawing=host.readDrawing();drawing.getLocationOnScreen(location);
      coordinates[0]=location[0]+drawing.getWidth()/2f;
      coordinates[1]=location[1]+drawing.getHeight()/2f;
      coordinates[2]=Math.min(90,drawing.getWidth()*.2f);
      host.getWindow().addOnFrameMetricsAvailableListener(listener,new Handler(collector.getLooper()));
    });
    long down=SystemClock.uptimeMillis();
    try {
      send(inst,down,MotionEvent.ACTION_DOWN,coordinates[0]-coordinates[2],coordinates[1]);
      for(int move=0;move<70;move++) {
        float x=coordinates[0]-coordinates[2]+2*coordinates[2]*move/69f;
        send(inst,down,MotionEvent.ACTION_MOVE,x,coordinates[1]+(float)Math.sin(move*.15)*15);
        Thread.sleep(20);
      }
      send(inst,down,MotionEvent.ACTION_UP,coordinates[0]+coordinates[2],coordinates[1]);
      inst.waitForIdleSync();Thread.sleep(150);
    } finally {
      inst.runOnMainSync(()->host.getWindow().removeOnFrameMetricsAvailableListener(listener));
      collector.quitSafely();collector.join(2000);
    }
    List<Long> stable;
    synchronized(frames){stable=new ArrayList<>(frames.subList(Math.min(10,frames.size()),frames.size()));}
    assertTrue("Too few actual rendered frames: "+frames.size(),stable.size()>=20);
    Collections.sort(stable);long p95=stable.get((int)Math.ceil(stable.size()*.95)-1)/1000000;
    android.util.Log.i("LargeImportPerformance","rendered frames="+frames.size()+", p95="+p95+" ms");
    assertTrue("Continuous drawing p95 exceeded 100 ms: "+p95,p95<100);
  }
  private static void send(Instrumentation inst,long down,int action,float x,float y) {
    MotionEvent event=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,x,y,0);
    event.setSource(InputDevice.SOURCE_TOUCHSCREEN);
    try {assertTrue(inst.getUiAutomation().injectInputEvent(event,true));} finally {event.recycle();}
  }
}
