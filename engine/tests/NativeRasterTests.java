package art.velyntora.core;
import java.util.Arrays;
class DrawingView {
 static {System.loadLibrary("velyntora_host");}
 static native String nativeLayerName(int index);
 static native boolean nativeRenameLayer(int index,String name);
 static native long nativeRevision();
 static native int nativeCurveTag();
 static native void nativeMarkCurve(int tag);
 static native void nativeBeginSampled(int x,int y);
 static native void nativeSampledStroke(boolean clone,int ox,int oy,int replacement,int tolerance,float x0,float y0,float x1,float y1,float radius,float opacity,float hardness);
 static native boolean nativeCreate(int w,int h);
 static native boolean nativeBeginImageImport(int w,int h);
 static native boolean nativeImageImportRegion(int x,int y,int w,int h,int[] pixels);
 static native boolean nativeFinishImageImport();
 static native void nativeAcceptImageImport();
 static native void nativeCancelImageImport();
 static native int nativeWidth();
 static native int nativeHeight();
 static native void nativeBeginEdit();
 static native boolean nativeFillSelection(int x,int y,int color,float opacity,byte[] selection);
 static native boolean nativeResizeDocumentOptions(int w,int h,boolean scale,boolean bilinear,int anchor);
 static native boolean nativeLoadBitmap(int w,int h,int[] pixels);
 static native boolean nativeAddLayer();
 static native boolean nativePasteSelection(int[] data,int x,int y);
 static native int[] nativePixels();
 static native int[] nativeRegionPixels(int x,int y,int w,int h);
 static native boolean nativeUndo();
 static native boolean nativeRedo();
 static native boolean nativeTransformSelection(int left,int top,int width,int height,byte[] mask,float degrees,float sx,float sy);
 static native boolean nativeSetEffectSelection(byte[] mask);
 static native boolean nativeUtility(int kind,int amount,int size,int parameter);
 static native boolean nativeObject(int kind,int amount,int tolerance,int color,boolean option);
 static native boolean nativeRender(int kind,int scale,int detail,int seed,int first,int second);
 static native boolean nativeArtistic(int kind,int strength,int radius,int threshold);
 static native boolean nativeDistortion(int kind,int amount,int size,int angle,int cx,int cy);
 static native boolean nativeBlur(int kind,int amount,int angle,int cx,int cy);
 static native boolean nativeColorAdjustment(int kind,int[] values);
 static native boolean nativeEffect(int kind,int amount);
 static native void nativeSetBrushSelection(byte[] mask);
 static native void nativeStyledStroke(float x0,float y0,float x1,float y1,float radius,int color,float opacity,float hardness,boolean square,boolean eraser);
}
public final class NativeRasterTests {
 static void check(boolean ok){if(!ok)throw new AssertionError();}
 public static void main(String[] args){
  check(DrawingView.nativeCreate(128,128));DrawingView.nativeBeginEdit();
  DrawingView.nativeStyledStroke(20,20,30,28,3,0xffff0000,1,1,false,false);
  int[] region=DrawingView.nativeRegionPixels(15,15,20,20), full=DrawingView.nativePixels();
  check(region.length==400);
  for(int row=0;row<20;row++)for(int col=0;col<20;col++)check(region[row*20+col]==full[(row+15)*128+col+15]);
  check(DrawingView.nativeRegionPixels(-1,0,2,2)==null);
  check(DrawingView.nativeUndo());check(DrawingView.nativePixels()[20*128+20]==0xffffffff);

  check(DrawingView.nativeCreate(1,1));long named=DrawingView.nativeRevision();String originalName=DrawingView.nativeLayerName(0);String unicodeName="Línea 🖌️ 水";
  check(DrawingView.nativeRenameLayer(0,unicodeName));check(unicodeName.equals(DrawingView.nativeLayerName(0)));long renamed=DrawingView.nativeRevision();check(renamed!=named);
  check(DrawingView.nativeRenameLayer(0,unicodeName));check(DrawingView.nativeRevision()==renamed);check(DrawingView.nativeUndo());check(originalName.equals(DrawingView.nativeLayerName(0)));check(DrawingView.nativeRevision()==named);check(DrawingView.nativeRedo());check(unicodeName.equals(DrawingView.nativeLayerName(0)));
  check(!DrawingView.nativeRenameLayer(0,""));check(!DrawingView.nativeRenameLayer(0,"x".repeat(4097)));check(!DrawingView.nativeRenameLayer(0,"nul\0name"));check(!DrawingView.nativeRenameLayer(-1,"x"));check(DrawingView.nativeRevision()==renamed);
  check(DrawingView.nativeLoadBitmap(5,1,new int[5]));long fillBefore=DrawingView.nativeRevision();
  check(DrawingView.nativeFillSelection(0,0,0xffff0000,.5f,new byte[]{1,1,0,1,1}));check(Arrays.equals(DrawingView.nativePixels(),new int[]{0x80ff0000,0x80ff0000,0,0,0}));
  check(DrawingView.nativeUndo());check(DrawingView.nativeRevision()==fillBefore);check(DrawingView.nativeRedo());long fillAfter=DrawingView.nativeRevision();
  check(!DrawingView.nativeFillSelection(2,0,0xffff0000,1,new byte[]{1,1,0,1,1}));check(!DrawingView.nativeFillSelection(0,0,0,1,null));check(DrawingView.nativeRevision()==fillAfter);
  check(DrawingView.nativeCreate(1,1));long clean=DrawingView.nativeRevision();
  check(!DrawingView.nativeResizeDocumentOptions(0,1,true,false,0));check(DrawingView.nativeRevision()==clean);
  check(DrawingView.nativePasteSelection(new int[]{1,1,0xff123456},0,0));long edited=DrawingView.nativeRevision();check(edited!=clean);
  check(DrawingView.nativeUndo());check(DrawingView.nativeRevision()==clean);check(DrawingView.nativeRedo());check(DrawingView.nativeRevision()==edited);
  check(DrawingView.nativeUndo());check(DrawingView.nativePasteSelection(new int[]{1,1,0xffabcdef},0,0));check(DrawingView.nativeRevision()!=edited);check(!DrawingView.nativeRedo());
  check(DrawingView.nativeCreate(1,1));check(DrawingView.nativeRevision()!=clean);
  check(DrawingView.nativeLoadBitmap(1,1,new int[]{0}));check(DrawingView.nativePasteSelection(new int[]{1,1,0xffff0000},0,0));DrawingView.nativeMarkCurve(7);check(DrawingView.nativeCurveTag()==7);
  check(DrawingView.nativeUndo());check(DrawingView.nativeCurveTag()==0);check(DrawingView.nativeRedo());check(DrawingView.nativeCurveTag()==7);
  DrawingView.nativeBeginEdit();DrawingView.nativeStyledStroke(.5f,.5f,.5f,.5f,.5f,0xff0000ff,1,1,true,false);check(DrawingView.nativeCurveTag()==0);check(DrawingView.nativeUndo());check(DrawingView.nativeCurveTag()==7);

  check(DrawingView.nativeLoadBitmap(1,1,new int[]{0}));check(DrawingView.nativeCurveTag()==0);check(!DrawingView.nativeUndo());
  int[] sampled={0xff112233,0xff445566,0,0};check(DrawingView.nativeLoadBitmap(4,1,sampled));DrawingView.nativeSetBrushSelection(null);
  DrawingView.nativeBeginEdit();DrawingView.nativeBeginSampled(2,0);DrawingView.nativeSampledStroke(true,-2,0,0,0,2.5f,.5f,3.5f,.5f,.5f,1,1);
  check(Arrays.equals(DrawingView.nativePixels(),new int[]{sampled[0],sampled[1],sampled[0],sampled[1]}));check(DrawingView.nativeUndo());check(Arrays.equals(DrawingView.nativePixels(),sampled));check(DrawingView.nativeRedo());
  check(DrawingView.nativeLoadBitmap(3,1,new int[]{0x80112233,0x80112233,0}));DrawingView.nativeSetBrushSelection(new byte[]{1,0,1});DrawingView.nativeBeginEdit();DrawingView.nativeBeginSampled(0,0);
  DrawingView.nativeSampledStroke(false,0,0,0xffff0000,0,.5f,.5f,2.5f,.5f,.5f,1,1);check(Arrays.equals(DrawingView.nativePixels(),new int[]{0x80ff0000,0x80112233,0}));check(DrawingView.nativeUndo());check(DrawingView.nativeRedo());

  check(DrawingView.nativeLoadBitmap(3,3,new int[9]));DrawingView.nativeSetBrushSelection(null);DrawingView.nativeBeginEdit();DrawingView.nativeStyledStroke(.5f,1.5f,2.5f,1.5f,.5f,0xff123456,1,1,true,false);
  check(Arrays.equals(DrawingView.nativePixels(),new int[]{0,0,0,0xff123456,0xff123456,0xff123456,0,0,0}));check(DrawingView.nativeUndo());check(Arrays.equals(DrawingView.nativePixels(),new int[9]));check(DrawingView.nativeRedo());

  check(DrawingView.nativeLoadBitmap(2,1,new int[]{0xffff0000,0}));
  check(DrawingView.nativeResizeDocumentOptions(3,1,true,true,4));check(Arrays.equals(DrawingView.nativePixels(),new int[]{0xffff0000,0x80ff0000,0}));
  check(DrawingView.nativeUndo());check(Arrays.equals(DrawingView.nativePixels(),new int[]{0xffff0000,0}));check(DrawingView.nativeRedo());
  int[] resized=DrawingView.nativePixels();check(!DrawingView.nativeResizeDocumentOptions(4,2,false,false,9));check(Arrays.equals(resized,DrawingView.nativePixels()));
  check(DrawingView.nativeResizeDocumentOptions(5,3,false,false,4));check(DrawingView.nativePixels()[6]==0xffff0000);check(DrawingView.nativeUndo());check(Arrays.equals(resized,DrawingView.nativePixels()));
  check(DrawingView.nativeLoadBitmap(4,3,new int[12]));
  check(DrawingView.nativeAddLayer());
  int[] before=DrawingView.nativePixels();
  check(DrawingView.nativePasteSelection(new int[]{2,2,0x80ff0000,0,0xff0000ff,0xff00ff00},1,1));
  int[] pasted=DrawingView.nativePixels();check(pasted[5]==0x80ff0000&&pasted[6]==0&&pasted[9]==0xff0000ff&&pasted[10]==0xff00ff00);
  check(DrawingView.nativeUndo());check(Arrays.equals(before,DrawingView.nativePixels()));
  check(DrawingView.nativeRedo());check(Arrays.equals(pasted,DrawingView.nativePixels()));
  check(!DrawingView.nativePasteSelection(new int[]{2,2,1},0,0));check(Arrays.equals(pasted,DrawingView.nativePixels()));
  check(!DrawingView.nativeLoadBitmap(4,3,new int[1]));check(Arrays.equals(pasted,DrawingView.nativePixels()));
  check(DrawingView.nativePasteSelection(new int[]{1,1,0x800000ff},1,1));
  int blended=DrawingView.nativePixels()[5];check((blended>>>24)==192);check(((blended>>>16)&255)>=84&&((blended>>>16)&255)<=86);
  check((blended&255)>=169&&(blended&255)<=171);
  check(DrawingView.nativeLoadBitmap(4,3,new int[12]));byte[] mask=new byte[12];mask[5]=1;
  DrawingView.nativeSetBrushSelection(mask);DrawingView.nativeStyledStroke(1.5f,1.5f,1.5f,1.5f,4,0xffffffff,1,1,false,false);
  int[] selected=DrawingView.nativePixels();check(selected[5]==0xffffffff&&selected[4]==0&&selected[6]==0);
  int[] red = {0,0xffff0000,0};check(DrawingView.nativeLoadBitmap(3,1,red));
  check(DrawingView.nativeEffect(0,1));check(DrawingView.nativePixels()[1]==0x55ff0000);
  check(DrawingView.nativeUndo());check(Arrays.equals(red,DrawingView.nativePixels()));
  check(DrawingView.nativeRedo());check(DrawingView.nativePixels()[1]==0x55ff0000);
  int[] stripes={0xffff0000,0xff00ff00,0xff0000ff};check(DrawingView.nativeLoadBitmap(3,1,stripes));
  check(DrawingView.nativeTransformSelection(0,0,3,1,new byte[]{1,1,1},0,-1,1));
  check(DrawingView.nativePixels()[0]==stripes[2]&&DrawingView.nativePixels()[2]==stripes[0]);
  check(DrawingView.nativeUndo());check(Arrays.equals(stripes,DrawingView.nativePixels()));
  check(DrawingView.nativeRedo());check(DrawingView.nativePixels()[0]==stripes[2]);
  int[] colors={0x80ff0000,0xff204080,0};check(DrawingView.nativeLoadBitmap(3,1,colors));
  int[] curve=new int[257];for(int i=0;i<256;i++)curve[i]=255-i;
  check(DrawingView.nativeColorAdjustment(0,curve));check(DrawingView.nativePixels()[0]==0x8000ffff);
  check(DrawingView.nativeUndo());check(Arrays.equals(colors,DrawingView.nativePixels()));
  check(DrawingView.nativeRedo());check(DrawingView.nativePixels()[0]==0x8000ffff);
  int[] adjusted=DrawingView.nativePixels();check(!DrawingView.nativeColorAdjustment(0,new int[2]));check(!DrawingView.nativeColorAdjustment(1,new int[15]));check(Arrays.equals(adjusted,DrawingView.nativePixels()));
  check(DrawingView.nativeLoadBitmap(3,1,colors));check(DrawingView.nativeColorAdjustment(4,new int[]{120,100,0}));check(DrawingView.nativePixels()[0]==0x8000ff00);check(DrawingView.nativeUndo());check(Arrays.equals(colors,DrawingView.nativePixels()));
  check(!DrawingView.nativeColorAdjustment(4,new int[]{0,100,0}));check(!DrawingView.nativeUndo());
  check(DrawingView.nativeColorAdjustment(1,new int[]{0,255,100,0,255,0,255,100,0,255,0,255,100,20,255}));check(DrawingView.nativeUndo());check(Arrays.equals(colors,DrawingView.nativePixels()));
  check(DrawingView.nativeColorAdjustment(2,new int[]{0}));check(DrawingView.nativeUndo());check(Arrays.equals(colors,DrawingView.nativePixels()));
  check(DrawingView.nativeColorAdjustment(3,new int[]{2,4,8}));check(DrawingView.nativeUndo());check(Arrays.equals(colors,DrawingView.nativePixels()));
  check(DrawingView.nativeColorAdjustment(5,new int[]{15,20}));check(DrawingView.nativeUndo());check(Arrays.equals(colors,DrawingView.nativePixels()));
  for(int kind=0;kind<3;kind++){check(DrawingView.nativeColorAdjustment(6,new int[]{kind}));check(DrawingView.nativeUndo());check(Arrays.equals(colors,DrawingView.nativePixels()));}
  int[] impulse={0,0xffff0000,0};check(DrawingView.nativeLoadBitmap(3,1,impulse));
  for(int kind=0;kind<4;kind++){check(DrawingView.nativeBlur(kind,kind==2?45:4,35,25,50));check(DrawingView.nativeUndo());check(Arrays.equals(impulse,DrawingView.nativePixels()));check(DrawingView.nativeRedo());check(DrawingView.nativeUndo());}
  check(!DrawingView.nativeBlur(0,33,0,50,50));check(Arrays.equals(impulse,DrawingView.nativePixels()));
  int[] pattern=new int[81];for(int y=0;y<9;y++)for(int x=0;x<9;x++)pattern[y*9+x]=0xff000000|x*25<<16|y*25<<8|((x+y)*12);check(DrawingView.nativeLoadBitmap(9,9,pattern));
  for(int kind=0;kind<5;kind++){check(DrawingView.nativeDistortion(kind,60,70,25,50,50));check(DrawingView.nativeUndo());check(Arrays.equals(pattern,DrawingView.nativePixels()));check(DrawingView.nativeRedo());check(DrawingView.nativeUndo());}
  byte[] effectMask=new byte[81];effectMask[40]=1;check(DrawingView.nativeSetEffectSelection(effectMask));check(DrawingView.nativeColorAdjustment(6,new int[]{1}));int[] masked=DrawingView.nativePixels();check(masked[40]!=pattern[40]);for(int i=0;i<81;i++)if(i!=40)check(masked[i]==pattern[i]);check(DrawingView.nativeUndo());check(Arrays.equals(pattern,DrawingView.nativePixels()));
  effectMask[40]=0;check(DrawingView.nativeSetEffectSelection(effectMask));check(!DrawingView.nativeBlur(0,4,0,50,50));check(!DrawingView.nativeDistortion(4,60,50,0,50,50));check(Arrays.equals(pattern,DrawingView.nativePixels()));check(DrawingView.nativeSetEffectSelection(null));check(!DrawingView.nativeSetEffectSelection(new byte[2]));
  check(DrawingView.nativeLoadBitmap(9,9,pattern));
  for(int kind=0;kind<7;kind++){if(kind==5){pattern[40]=0xffffffff;check(DrawingView.nativeLoadBitmap(9,9,pattern));}check(DrawingView.nativeArtistic(kind,kind==0?8:kind==5?50:100,2,20));check(DrawingView.nativeUndo());check(Arrays.equals(pattern,DrawingView.nativePixels()));check(DrawingView.nativeRedo());check(DrawingView.nativeUndo());}
  check(DrawingView.nativeSetEffectSelection(new byte[81]));check(!DrawingView.nativeArtistic(3,100,2,20));check(Arrays.equals(pattern,DrawingView.nativePixels()));check(DrawingView.nativeSetEffectSelection(null));check(!DrawingView.nativeArtistic(0,1,2,20));
  for(int kind=0;kind<5;kind++){check(DrawingView.nativeRender(kind,kind<3?4:1,32,0,0xff000000,0xffffffff));check(DrawingView.nativeUndo());check(Arrays.equals(pattern,DrawingView.nativePixels()));check(DrawingView.nativeRedo());check(DrawingView.nativeUndo());}
  check(DrawingView.nativeSetEffectSelection(new byte[81]));check(!DrawingView.nativeRender(0,4,4,0,0xff000000,0xffffffff));check(Arrays.equals(pattern,DrawingView.nativePixels()));check(DrawingView.nativeSetEffectSelection(null));
  for(int kind=0;kind<8;kind++){int size=kind==0||kind==7?4:kind==2?4:kind==3?100:2;check(DrawingView.nativeUtility(kind,kind<2?4:100,size,kind==4?60:0));check(DrawingView.nativeUndo());check(Arrays.equals(pattern,DrawingView.nativePixels()));}
  int[] object=new int[25];object[6]=0x80ff0000;check(DrawingView.nativeLoadBitmap(5,5,object));for(int kind=0;kind<3;kind++){check(DrawingView.nativeObject(kind,kind==0?4:2,0,0xff00ff00,false));check(DrawingView.nativeUndo());check(Arrays.equals(object,DrawingView.nativePixels()));check(DrawingView.nativeRedo());check(DrawingView.nativeUndo());}
  check(DrawingView.nativeSetEffectSelection(new byte[25]));check(!DrawingView.nativeObject(2,2,0,0xff00ff00,false));check(!DrawingView.nativeUtility(0,4,4,0));check(Arrays.equals(object,DrawingView.nativePixels()));check(DrawingView.nativeSetEffectSelection(null));
  System.out.println("JNI raster: transparency, source-over composition, atomic import, and undo/redo passed");
 }
}
