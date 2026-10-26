package art.velyntora.core;

import static org.junit.Assert.*;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.widget.EditText;
import android.view.View;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import java.lang.reflect.*;

public final class ColorPickerDeviceTests {
  private Object field(Object picker,String name) throws Exception {
    Field f=ColorPickerDialog.class.getDeclaredField(name);f.setAccessible(true);return f.get(picker);
  }
  private void call(Object picker,String name,Class<?>[] types,Object... args) throws Exception {
    Method m=ColorPickerDialog.class.getDeclaredMethod(name,types);m.setAccessible(true);m.invoke(picker,args);
  }
  @Test public void editsSynchronizeChannelsAndKeepDraftUntilAccepted() {
    android.app.Instrumentation inst=InstrumentationRegistry.getInstrumentation();
    android.content.Intent intent=new android.content.Intent(inst.getTargetContext(),MainActivity.class);
    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
    MainActivity activity=(MainActivity)inst.startActivitySync(intent);
    inst.runOnMainSync(()->{
      Dialog dialog=null;
      try {
        int[] result={0,0};
        Constructor<?> c=ColorPickerDialog.class.getDeclaredConstructor(Context.class,int.class,int.class,boolean.class,ColorPickerDialog.Listener.class);
        c.setAccessible(true);
        Object picker=c.newInstance(activity,Color.BLACK,Color.WHITE,false,(ColorPickerDialog.Listener)(p,s)->{result[0]=p;result[1]=s;});
        dialog=(Dialog)field(picker,"dialog");
        EditText hex=(EditText)field(picker,"hex");
        hex.setText("FF800080");
        assertEquals(0x80ff8000,((int[])field(picker,"colors"))[0]);
        assertEquals(0,result[0]);
        EditText[] numbers=(EditText[])field(picker,"numbers");
        assertEquals("255",numbers[3].getText().toString());
        assertEquals("128",numbers[6].getText().toString());
        call(picker,"change",new Class<?>[]{int.class,int.class},0,240);
        call(picker,"change",new Class<?>[]{int.class,int.class},1,100);
        call(picker,"change",new Class<?>[]{int.class,int.class},2,100);
        assertEquals(0x800000ff,((int[])field(picker,"colors"))[0]);
        hex.setText("oops");
        assertEquals(0x800000ff,((int[])field(picker,"colors"))[0]);
        View[] swatches=(View[])field(picker,"swatches");
        swatches[1].performClick();
        call(picker,"change",new Class<?>[]{int.class,int.class},6,64);
        assertEquals(0x40ffffff,((int[])field(picker,"colors"))[1]);
        assertEquals(0,result[1]);
        assertTrue(dialog.isShowing());
      } catch(Exception e){throw new AssertionError(e);}
      finally {if(dialog!=null)dialog.dismiss();activity.finish();}
    });
  }
}
