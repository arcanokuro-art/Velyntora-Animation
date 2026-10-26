package art.velyntora.core;

import static org.junit.Assert.*;
import android.app.Instrumentation;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;

public final class SavedPaletteDeviceTests {
  Instrumentation inst;MainActivity activity;
  @Before public void open(){
    inst=InstrumentationRegistry.getInstrumentation();
    inst.getTargetContext().getSharedPreferences("saved-color-palette",0).edit().clear().commit();
    launch();
  }
  void launch(){
    activity=(MainActivity)inst.startActivitySync(new Intent(inst.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    inst.waitForIdleSync();
  }
  @After public void close(){
    inst.runOnMainSync(()->activity.finish());inst.waitForIdleSync();
    inst.getTargetContext().getSharedPreferences("saved-color-palette",0).edit().clear().commit();
  }
  View find(View root,String label){
    if(label.contentEquals(root.getContentDescription()==null?"":root.getContentDescription()))return root;
    if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;for(int i=0;i<group.getChildCount();i++){View v=find(group.getChildAt(i),label);if(v!=null)return v;}}
    return null;
  }
  View find(String label){return find(activity.getWindow().getDecorView(),label);}
  void choose(String text){
    inst.waitForIdleSync();AccessibilityNodeInfo root=inst.getUiAutomation().getRootInActiveWindow();assertNotNull(root);
    java.util.List<AccessibilityNodeInfo> nodes=root.findAccessibilityNodeInfosByText(text);assertFalse(nodes.isEmpty());
    AccessibilityNodeInfo node=nodes.get(0);while(node!=null&&!node.isClickable())node=node.getParent();
    assertNotNull(node);assertTrue(node.performAction(AccessibilityNodeInfo.ACTION_CLICK));inst.waitForIdleSync();
  }
  @Test public void saveAndReuseArgbAcrossActivityRestart(){
    inst.runOnMainSync(()->{
      activity.setActiveColor(0x8034abef);assertTrue(find("Guardar color activo").performClick());
      activity.setActiveColor(0xffff0000);assertTrue(find("Guardar color activo").performClick());
      assertTrue(find("Guardar color activo").performClick());
      assertEquals(2,new SavedPaletteStore(activity).load().colors().size());
      assertNotNull(find("Color guardado #8034ABEF"));assertTrue(find("Color guardado #8034ABEF").performClick());
      assertEquals(0x8034abef,activity.readActiveColor());
      activity.finish();
    });inst.waitForIdleSync();launch();
    inst.runOnMainSync(()->{
      assertNotNull(find("Color guardado #8034ABEF"));assertNotNull(find("Color guardado #FFFF0000"));
      assertTrue(find("Color guardado #8034ABEF").performClick());assertEquals(0x8034abef,activity.readActiveColor());
      assertTrue(activity.readDrawing().newDocument(64,64));
      assertEquals(2,new SavedPaletteStore(activity).load().colors().size());
    });
    capture();
  }
  @Test public void savedSwatchOffersSecondaryAndRemoval(){
    inst.runOnMainSync(()->{activity.setActiveColor(0xff56789a);find("Guardar color activo").performClick();find("Color guardado #FF56789A").performLongClick();});
    choose("Usar como color secundario");
    inst.runOnMainSync(()->{assertEquals(0xff56789a,activity.readSecondaryColor());assertEquals(0xff56789a,activity.readDrawing().secondaryColor);find("Color guardado #FF56789A").performLongClick();});
    choose("Eliminar color guardado");
    inst.runOnMainSync(()->{
      assertNull(find("Color guardado #FF56789A"));assertTrue(new SavedPaletteStore(activity).load().colors().isEmpty());
      assertEquals(0xff56789a,activity.readSecondaryColor());
    });
  }
  @Test public void saveSecondaryAndTransparentColor(){
    inst.runOnMainSync(()->{activity.setSecondaryColor(0x0034abef);find("Guardar color activo").performLongClick();});
    choose("Guardar color secundario");
    inst.runOnMainSync(()->{
      assertNotNull(find("Color guardado #0034ABEF"));find("Color guardado #0034ABEF").performClick();assertEquals(0x0034abef,activity.readActiveColor());
      find("Guardar color activo").performClick();assertEquals(1,new SavedPaletteStore(activity).load().colors().size());
    });
  }
  void capture(){
    inst.runOnMainSync(()->activity.readPaletteScroll().fullScroll(View.FOCUS_RIGHT));inst.waitForIdleSync();
    try{inst.getUiAutomation().waitForIdle(100,2000);}catch(java.util.concurrent.TimeoutException ignored){}
    String dir="/sdcard/Download/velyntora-core-workspace";
    for(String cmd:new String[]{"mkdir -p "+dir,"screencap -p "+dir+"/saved-colors.png"}){
      try(android.os.ParcelFileDescriptor result=inst.getUiAutomation().executeShellCommand(cmd);java.io.FileInputStream input=new java.io.FileInputStream(result.getFileDescriptor())){
        byte[] buffer=new byte[1024];while(input.read(buffer)!=-1){}
      }catch(java.io.IOException e){throw new AssertionError(e);}
    }
  }
}
