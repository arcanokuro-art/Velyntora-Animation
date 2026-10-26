package art.velyntora.core;

import static org.junit.Assert.*;
import android.app.Instrumentation;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;

public final class ToolWidthVisibilityDeviceTests {
  View find(View v,String description){
    if(description.contentEquals(v.getContentDescription()==null?"":v.getContentDescription()))return v;
    if(v instanceof ViewGroup group)for(int i=0;i<group.getChildCount();i++){View result=find(group.getChildAt(i),description);if(result!=null)return result;}
    return null;
  }
  @Test public void everyToolShowsOnlyRelevantWidthOrToleranceAndPreservesBothValues(){
    Instrumentation inst=InstrumentationRegistry.getInstrumentation();
    MainActivity host=(MainActivity)inst.startActivitySync(new Intent(inst.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    try{inst.runOnMainSync(()->{
      DrawingView v=host.readDrawing();v.setBrushRadius(37);v.setWandTolerance(83);
      int[] widthTools={0,1,2,3,5,16,18,20,24,25,26,27,28};
      for(int tool=0;tool<=28;tool++){
        v.setTool(tool);host.syncToolState();boolean width=false;for(int candidate:widthTools)width|=candidate==tool;
        assertEquals(width,ToolWidthPolicy.usesWidth(tool));
        SeekBar slider=(SeekBar)find(host.getWindow().getDecorView(),tool==DrawingView.MAGIC_WAND?"Tolerancia":"Anchura del pincel en píxeles");
        assertNotNull(slider);ViewGroup parent=(ViewGroup)slider.getParent();
        TextView label=(TextView)parent.getChildAt(parent.indexOfChild(slider)-1);
        int visibility=width || tool==DrawingView.MAGIC_WAND?View.VISIBLE:View.GONE;
        assertEquals("Slider for tool "+tool,visibility,slider.getVisibility());assertEquals(visibility,label.getVisibility());
        assertEquals(37,v.brushRadius(),.001);assertEquals(83,v.readWandTolerance());
        if(tool==DrawingView.MAGIC_WAND){assertEquals(255,slider.getMax());assertEquals(83,slider.getProgress());assertTrue(label.getText().toString().startsWith("Tolerancia:"));}
        else if(width){assertEquals(299,slider.getMax());assertEquals(73,slider.getProgress());}
      }
      v.setTool(DrawingView.BRUSH);host.syncToolState();assertEquals(View.VISIBLE,find(host.getWindow().getDecorView(),"Anchura del pincel en píxeles").getVisibility());
    });}finally{inst.runOnMainSync(host::finish);}
  }
}
