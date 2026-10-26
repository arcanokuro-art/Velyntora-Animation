package app.velyntora.ui.start;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import static app.velyntora.ui.common.Ui.*;
public final class DrawingHero {
    public static View create(Context c,StartActions actions){
        FrameLayout hero=new FrameLayout(c);
        ImageView image=new ImageView(c);image.setImageResource(app.velyntora.R.drawable.start_drawing);image.setScaleType(ImageView.ScaleType.CENTER_CROP);hero.addView(image,new FrameLayout.LayoutParams(-1,-1));
        View shade=new View(c);shade.setBackgroundColor(0x40000000);hero.addView(shade,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout controls=column(c);controls.setGravity(Gravity.CENTER);controls.setPadding(dp(c,18),dp(c,20),dp(c,18),dp(c,20));
        TextView title=text(c,"DIBUJO",28,true);title.setGravity(Gravity.CENTER);controls.addView(title);
        TextView description=text(c,"Crea ilustraciones, edita imágenes\ny da vida a tus ideas.",16,false);description.setGravity(Gravity.CENTER);controls.addView(description);
        LinearLayout.LayoutParams button=new LinearLayout.LayoutParams(-1,dp(c,52));button.topMargin=dp(c,18);controls.addView(button(c,"Crear ilustración",true,actions::createIllustration),button);
        LinearLayout.LayoutParams open=new LinearLayout.LayoutParams(-1,dp(c,52));open.topMargin=dp(c,12);controls.addView(button(c,"Abrir imagen",false,actions::openImage),open);
        FrameLayout.LayoutParams center=new FrameLayout.LayoutParams(-1,-2,Gravity.CENTER);hero.addView(controls,center);
        TextView credit=text(c,"Powered By Zaskar Startup",15,true);credit.setGravity(Gravity.CENTER);FrameLayout.LayoutParams footer=new FrameLayout.LayoutParams(-1,dp(c,52),Gravity.BOTTOM);hero.addView(credit,footer);return hero;
    }
}
