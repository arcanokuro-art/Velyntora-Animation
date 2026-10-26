package art.velyntora.core;
import static org.junit.Assert.*;
import android.app.AlertDialog;
import android.graphics.*;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.widget.EditText;
import java.io.*;
import org.junit.*;

public final class TextElementDeviceTests {
  final PlacedImageDeviceTests p=new PlacedImageDeviceTests();
  @Before public void open(){p.open();}
  @After public void close()throws Exception{p.close();}
  PlacedImageSession session(){return p.host.readDrawing().placedImage();}
  void waitWork()throws Exception{LargeImportResponsivenessTests.waitResponsive(p.inst,p.host);}
  TextSpec spec(String content){return new TextSpec(content,48,0xff102030,0,0,600);}
  void apply(TextSpec spec,boolean editing)throws Exception{p.inst.runOnMainSync(()->session().applyText(spec,70,90,editing));waitWork();}
  void set(View root,String label,String value){View input=p.find(root,label);assertNotNull(label,input);((EditText)input).setText(value);}
  @Test public void dialogCreatesIndependentTextLayerAndReopensWriting()throws Exception {
    AlertDialog[] dialog={null};p.inst.runOnMainSync(()->{p.host.readDrawing().setTool(DrawingView.TEXT);dialog[0]=new TextDialog(p.host).show(spec("hola"),70,90,false);set(dialog[0].getWindow().getDecorView(),"Contenido del texto","hola");dialog[0].getButton(-1).performClick();});waitWork();
    p.inst.runOnMainSync(()->{assertEquals("hola",session().text.content);assertEquals(1,p.host.readDrawing().layerCount());assertEquals(View.VISIBLE,p.find(p.host.getWindow().getDecorView(),"Editar texto").getVisibility());assertEquals(View.GONE,p.find(p.host.getWindow().getDecorView(),"Recortar imagen").getVisibility());});p.confirmImage();
    assertEquals("hola",TextSpec.read(1).content);assertEquals(2,DrawingView.nativeLayerCount());p.editImage(1);
    p.inst.runOnMainSync(()->{dialog[0]=TextDialog.edit(p.host);assertNotNull(dialog[0]);View root=dialog[0].getWindow().getDecorView();assertEquals("hola",((EditText)p.find(root,"Contenido del texto")).getText().toString());set(root,"Contenido del texto","hola, mundo");dialog[0].getButton(-1).performClick();});waitWork();p.confirmImage();
    assertEquals("hola, mundo",TextSpec.read(1).content);assertEquals(2,DrawingView.nativeLayerCount());
  }
  @Test public void cancelAndLocalUndoRestoreWordsFormattingAndPixels()throws Exception {
    apply(spec("hola"),false);p.confirmImage();long revision=DrawingView.nativeRevision();int[] pixels=DrawingView.nativeRegionPixels(0,0,800,800);p.editImage(1);
    apply(new TextSpec("otra frase\n日本語 😀",64,0xffcc1122,1,3,500),true);
    p.inst.runOnMainSync(()->{assertEquals("otra frase\n日本語 😀",session().text.content);session().undo();assertEquals("hola",session().text.content);assertEquals(48,session().text.size);session().redo();assertEquals(64,session().text.size);assertEquals("hola",TextSpec.read(1).content);session().cancel();});
    assertEquals(revision,DrawingView.nativeRevision());assertArrayEquals(pixels,DrawingView.nativeRegionPixels(0,0,800,800));assertEquals("hola",TextSpec.read(1).content);
  }
  @Test public void confirmedChangesHistoryDuplicateAndNativeProjectKeepUnicode()throws Exception {
    apply(spec("hola"),false);p.confirmImage();p.editImage(1);TextSpec changed=new TextSpec("Adiós\n日本語 😀",56,0xffe02030,2,3,460);apply(changed,true);
    p.inst.runOnMainSync(()->{session().rotate();session().state.opacity=.5f;session().notifyChanged();});p.confirmImage();
    p.inst.runOnMainSync(p.host::undo);waitWork();assertEquals("hola",TextSpec.read(1).content);p.inst.runOnMainSync(p.host::redo);waitWork();assertEquals(changed.content,TextSpec.read(1).content);
    assertTrue(DrawingView.nativeLayerAction(0));assertEquals(changed.content,TextSpec.read(2).content);
    File project=new File(p.host.getCacheDir(),"text-element.vly");
    try{
      try(ParcelFileDescriptor fd=ParcelFileDescriptor.open(project,ParcelFileDescriptor.MODE_CREATE|ParcelFileDescriptor.MODE_WRITE_ONLY|ParcelFileDescriptor.MODE_TRUNCATE)){assertTrue(DrawingView.nativeSaveProject(fd.getFd()));}
      p.inst.runOnMainSync(()->p.host.readDrawing().newDocument(64,64));
      try(ParcelFileDescriptor fd=ParcelFileDescriptor.open(project,ParcelFileDescriptor.MODE_READ_ONLY)){assertTrue(DrawingView.nativeOpenProject(fd.getFd()));}
      p.inst.runOnMainSync(()->p.host.readDrawing().projectOpened());p.editImage(2);
      p.inst.runOnMainSync(()->{assertEquals(changed.content,session().text.content);assertArrayEquals(changed.values(),session().text.values());assertEquals(90,session().state.angle,.001);assertEquals(.5f,session().state.opacity,.001);session().cancel();});
    }finally{project.delete();}
  }
  @Test public void geometryDoesNotChangeTextAndRewritingKeepsAnchor()throws Exception {
    apply(spec("hola"),false);p.confirmImage();p.editImage(1);
    float[][] anchor={null};p.inst.runOnMainSync(()->{session().state.angle=27;session().state.cx=350;session().state.cy=300;session().state.width*=2;session().state.height*=2;anchor[0]=new float[]{0,0};session().state.matrix().mapPoints(anchor[0]);});
    apply(spec("hola, esta frase ahora es más larga"),true);
    p.inst.runOnMainSync(()->{float[] point={0,0};session().state.matrix().mapPoints(point);assertArrayEquals(anchor[0],point,.01f);assertEquals(27,session().state.angle,.001);});p.confirmImage();assertEquals("hola, esta frase ahora es más larga",TextSpec.read(1).content);
  }
  @Test public void emptyDialogAndTooTallTextLeaveDocumentUntouched()throws Exception {
    AlertDialog[] dialog={null};p.inst.runOnMainSync(()->{dialog[0]=new TextDialog(p.host).show(spec("hola"),70,90,false);set(dialog[0].getWindow().getDecorView(),"Contenido del texto","");dialog[0].getButton(-1).performClick();assertTrue(dialog[0].isShowing());dialog[0].getButton(-2).performClick();assertFalse(session().active());});
    apply(new TextSpec("a\n".repeat(1000),256,Color.BLACK,0,0,400),false);
    p.inst.runOnMainSync(()->{assertFalse(session().active());assertEquals(1,p.host.readDrawing().layerCount());});
  }
  @Test public void textToolCanAddWritingOverAnExistingImage()throws Exception {
    p.load(400,300);p.confirmImage();p.inst.runOnMainSync(()->p.host.readDrawing().setTool(DrawingView.TEXT));p.gesture(400,400,400,400);p.inst.waitForIdleSync();
    android.view.accessibility.AccessibilityNodeInfo root=p.inst.getUiAutomation().getRootInActiveWindow();assertNotNull(root);assertTrue(hasDescription(root,"Contenido del texto"));assertFalse(session().active());p.dialogButton("Cancelar");assertEquals(2,DrawingView.nativeLayerCount());assertNull(TextSpec.read(1));
  }
  boolean hasDescription(android.view.accessibility.AccessibilityNodeInfo node,String description){
    if(description.contentEquals(node.getContentDescription()==null?"":node.getContentDescription()))return true;
    for(int i=0;i<node.getChildCount();i++){android.view.accessibility.AccessibilityNodeInfo child=node.getChild(i);if(child!=null && hasDescription(child,description))return true;}return false;
  }
}
