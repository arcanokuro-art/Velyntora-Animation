package art.velyntora.core;
import android.content.Context;
import android.graphics.drawable.Drawable;
import java.io.InputStream;
/** Uses the exact user-supplied icon, without recoloring or substituting it. */
final class RemoveAiIcon {
 static Drawable load(Context context){
  try(InputStream input=context.getAssets().open("remove_ai/remove_ai.webp")){
   Drawable icon=Drawable.createFromStream(input,"Remove AI");
   if(icon==null)throw new IllegalStateException("Icono Remove AI inválido");
   return icon;
  }catch(java.io.IOException error){throw new IllegalStateException("Falta el icono Remove AI",error);}
 }
}
