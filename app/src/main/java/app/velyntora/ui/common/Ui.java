package app.velyntora.ui.common;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.widget.*;
public final class Ui {
    public static final int BG=0xff151922, PANEL=0xff0e1826, TEXT=0xffeef1f7, MUTED=0xffafb8c9;
    public static int dp(Context c,int n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
    public static LinearLayout column(Context c){LinearLayout v=new LinearLayout(c);v.setOrientation(LinearLayout.VERTICAL);return v;}
    public static TextView text(Context c,String s,int size,boolean bold){TextView v=new TextView(c);v.setText(s);v.setTextSize(size);v.setTextColor(TEXT);if(bold)v.setTypeface(null,Typeface.BOLD);return v;}
    public static GradientDrawable background(int color){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(8);d.setStroke(1,0xff344155);return d;}
    public static Button button(Context c,String s,boolean primary,Runnable action){Button b=new Button(c);b.setText(s);b.setTextColor(Color.WHITE);b.setAllCaps(false);b.setTextSize(14);b.setBackground(background(primary?0xffab34be:PANEL));b.setOnClickListener(v->action.run());return b;}
}
