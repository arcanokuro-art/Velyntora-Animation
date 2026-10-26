package app.velyntora.ui.start;
import android.content.Context;
import android.view.Gravity;
import android.widget.*;
import static app.velyntora.ui.common.Ui.*;
public final class RecentProjectsPanel {
    public static LinearLayout create(Context c,StartActions actions){
        LinearLayout box=column(c);box.setPadding(dp(c,16),dp(c,12),dp(c,16),dp(c,12));box.setBackground(background(PANEL));
        LinearLayout header=new LinearLayout(c);header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(text(c,"Proyectos recientes",16,true),new LinearLayout.LayoutParams(0,-2,1));
        header.addView(button(c,"Ver más »",false,actions::showRecentProjects),new LinearLayout.LayoutParams(dp(c,100),dp(c,48)));box.addView(header);
        TextView empty=text(c,"Tus proyectos recientes aparecerán aquí.",14,false);empty.setTextColor(MUTED);empty.setGravity(Gravity.CENTER);box.addView(empty,new LinearLayout.LayoutParams(-1,dp(c,90)));return box;
    }
}
