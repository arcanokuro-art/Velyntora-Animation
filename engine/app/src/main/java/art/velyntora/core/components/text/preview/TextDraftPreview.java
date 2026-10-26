package art.velyntora.core;
import android.content.Context;
import android.graphics.*;
import android.view.View;
final class TextDraftPreview extends View {
 TextSpec spec;TextGlyphLayout glyphs;final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
 TextDraftPreview(Context context){super(context);setContentDescription("Vista previa del texto");}
 void update(TextSpec next){spec=next;glyphs=new TextGlyphLayout(next);invalidate();}
 protected void onDraw(Canvas canvas){super.onDraw(canvas);canvas.drawColor(0xff222222);if(glyphs==null)return;
  Checkerboard.draw(canvas,getWidth(),getHeight());float fit=Math.min(1,Math.min((getWidth()-24f)/glyphs.width,(getHeight()-24f)/glyphs.height));
  canvas.save();canvas.translate(12,12);canvas.scale(Math.max(.0001f,fit),Math.max(.0001f,fit));glyphs.draw(canvas);canvas.restore();
 }
}
