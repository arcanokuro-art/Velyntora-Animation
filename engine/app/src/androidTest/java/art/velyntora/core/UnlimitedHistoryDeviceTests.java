package art.velyntora.core;
import static org.junit.Assert.*;
import android.app.Instrumentation;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
public final class UnlimitedHistoryDeviceTests {
  @Test public void diskHistoryKeeps140StepsAndBranchesWithoutLosingTheBeginning() {
    Instrumentation inst=InstrumentationRegistry.getInstrumentation();
    DrawingView.nativeHistoryDirectory(inst.getTargetContext().getCacheDir().getAbsolutePath());
    assertTrue(DrawingView.nativeCreate(64,64));
    int[] initial=DrawingView.nativeRemoveSource();
    try {
      for(int i=0;i<140;++i){
        DrawingView.nativeBeginEdit();
        DrawingView.nativeStroke(32,32,32,32,2,0xff000000|i);
      }
      int[] latest=DrawingView.nativeRemoveSource();
      for(int i=0;i<140;++i)assertTrue("Undo step "+i,DrawingView.nativeUndo());
      assertArrayEquals(initial,DrawingView.nativeRemoveSource());assertFalse(DrawingView.nativeUndo());
      for(int i=0;i<140;++i)assertTrue("Redo step "+i,DrawingView.nativeRedo());
      assertArrayEquals(latest,DrawingView.nativeRemoveSource());
      assertTrue(DrawingView.nativeUndo());DrawingView.nativeBeginEdit();
      DrawingView.nativeStroke(10,10,10,10,2,0xffabcdef);
      assertFalse(DrawingView.nativeRedo());
      for(int i=0;i<140;++i)assertTrue(DrawingView.nativeUndo());
      assertArrayEquals(initial,DrawingView.nativeRemoveSource());
    } finally {assertTrue(DrawingView.nativeCreate(64,64));}
  }
}
