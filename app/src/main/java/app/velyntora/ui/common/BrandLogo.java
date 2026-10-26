package app.velyntora.ui.common;
import android.content.Context;
import android.graphics.*;
/** Preserves the original white and purple mark while removing its black backdrop. */
public final class BrandLogo {
    private static Bitmap transparent;
    private BrandLogo(){}
    public static Bitmap load(Context context){
        if(transparent!=null)return transparent;
        Bitmap source=BitmapFactory.decodeResource(context.getResources(),app.velyntora.R.drawable.velyntora_logo);
        Bitmap result=source.copy(Bitmap.Config.ARGB_8888,true);
        int width=result.getWidth(),height=result.getHeight();
        int[] pixels=new int[width*height];result.getPixels(pixels,0,width,0,0,width,height);
        for(int i=0;i<pixels.length;i++){
            int p=pixels[i];
            if(Color.red(p)<28&&Color.green(p)<28&&Color.blue(p)<28)pixels[i]=Color.TRANSPARENT;
        }
        result.setPixels(pixels,0,width,0,0,width,height);
        transparent=result;return result;
    }
}
