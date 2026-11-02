package app.velyntora.animation.timeline;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import app.velyntora.animation.preview.FramePreviewCache;
import app.velyntora.animation.storage.AnimationProject;

/** Timeline presentation; document mutations remain in the animation session. */
public final class TimelinePanel extends LinearLayout {
  public interface Actions {
    void select(int i); void add(boolean duplicate); void delete(); void move(int delta);
    void playback(); void fps(); void setFps(int value); void exposure(); void onion(); void save(); void export();
  }
  private final Actions actions;
  private final LinearLayout toolbar, transport, editing;
  private final TextView status, counter;
  private final TimelineStrip strip;
  private final HorizontalScrollView scroll;
  private final LinearLayout frames;
  private int selected, count;
  private final Paint playhead = new Paint(3);
  private boolean compact;

  public TimelinePanel(Context c, FramePreviewCache cache, Actions actions) {
    super(c); this.actions=actions;
    setOrientation(VERTICAL); setBackgroundColor(0xff1b2228);
    status=new TextView(c); status.setTextColor(0xffc5ccd1);status.setTextSize(10);
    addView(status,new LayoutParams(-1,dp(17)));
    toolbar=row(); transport=row();editing=row();
    counter=new TextView(c); counter.setTextColor(Color.WHITE);counter.setTextSize(16);
    counter.setGravity(Gravity.CENTER);counter.setBackground(background());
    addView(toolbar,new LayoutParams(-1,-2));
    frames=row(); frames.setPadding(0,dp(2),0,dp(6));
    frames.addView(control("previous","Fotograma anterior", "",()->navigate(-1)),new LayoutParams(dp(30),dp(88)));
    scroll=new HorizontalScrollView(c);scroll.setHorizontalScrollBarEnabled(false);
    strip=new TimelineStrip(c,cache,actions::select,()->actions.add(false));
    scroll.addView(strip);frames.addView(scroll,new LayoutParams(0,dp(94),1));
    frames.addView(control("next","Fotograma siguiente","",()->navigate(1)),new LayoutParams(dp(30),dp(88)));
    addView(frames,new LayoutParams(-1,dp(102)));
    scroll.setOnScrollChangeListener((v,x,y,ox,oy)->invalidate());
    LinearLayout audio=row();audio.setPadding(dp(7),dp(4),dp(7),dp(4));audio.setBackground(background());
    TextView label=new TextView(c);label.setText("Audio (opcional)");label.setTextColor(0xffe6e9eb);label.setTextSize(12);label.setGravity(Gravity.CENTER_VERTICAL);
    TimelineIcon music=new TimelineIcon("music");music.setBounds(0,0,dp(23),dp(23));label.setCompoundDrawables(music,null,null,null);label.setCompoundDrawablePadding(dp(10));
    audio.addView(label,new LayoutParams(dp(184),-1));
    TextView lane=new TextView(c);lane.setText("Sin audio");lane.setTextColor(0xff87929d);lane.setTextSize(11);lane.setGravity(Gravity.CENTER);lane.setBackground(background());
    audio.addView(lane,new LayoutParams(0,-1,1));addView(audio,new LayoutParams(-1,dp(42)));
    setOnApplyWindowInsetsListener((v,insets)->{setPadding(0,0,0,insets.getSystemWindowInsetBottom());return insets;});
    requestApplyInsets();
  }
  private LinearLayout row(){LinearLayout r=new LinearLayout(getContext());r.setGravity(Gravity.CENTER_VERTICAL);return r;}
  private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
  private GradientDrawable background(){GradientDrawable d=new GradientDrawable();d.setColor(0xff1a2025);d.setCornerRadius(dp(5));d.setStroke(dp(1),0xff364047);return d;}
  private Button control(String icon,String description,String text,Runnable action){
    Button b=new Button(getContext()){
      protected void onDraw(Canvas canvas){
        if(getText().length()!=0){super.onDraw(canvas);return;}
        android.graphics.drawable.Drawable icon=getCompoundDrawables()[0];
        if(icon==null){super.onDraw(canvas);return;}
        int checkpoint=canvas.save();
        canvas.translate((getWidth()-icon.getBounds().width())/2f,(getHeight()-icon.getBounds().height())/2f);
        icon.draw(canvas);canvas.restoreToCount(checkpoint);
      }
    };b.setAllCaps(false);b.setText(text);b.setTextColor(0xffe6e9eb);b.setTextSize(11);b.setMinWidth(0);b.setMinimumWidth(0);b.setMinHeight(0);b.setMinimumHeight(0);
    b.setPadding(dp(text.isEmpty()?8:10),0,dp(text.isEmpty()?8:10),0);b.setGravity(Gravity.CENTER);b.setBackground(background());b.setContentDescription(description);
    TimelineIcon d=new TimelineIcon(icon);d.setBounds(0,0,dp(22),dp(22));b.setCompoundDrawables(d,null,null,null);b.setCompoundDrawablePadding(text.isEmpty()?0:dp(7));b.setOnClickListener(v->action.run());return b;
  }
  private void put(LinearLayout row,Button b,int width){LayoutParams p=new LayoutParams(width<0?width:dp(width),dp(40));p.setMargins(dp(3),dp(4),dp(3),dp(4));row.addView(b,p);}
  private void navigate(int delta){if(count>0)actions.select(Math.max(0,Math.min(count-1,selected+delta)));}
  private void arrange(){
    if(transport.getParent()!=null)((ViewGroup)transport.getParent()).removeView(transport);
    if(editing.getParent()!=null)((ViewGroup)editing.getParent()).removeView(editing);
    toolbar.removeAllViews();toolbar.setOrientation(HORIZONTAL);
    LinearLayout controls=toolbar;
    if(compact){
      HorizontalScrollView viewport=new HorizontalScrollView(getContext());
      viewport.setHorizontalScrollBarEnabled(false);
      controls=row();viewport.addView(controls);
      toolbar.addView(viewport,new LayoutParams(-1,dp(48)));
    }
    controls.addView(transport,new LayoutParams(-2,dp(48)));
    if(!compact)controls.addView(new View(getContext()),new LayoutParams(0,1,1));
    controls.addView(editing,new LayoutParams(-2,dp(48)));
  }
  protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);boolean small=w<dp(850);if(small!=compact||toolbar.getChildCount()==0){compact=small;arrange();}}
  public void bind(AnimationProject project,boolean playing,boolean onion){
    strip.bind(project);transport.removeAllViews();editing.removeAllViews();
    if(project==null){status.setText("Preparando animación…");count=0;return;}
    count=project.sequence.size();selected=project.sequence.selected();
    status.setText(project.title+" · Fotograma "+(selected+1)+"/"+count+" · "+project.sequence.fps()+" FPS · "+String.format(java.util.Locale.getDefault(),"%.2f s",project.sequence.totalTicks()/(double)project.sequence.fps()));
    put(transport,control("first","Primer fotograma","",()->actions.select(0)),40);
    put(transport,control(playing?"pause":"play",playing?"Pausar":"Reproducir","",actions::playback),40);
    put(transport,control("previous","Fotograma anterior","",()->navigate(-1)),40);
    put(transport,control("next","Fotograma siguiente","",()->navigate(1)),40);
    Button fps=control("more","Velocidad de reproducción",project.sequence.fps()+" FPS",actions::fps);fps.setOnClickListener(v->{PopupMenu menu=new PopupMenu(getContext(),fps);for(int rate:new int[]{6,8,12,15,24,25,30,60})menu.getMenu().add(rate+" FPS").setOnMenuItemClickListener(item->{actions.setFps(rate);return true;});menu.getMenu().add("Personalizar…").setOnMenuItemClickListener(item->{actions.fps();return true;});menu.show();});put(transport,fps,96);
    Button onionButton=control("link","Cebolla: "+(onion?"sí":"no"),"",actions::onion);
    onionButton.setSelected(onion);
    if(onion){GradientDrawable enabled=background();enabled.setStroke(dp(2),0xffff269d);onionButton.setBackground(enabled);}
    put(transport,onionButton,42);
    if(counter.getParent()!=null)((android.view.ViewGroup)counter.getParent()).removeView(counter);
    counter.setText((selected+1)+" / "+count);transport.addView(counter,new LayoutParams(dp(82),dp(40)));
    put(editing,control("add","+ Fotograma","Añadir",()->actions.add(false)),-2);
    put(editing,control("duplicate","Duplicar","Duplicar",()->actions.add(true)),-2);
    put(editing,control("delete","Eliminar","Eliminar",actions::delete),-2);
    Button more=control("more","Más opciones","Más",()->{});more.setOnClickListener(v->{PopupMenu m=new PopupMenu(getContext(),v);
      String[] labels={"← Mover","Mover →","Duración: "+project.sequence.current().exposure,"Último fotograma","Guardar animación","Exportar PNG"};
      Runnable[] jobs={()->actions.move(-1),()->actions.move(1),actions::exposure,()->actions.select(count-1),actions::save,actions::export};
      for(int n=0;n<labels.length;n++){final int index=n;m.getMenu().add(labels[n]).setOnMenuItemClickListener(item->{jobs[index].run();return true;});}m.show();});put(editing,more,-2);
    if(toolbar.getChildCount()==0)arrange();
    scroll.post(()->scroll.smoothScrollTo(Math.max(0,selected*strip.cell-scroll.getWidth()/2+strip.cell/2),0));invalidate();
  }
  public void setPlaybackFrame(int frame){selected=frame;strip.setPlaybackFrame(frame);counter.setText((frame+1)+" / "+count);scroll.smoothScrollTo(Math.max(0,frame*strip.cell-scroll.getWidth()/2+strip.cell/2),0);invalidate();}
  protected void dispatchDraw(Canvas c){super.dispatchDraw(c);if(count==0)return;
    float x=frames.getLeft()+scroll.getLeft()+selected*strip.cell+strip.cell/2f-scroll.getScrollX();
    if(x<scroll.getLeft()||x>scroll.getRight())return;
    playhead.setColor(0xfff13d51);playhead.setStrokeWidth(dp(2));c.drawLine(x,frames.getTop()+dp(3),x,getHeight()-getPaddingBottom(),playhead);
    Path marker=new Path();marker.moveTo(x-dp(4),frames.getTop());marker.lineTo(x+dp(4),frames.getTop());marker.lineTo(x,frames.getTop()+dp(7));marker.close();c.drawPath(marker,playhead);
  }
}
