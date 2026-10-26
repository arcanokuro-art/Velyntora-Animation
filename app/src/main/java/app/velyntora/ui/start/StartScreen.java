package app.velyntora.ui.start;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import static app.velyntora.ui.common.Ui.*;
public final class StartScreen {
    private final Context context;private final StartActions actions;
    public StartScreen(Context context,StartActions actions){this.context=context;this.actions=actions;}
    public View create(){
        Context c=context;boolean wide=c.getResources().getConfiguration().screenWidthDp>=700;
        LinearLayout root=new LinearLayout(c);root.setOrientation(wide?LinearLayout.HORIZONTAL:LinearLayout.VERTICAL);root.setBackgroundColor(BG);
        View hero=DrawingHero.create(c,actions);
        root.addView(hero,wide?new LinearLayout.LayoutParams(0,-1,0.36f):new LinearLayout.LayoutParams(-1,dp(c,460)));
        ScrollView scroll=new ScrollView(c);scroll.setFillViewport(true);
        LinearLayout right=column(c);right.setPadding(dp(c,24),dp(c,24),dp(c,24),dp(c,24));
        ImageView logo=new ImageView(c);logo.setImageBitmap(app.velyntora.ui.common.BrandLogo.load(c));logo.setScaleType(ImageView.ScaleType.FIT_CENTER);right.addView(logo,new LinearLayout.LayoutParams(-1,dp(c,64)));
        TextView drawing=text(c,"Drawing",22,true);drawing.setGravity(Gravity.CENTER);right.addView(drawing);
        TextView pilot=text(c,"VERSIÓN PILOTO · Prototipo en desarrollo",13,false);pilot.setTextColor(MUTED);pilot.setGravity(Gravity.CENTER);pilot.setPadding(0,dp(c,10),0,dp(c,24));right.addView(pilot);
        right.addView(RecentProjectsPanel.create(c,actions),new LinearLayout.LayoutParams(-1,-2));
        LinearLayout footer=new LinearLayout(c);footer.setGravity(Gravity.END);footer.setPadding(0,dp(c,12),0,0);
        footer.addView(button(c,"Preferencias de Velyntora",false,actions::showPreferences),new LinearLayout.LayoutParams(-2,dp(c,48)));
        LinearLayout.LayoutParams help=new LinearLayout.LayoutParams(-2,dp(c,48));help.leftMargin=dp(c,8);footer.addView(button(c,"? Ayuda",false,actions::showHelp),help);right.addView(footer);
        scroll.addView(right);root.addView(scroll,wide?new LinearLayout.LayoutParams(0,-1,0.64f):new LinearLayout.LayoutParams(-1,-2));
        if(wide)return root;
        ScrollView page=new ScrollView(c);page.addView(root);return page;
    }
}
