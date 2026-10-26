package art.velyntora.core;
import static org.junit.Assert.*;
import android.app.AlertDialog;
import android.view.*;
import android.widget.*;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
public final class LayersDeviceTests {
 private static <T> T find(View view,Class<T> type){if(type.isInstance(view))return type.cast(view);if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){T result=find(group.getChildAt(i),type);if(result!=null)return result;}}return null;}

 @Test public void propertiesCancelAcceptAndUndoAreAtomic() throws Exception {
  android.app.Instrumentation inst=InstrumentationRegistry.getInstrumentation();
  android.content.Intent intent=new android.content.Intent(inst.getTargetContext(),MainActivity.class);
  intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
  MainActivity activity=(MainActivity)inst.startActivitySync(intent);
  java.util.concurrent.atomic.AtomicReference<AlertDialog> dialog=new java.util.concurrent.atomic.AtomicReference<>();
  String[] original={null};int[] index={0};
  try {
   inst.runOnMainSync(()->{
    DrawingView drawing=activity.readDrawing();assertTrue(drawing.addLayer());index[0]=drawing.activeLayer();original[0]=drawing.layerName(index[0]);
    dialog.set(LayerPropertiesDialog.show(activity,index[0]));
   });inst.waitForIdleSync();
   inst.runOnMainSync(()->{
    View root=dialog.get().getWindow().getDecorView();find(root,EditText.class).setText("Prueba");find(root,Spinner.class).setSelection(8);dialog.get().getButton(-2).performClick();
   });inst.waitForIdleSync();
   inst.runOnMainSync(()->{
    assertEquals(original[0],activity.readDrawing().layerName(index[0]));assertEquals(0,activity.readDrawing().layerBlendMode(index[0]));
    dialog.set(LayerPropertiesDialog.show(activity,index[0]));
   });inst.waitForIdleSync();
   inst.runOnMainSync(()->{
    View root=dialog.get().getWindow().getDecorView();find(root,EditText.class).setText("Prueba");find(root,Spinner.class).setSelection(8);find(root,CheckBox.class).setChecked(false);
    LinearLayout opacityRow=(LinearLayout)find(root,SeekBar.class).getParent();((EditText)opacityRow.getChildAt(0)).setText("50");dialog.get().getButton(-1).performClick();
   });LargeImportResponsivenessTests.waitResponsive(inst,activity);inst.waitForIdleSync();
   inst.runOnMainSync(()->{
    DrawingView drawing=activity.readDrawing();assertFalse("Accept must dismiss the dialog",dialog.get().isShowing());
    assertEquals("Prueba",drawing.layerName(index[0]));assertEquals(8,drawing.layerBlendMode(index[0]));assertFalse("Visibility must be applied",drawing.layerVisible(index[0]));assertEquals(.5f,drawing.layerOpacity(),.001f);
    drawing.undo();assertEquals(original[0],drawing.layerName(index[0]));assertEquals(0,drawing.layerBlendMode(index[0]));assertTrue(drawing.layerVisible(index[0]));assertEquals(1f,drawing.layerOpacity(),.001f);
    int count=drawing.layerCount();assertTrue(drawing.layerAction(0));assertEquals(count+1,drawing.layerCount());assertTrue(drawing.layerAction(1));assertEquals(count,drawing.layerCount());
    activity.refreshLayerPanel();LinearLayout items=activity.readLayerItems();assertEquals(count,items.getChildCount());LinearLayout row=(LinearLayout)items.getChildAt(0);assertEquals(3,row.getChildCount());assertTrue(row.getChildAt(0) instanceof ImageView);assertTrue(row.getChildAt(1) instanceof TextView);assertTrue(row.getChildAt(2) instanceof CheckBox);
   });
  }finally{inst.runOnMainSync(()->{if(dialog.get()!=null)dialog.get().dismiss();activity.finish();});}
 }

 @Test public void captureLayersAndProperties() throws Exception {
  android.app.Instrumentation inst=InstrumentationRegistry.getInstrumentation();android.content.Intent intent=new android.content.Intent(inst.getTargetContext(),MainActivity.class);intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);MainActivity activity=(MainActivity)inst.startActivitySync(intent);
  java.util.concurrent.atomic.AtomicReference<AlertDialog> dialog=new java.util.concurrent.atomic.AtomicReference<>();
  try{
   inst.runOnMainSync(()->activity.setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));
   long deadline=android.os.SystemClock.uptimeMillis()+5000;while(activity.getResources().getConfiguration().orientation!=2&&android.os.SystemClock.uptimeMillis()<deadline)android.os.SystemClock.sleep(50);inst.waitForIdleSync();
   inst.runOnMainSync(()->{for(int i=activity.readDrawing().layerCount();i<12;i++)assertTrue(activity.readDrawing().addLayer());activity.refreshLayerPanel();activity.readToolScroll().setVisibility(View.GONE);activity.readLayerScroll().setVisibility(View.VISIBLE);});inst.waitForIdleSync();
   android.os.SystemClock.sleep(150);capture(inst,"layers-panel");
   inst.runOnMainSync(()->{
    ScrollView list=(ScrollView)activity.readLayerItems().getParent();LinearLayout panel=(LinearLayout)list.getParent();
    HorizontalScrollView commands=(HorizontalScrollView)panel.getChildAt(2);
    LinearLayout buttons=(LinearLayout)commands.getChildAt(0);
    assertEquals(LinearLayout.HORIZONTAL,buttons.getOrientation());assertEquals(6,buttons.getChildCount());
    String[] labels={"Añadir una capa nueva","Eliminar capa","Duplicar la capa","Combinar con la capa inferior","Subir capa","Bajar capa"};
    for(int i=0;i<6;i++){
     Button button=(Button)buttons.getChildAt(i);
     assertEquals(labels[i],button.getContentDescription());
     assertTrue(button.getWidth()>=activity.dp(32));assertTrue(button.getHeight()>=activity.dp(48));
     assertEquals(buttons.getChildAt(0).getTop(),button.getTop());
     assertEquals(activity.dp(20),button.getCompoundDrawables()[0].getBounds().width());
     assertEquals(0,button.getCompoundDrawablePadding());
     assertCenteredIcon(button,activity.dp(20));
     if(i>0)assertTrue(button.getLeft()-buttons.getChildAt(i-1).getRight()>=activity.dp(4));
    }
    assertTrue(buttons.getChildAt(0).getLeft()>=activity.dp(8));
    assertTrue(buttons.getWidth()-buttons.getChildAt(5).getRight()>=activity.dp(8));
    assertTrue("All six actions should fit the normal panel",buttons.getWidth()<=commands.getWidth());
    assertTrue("List must have a viewport",list.getHeight()>0);
    assertTrue("Actions must stay inside panel",commands.getBottom()<=panel.getHeight());list.fullScroll(View.FOCUS_DOWN);
   });inst.waitForIdleSync();android.os.SystemClock.sleep(150);capture(inst,"layers-panel-bottom");
   inst.runOnMainSync(()->dialog.set(LayerPropertiesDialog.show(activity,activity.readDrawing().activeLayer())));inst.waitForIdleSync();android.os.SystemClock.sleep(150);capture(inst,"layers-properties");
  }finally{inst.runOnMainSync(()->{if(dialog.get()!=null)dialog.get().dismiss();activity.finish();});}
 }

 private static void assertCenteredIcon(Button button,int iconSize){
  android.graphics.Bitmap image=android.graphics.Bitmap.createBitmap(button.getWidth(),button.getHeight(),android.graphics.Bitmap.Config.ARGB_8888);
  android.graphics.drawable.Drawable background=button.getBackground();
  try{
   button.setBackground(null);button.draw(new android.graphics.Canvas(image));
   int left=image.getWidth(),right=-1;
   for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++)if((image.getPixel(x,y)>>>24)>0){left=Math.min(left,x);right=Math.max(right,x);}
   assertTrue("Icon must be visible",right>=left);
   assertTrue("Icon must keep its size",right-left+1<=iconSize+2);
   assertTrue("Icon must be centered in its cell",Math.abs(left+right+1-button.getWidth())<=3);
  }finally{button.setBackground(background);image.recycle();}
 }

 private void capture(android.app.Instrumentation inst,String name)throws Exception{
  String directory="/sdcard/Download/velyntora-core-workspace";
  for(String command:new String[]{"mkdir -p "+directory,"screencap -p "+directory+"/"+name+".png"})try(android.os.ParcelFileDescriptor result=inst.getUiAutomation().executeShellCommand(command);java.io.FileInputStream input=new java.io.FileInputStream(result.getFileDescriptor())){byte[] buffer=new byte[1024];while(input.read(buffer)!=-1){}}
 }
}
