package art.velyntora.core;
import static org.junit.Assert.*;
import android.app.*;import android.content.*;import android.view.*;import android.widget.*;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
/** Real UI -> native layered documents -> frame switching and lifecycle. */
public final class AnimationIntegrationTest {
 private final Instrumentation i=InstrumentationRegistry.getInstrumentation();
 private Button button(View v,String text){if(v instanceof Button&&text.contentEquals(((Button)v).getText()))return (Button)v;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int n=0;n<g.getChildCount();n++){Button b=button(g.getChildAt(n),text);if(b!=null)return b;}}return null;}
 private void click(Activity a,String text){Button b=button(a.getWindow().getDecorView(),text);assertNotNull("Missing control: "+text,b);b.performClick();}
 @Test public void nativeFramesDuplicateSwitchAndPlayback(){
  AnimationActivity a=(AnimationActivity)i.startActivitySync(new Intent(i.getTargetContext(),AnimationActivity.class).setAction("app.velyntora.animation.CREATE").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
  try{i.waitForIdleSync();i.runOnMainSync(()->{
   assertFalse(a.isFinishing());assertEquals(1280,a.readDrawing().documentWidth());
   int initial=a.readDrawing().layerCount();a.readDrawing().addLayer();assertEquals(initial+1,a.readDrawing().layerCount());
   click(a,"Duplicar");assertEquals(initial+1,a.readDrawing().layerCount());
   click(a,"+ Fotograma");assertEquals(initial,a.readDrawing().layerCount());
   click(a,"1");assertEquals(initial+1,a.readDrawing().layerCount());
   click(a,"Cebolla: no");assertNotNull(button(a.getWindow().getDecorView(),"Cebolla: sí"));
   click(a,"Reproducir");assertNotNull(button(a.getWindow().getDecorView(),"Pausar"));
   click(a,"Pausar");assertNotNull(button(a.getWindow().getDecorView(),"Reproducir"));
  });}finally{i.runOnMainSync(a::finish);}
 }
}
