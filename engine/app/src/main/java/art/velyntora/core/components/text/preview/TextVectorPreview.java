package art.velyntora.core;
import android.graphics.*;
import android.os.Build;

/** Live glyphs remain vectors while the immutable surrounding layers stay cached. */
final class TextVectorPreview {
 final Bitmap lower;final Bitmap[] above;final int[] styles;final RectF bounds;
 TextSpec last;TextGlyphLayout glyphs;
 private TextVectorPreview(int[] info,int[] styles){this.styles=styles;lower=plane(0,info);above=new Bitmap[styles[2]];for(int i=0;i<above.length;i++)above[i]=plane(i+1,info);bounds=new RectF(0,0,info[0]*info[2],info[1]*info[2]);}
 static TextVectorPreview prepare(int[] info){int[] styles=DrawingView.nativeTextPreviewStyle();if(styles==null)return null;
  if(!supported(styles[0]))return null;for(int i=0;i<styles[2];i++)if(!supported(styles[3+i*2]))return null;
  return new TextVectorPreview(info,styles);
 }
 static boolean supported(int mode){return mode!=9 && (mode==0 || Build.VERSION.SDK_INT>=29);}
 static Bitmap plane(int index,int[] info){int[] pixels=DrawingView.nativeTextPreviewPlane(index);if(pixels==null)throw new IllegalStateException("Incomplete text preview");return Bitmap.createBitmap(pixels,info[0],info[1],Bitmap.Config.ARGB_8888);}
 static Paint paint(int mode,float opacity){Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);p.setAlpha(Math.round(opacity*255));if(mode!=0 && Build.VERSION.SDK_INT>=29)Modes.apply(p,mode);return p;}
 void draw(Canvas canvas,TextSpec text,PlacedImageTransform state){
  if(last!=text){last=text;glyphs=new TextGlyphLayout(text);}
  int saved=canvas.saveLayer(bounds,null);canvas.drawBitmap(lower,null,bounds,new Paint(Paint.FILTER_BITMAP_FLAG));
  if(styles[1]!=0){int layer=canvas.saveLayer(bounds,paint(styles[0],state.opacity));canvas.save();canvas.concat(state.matrix());canvas.clipRect(state.crop);glyphs.draw(canvas);canvas.restore();canvas.restoreToCount(layer);}
  for(int i=0;i<above.length;i++)canvas.drawBitmap(above[i],null,bounds,paint(styles[3+i*2],Float.intBitsToFloat(styles[4+i*2])));
  canvas.restoreToCount(saved);
 }
 static final class Modes {static void apply(Paint p,int mode){BlendMode[] modes={BlendMode.SRC_OVER,BlendMode.MULTIPLY,BlendMode.COLOR_BURN,BlendMode.COLOR_DODGE,BlendMode.OVERLAY,BlendMode.DIFFERENCE,BlendMode.LIGHTEN,BlendMode.DARKEN,BlendMode.SCREEN,BlendMode.SRC_OVER,BlendMode.HARD_LIGHT,BlendMode.SOFT_LIGHT,BlendMode.COLOR,BlendMode.LUMINOSITY,BlendMode.HUE,BlendMode.SATURATION};p.setBlendMode(modes[mode]);}}
}
