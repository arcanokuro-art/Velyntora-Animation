package app.velyntora.animation.timeline;
import static org.junit.Assert.*;
import android.content.Context;import android.content.res.Configuration;import android.view.*;
import androidx.test.platform.app.InstrumentationRegistry;
import app.velyntora.animation.model.AnimationSequence;
import app.velyntora.animation.preview.FramePreviewCache;
import app.velyntora.animation.storage.AnimationProject;
import java.io.File;import java.util.*;import org.junit.Test;
public final class LargeTimelineTest {
 @Test public void everyFrameRemainsReachableAtHighDensity()throws Exception{
  android.app.Instrumentation i=InstrumentationRegistry.getInstrumentation();
  Context host=i.getTargetContext();File folder=new File(host.getCacheDir(),"large-timeline-"+AnimationSequence.uuid());
  AnimationProject project=new AnimationProject(folder,"130 minutos",64,48,30);
  ArrayList<AnimationSequence.Frame> frames=new ArrayList<>();
  for(int n=0;n<234000;n++)frames.add(new AnimationSequence.Frame(AnimationSequence.uuid(),null,1));
  project.sequence.replace(frames,233999,30);
  FramePreviewCache cache=new FramePreviewCache();
  try{
   i.runOnMainSync(()->{
    Configuration config=new Configuration(host.getResources().getConfiguration());config.densityDpi=480;
    TimelineStrip strip=new TimelineStrip(host.createConfigurationContext(config),cache,n->{},()->{});
    strip.bind(project);strip.measure(View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED),
      View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
    assertEquals(strip.cell*234001,strip.getMeasuredWidth());
    assertTrue(strip.getMeasuredWidth()<=16000000);
    assertTrue(strip.cell*234000<strip.getMeasuredWidth());
   });
  }finally{cache.close();new File(folder,"objects").delete();folder.delete();}
 }
}
