package art.velyntora.core;
import static org.junit.Assert.*;
import android.app.AlertDialog;
import android.graphics.*;
import android.os.Bundle;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.*;
import org.junit.*;
public final class TextQualityDeviceTests {
 final TextElementDeviceTests t=new TextElementDeviceTests();final PlacedImageDeviceTests p=t.p;
 @Before public void open(){t.open();}
 @After public void close()throws Exception{t.close();}
 @Test public void slidersPreviewImmediatelyWithoutChangingTheDocument()throws Exception{
  AlertDialog[] dialog={null};long revision=DrawingView.nativeRevision();p.inst.runOnMainSync(()->{
   dialog[0]=new TextDialog(p.host).show(t.spec("hola"),70,90,false);View root=dialog[0].getWindow().getDecorView();((EditText)p.find(root,"Contenido del texto")).setText("hola");
   SeekBar size=(SeekBar)p.find(root,"Tamaño del texto en píxeles");SeekBar width=(SeekBar)p.find(root,"Ancho máximo del texto en píxeles");assertNotNull(size);assertNotNull(width);size.setProgress(500);width.setProgress(900);
   TextDraftPreview preview=(TextDraftPreview)p.find(root,"Vista previa del texto");assertTrue(preview.spec.size>100);assertEquals(908,preview.spec.wrapWidth);assertEquals(revision,DrawingView.nativeRevision());assertFalse(t.session().active());dialog[0].getButton(-2).performClick();
  });assertEquals(1,DrawingView.nativeLayerCount());
 }
 @Test public void textColorPickerDoesNotChangeBrushOrCommittedText()throws Exception{
  t.apply(t.spec("hola"),false);p.confirmImage();p.editImage(1);int brush=p.host.readDrawing().readColor();AlertDialog[] dialog={null};
  p.inst.runOnMainSync(()->{dialog[0]=TextDialog.edit(p.host);p.find(dialog[0].getWindow().getDecorView(),"Elegir color del texto").performClick();});p.inst.waitForIdleSync();
  AccessibilityNodeInfo input=find(p.inst.getUiAutomation().getRootInActiveWindow(),"Hexadecimal RGBA");assertNotNull(input);Bundle args=new Bundle();args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,"FF0000FF");assertTrue(input.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,args));p.dialogButton("Aceptar");
  p.inst.runOnMainSync(()->{TextDraftPreview preview=(TextDraftPreview)p.find(dialog[0].getWindow().getDecorView(),"Vista previa del texto");assertEquals(Color.RED,preview.spec.color);assertEquals(brush,p.host.readDrawing().readColor());assertEquals(0xff102030,t.session().text.color);dialog[0].getButton(-2).performClick();t.session().cancel();});assertEquals(0xff102030,TextSpec.read(1).color);
 }
 @Test public void resizedTextUsesSharpGlyphsAndReportsEffectivePixelSize()throws Exception{
  t.apply(new TextSpec("Hola",32,Color.BLACK,0,0,500),false);p.confirmImage();p.editImage(1);Bitmap expected=Bitmap.createBitmap(800,800,Bitmap.Config.ARGB_8888);expected.eraseColor(Color.WHITE);
  p.inst.runOnMainSync(()->{PlacedImageSession s=t.session();s.state.width*=4;s.state.height*=4;s.state.cx=350;s.state.cy=350;s.notifyChanged();assertEquals(128,TextSpec.effective(s.text,s.state).size);
   AlertDialog dialog=TextDialog.edit(p.host);TextDraftPreview preview=(TextDraftPreview)p.find(dialog.getWindow().getDecorView(),"Vista previa del texto");assertTrue(Math.abs(128-preview.spec.size)<=1);dialog.getButton(-2).performClick();
   Canvas canvas=new Canvas(expected);canvas.concat(s.state.matrix());canvas.clipRect(s.state.crop);new TextGlyphLayout(s.text).draw(canvas);
   Bitmap live=Bitmap.createBitmap(800,800,Bitmap.Config.ARGB_8888);try{assertNotNull(s.editor.vector);assertTrue(s.editor.draw(new Canvas(live)));compare(expected,live);}finally{live.recycle();}
  });p.confirmImage();int[] result=DrawingView.nativeRegionPixels(0,0,800,800);Bitmap actual=Bitmap.createBitmap(result,800,800,Bitmap.Config.ARGB_8888);try{compare(expected,actual);}finally{actual.recycle();expected.recycle();}
 }
 @Test public void toolbarControlsHaveSeparateMargins()throws Exception{
  t.apply(t.spec("hola"),false);p.inst.runOnMainSync(()->{View button=p.find(p.host.getWindow().getDecorView(),"Editar texto");LinearLayout row=(LinearLayout)button.getParent();for(int i=0;i<row.getChildCount();i++){View child=row.getChildAt(i);LinearLayout.LayoutParams params=(LinearLayout.LayoutParams)child.getLayoutParams();assertTrue(params.rightMargin>=p.host.dp(12));}t.session().cancel();});
 }
 static void compare(Bitmap a,Bitmap b){int[] ap=new int[800*800],bp=new int[800*800];a.getPixels(ap,0,800,0,0,800,800);b.getPixels(bp,0,800,0,0,800,800);int wrong=0;for(int i=0;i<ap.length;i++){int p=ap[i],q=bp[i];for(int shift:new int[]{0,8,16,24})if(Math.abs((p>>>shift&255)-(q>>>shift&255))>3){wrong++;break;}}assertTrue("Glyph raster differs in "+wrong+" pixels",wrong<30);}
 static AccessibilityNodeInfo find(AccessibilityNodeInfo node,String name){if(node==null)return null;if(name.contentEquals(node.getContentDescription()==null?"":node.getContentDescription()))return node;for(int i=0;i<node.getChildCount();i++){AccessibilityNodeInfo result=find(node.getChild(i),name);if(result!=null)return result;}return null;}
}
