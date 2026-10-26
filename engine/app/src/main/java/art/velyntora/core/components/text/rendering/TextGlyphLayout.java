package art.velyntora.core;
import android.graphics.*;
import android.text.*;
/** Resolution-independent glyph layout, shared by preview and final tile rendering. */
final class TextGlyphLayout {
 final StaticLayout layout;final int width,height;
 TextGlyphLayout(TextSpec spec){
  TextPaint paint=new TextPaint(Paint.ANTI_ALIAS_FLAG|Paint.SUBPIXEL_TEXT_FLAG);paint.setColor(spec.color);paint.setTextSize(spec.size);paint.setTypeface(Typeface.create(TextSpec.FAMILIES[spec.family],spec.style));
  int inner=Math.max(4,Math.min(spec.wrapWidth-4,(int)Math.ceil(Layout.getDesiredWidth(spec.content,paint))+2));
  layout=StaticLayout.Builder.obtain(spec.content,0,spec.content.length(),paint,inner).setIncludePad(true).setBreakStrategy(Layout.BREAK_STRATEGY_SIMPLE).build();width=inner+4;height=layout.getHeight()+4;
 }
 void draw(Canvas canvas){canvas.save();canvas.translate(2,2);layout.draw(canvas);canvas.restore();}
 static void drawObject(Canvas canvas,TextSpec spec,PlacedImageTransform state){canvas.save();canvas.concat(state.matrix());canvas.clipRect(state.crop);new TextGlyphLayout(spec).draw(canvas);canvas.restore();}
 static Bitmap tile(TextGlyphLayout glyphs,PlacedImageTransform state,Rect tile){Bitmap result=Bitmap.createBitmap(tile.width(),tile.height(),Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(result);canvas.translate(-tile.left,-tile.top);canvas.concat(state.matrix());canvas.clipRect(state.crop);glyphs.draw(canvas);return result;}
}
