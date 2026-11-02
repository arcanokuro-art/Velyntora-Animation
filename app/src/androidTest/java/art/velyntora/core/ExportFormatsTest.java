package art.velyntora.core;
import static org.junit.Assert.*;
import android.app.*;import android.content.*;import android.graphics.*;import android.media.*;
import android.os.*;import android.view.*;import android.view.accessibility.AccessibilityNodeInfo;import android.widget.*;
import androidx.test.platform.app.InstrumentationRegistry;
import app.velyntora.animation.export.*;import java.io.*;import java.util.concurrent.atomic.AtomicBoolean;import java.util.function.BooleanSupplier;import java.util.zip.*;import org.junit.Test;
public final class ExportFormatsTest {
 private final android.app.Instrumentation i=InstrumentationRegistry.getInstrumentation();
 private AnimationSession session(AnimationActivity host){
  try{java.lang.reflect.Field field=AnimationActivity.class.getDeclaredField("session");field.setAccessible(true);return (AnimationSession)field.get(host);}catch(Exception e){throw new AssertionError(e);}
 }
 private boolean busy(AnimationActivity host){
  try{java.lang.reflect.Field field=AnimationActivity.class.getDeclaredField("busy");field.setAccessible(true);return field.getBoolean(host);}catch(Exception e){throw new AssertionError(e);}
 }
 private void await(BooleanSupplier condition){
  long deadline=SystemClock.uptimeMillis()+30000;AtomicBoolean value=new AtomicBoolean();
  do{i.runOnMainSync(()->value.set(condition.getAsBoolean()));if(value.get())return;SystemClock.sleep(30);}while(SystemClock.uptimeMillis()<deadline);
  throw new AssertionError("Export test timed out");
 }
 private Button button(View view,String description){
  if(view instanceof Button&&view.getContentDescription()!=null&&description.contentEquals(view.getContentDescription()))return (Button)view;
  if(view instanceof ViewGroup)for(int n=0;n<((ViewGroup)view).getChildCount();n++){
   Button found=button(((ViewGroup)view).getChildAt(n),description);if(found!=null)return found;
  }return null;
 }
 private boolean text(AccessibilityNodeInfo node,String value){
  if(node==null)return false;if(node.getText()!=null&&value.contentEquals(node.getText()))return true;
  for(int n=0;n<node.getChildCount();n++)if(text(node.getChild(n),value))return true;return false;
 }
 private void paint(int color){DrawingView.nativeBeginEdit();DrawingView.nativeStroke(30,24,34,24,100,color);}
 @Test public void saveArrowAndEveryFormatProduceRealFilesAndKeepHistory()throws Exception{
  AnimationActivity host=(AnimationActivity)i.startActivitySync(new Intent(i.getTargetContext(),AnimationActivity.class)
    .setAction(AnimationActivity.ACTION).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("width",64).putExtra("height",48).putExtra("fps",4));
  try{
   await(()->!busy(host)&&session(host).project()!=null);
   i.runOnMainSync(()->{Button save=button(host.getWindow().getDecorView(),"Guardar");assertNotNull(save);save.performClick();});
   i.waitForIdleSync();
   for(ExportKind kind:ExportKind.values())assertTrue(kind.label,text(i.getUiAutomation().getRootInActiveWindow(),kind.label));
   i.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);i.waitForIdleSync();
   AnimationSession session=session(host);
   paint(0xffff0000);
   session.mutate(s->s.insert(false),true,false);
   paint(0xff0000ff);
   session.mutate(s->s.select(0),false,true);
   session.mutate(s->s.exposure(2),false,false);
   int[] original=DrawingView.nativePixels();int availability=DrawingView.nativeHistoryAvailability();
   for(ExportKind kind:ExportKind.values()){
    File file=new File(i.getTargetContext().getCacheDir(),"export-test."+kind.extension);
    try{
     session.exportFile(new ExportRequest(kind,64,48,500000),file,new AtomicBoolean(),n->{});
     assertTrue(kind.name(),file.length()>16);
     if(kind.video()){
      MediaExtractor extractor=new MediaExtractor();
      try{
       extractor.setDataSource(file.getAbsolutePath());assertEquals(kind.name(),1,extractor.getTrackCount());
       MediaFormat format=extractor.getTrackFormat(0);assertEquals(64,format.getInteger(MediaFormat.KEY_WIDTH));assertEquals(48,format.getInteger(MediaFormat.KEY_HEIGHT));
       extractor.selectTrack(0);int samples=0;long previous=-1;
       do{long at=extractor.getSampleTime();if(at<0)break;assertTrue(at>previous);previous=at;samples++;}while(extractor.advance());
       assertEquals(kind.name(),3,samples);assertEquals(500000,previous);
      }finally{extractor.release();}
      MediaMetadataRetriever decoder=new MediaMetadataRetriever();
      try{decoder.setDataSource(file.getAbsolutePath());Bitmap image=decoder.getFrameAtTime(0,MediaMetadataRetriever.OPTION_CLOSEST);assertNotNull(kind.name(),image);
       int color=image.getPixel(32,24);assertTrue(kind.name(),Color.red(color)>180&&Color.blue(color)<60);image.recycle();
      }finally{decoder.release();}
     }else if(kind==ExportKind.PROJECT||kind==ExportKind.PNG_SEQUENCE){
      try(ZipFile zip=new ZipFile(file)){assertNotNull(zip.getEntry(kind==ExportKind.PROJECT?"project.properties":"timing.csv"));}
     }else{
      if(kind==ExportKind.GIF){
        android.graphics.Movie movie=android.graphics.Movie.decodeFile(file.getAbsolutePath());
        assertNotNull(movie);assertEquals(750,movie.duration());
        Bitmap second=Bitmap.createBitmap(64,48,Bitmap.Config.ARGB_8888);
        movie.setTime(600);movie.draw(new Canvas(second),0,0);
        assertTrue(Color.blue(second.getPixel(32,24))>180);second.recycle();
      }
      if(kind==ExportKind.APNG)verifyApng(file);
      Bitmap image=BitmapFactory.decodeFile(file.getAbsolutePath());assertNotNull(kind.name(),image);assertEquals(64,image.getWidth());assertEquals(48,image.getHeight());image.recycle();
     }
     assertArrayEquals(kind.name(),original,DrawingView.nativePixels());assertEquals(availability,DrawingView.nativeHistoryAvailability());
    }finally{file.delete();}
   }
   AtomicBoolean canceled=new AtomicBoolean(true);File file=new File(i.getTargetContext().getCacheDir(),"canceled.mp4");
   try{session.exportFile(new ExportRequest(ExportKind.MP4,64,48,500000),file,canceled,n->{});fail("Expected cancellation");}
   catch(java.util.concurrent.CancellationException expected){}
   assertArrayEquals(original,DrawingView.nativePixels());assertEquals(availability,DrawingView.nativeHistoryAvailability());
  }finally{if(!host.isDestroyed()){i.runOnMainSync(host::onBackPressed);await(host::isDestroyed);}}
 }

 private void verifyApng(File file)throws Exception{
  try(DataInputStream in=new DataInputStream(new FileInputStream(file))){
   byte[] signature=new byte[8];in.readFully(signature);
   int controls=0;java.util.ArrayList<ByteArrayOutputStream> streams=new java.util.ArrayList<>();
   while(true){
    int length=in.readInt();assertTrue(length>=0&&length<1000000);
    byte[] tag=new byte[4],data=new byte[length];in.readFully(tag);in.readFully(data);
    CRC32 crc=new CRC32();crc.update(tag);crc.update(data);assertEquals((int)crc.getValue(),in.readInt());
    String type=new String(tag,"US-ASCII");
    if(type.equals("fcTL")){
     java.nio.ByteBuffer control=java.nio.ByteBuffer.wrap(data);
     assertEquals(controls==0?2:1,control.getShort(20));
     assertEquals(4,control.getShort(22));controls++;streams.add(new ByteArrayOutputStream());
    }
    if(type.equals("IDAT"))streams.get(0).write(data);
    if(type.equals("fdAT"))streams.get(streams.size()-1).write(data,4,data.length-4);
    if(type.equals("IEND"))break;
   }
   assertEquals(2,controls);
   for(int frame=0;frame<2;frame++){
    try(java.util.zip.InflaterInputStream z=new java.util.zip.InflaterInputStream(new ByteArrayInputStream(streams.get(frame).toByteArray()))){
     ByteArrayOutputStream raw=new ByteArrayOutputStream();byte[] bytes=new byte[1024];int n;while((n=z.read(bytes))!=-1)raw.write(bytes,0,n);
     byte[] rgba=raw.toByteArray();assertEquals((64*4+1)*48,rgba.length);
     int at=24*(64*4+1)+1+32*4;
     assertEquals(frame==0?255:0,rgba[at]&255);assertEquals(frame==1?255:0,rgba[at+2]&255);assertEquals(255,rgba[at+3]&255);
    }
   }
  }
 }
}
