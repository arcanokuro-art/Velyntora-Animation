package art.velyntora.core;

import android.app.Dialog;
import android.content.Context;
import android.graphics.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.util.Locale;

/** Pinta-style selector. All edits remain local until the user accepts. */
final class ColorPickerDialog {
  interface Listener { void accept(int primary, int secondary); }
  private final Context context;
  private final Dialog dialog;
  private final int[] colors, original;
  private final float[] hsv = new float[3];
  private int selected, alpha;
  private boolean updating, square, showValue = true;
  private final EditText[] numbers = new EditText[7];
  private final Channel[] sliders = new Channel[7];
  private final View[] swatches = new View[2];
  private EditText hex;
  private Plane plane;
  private final String[] names = {"Tono", "Saturación", "Valor", "Rojo", "Verde", "Azul", "Alfa"};
  private final int[] maxima = {360,100,100,255,255,255,255};

  static void show(Context context, int primary, int secondary, boolean editSecondary, Listener listener) {
    new ColorPickerDialog(context, primary, secondary, editSecondary, listener,false);
  }
  static void showSingle(Context context,int color,java.util.function.IntConsumer listener){new ColorPickerDialog(context,color,color,false,(primary,secondary)->listener.accept(primary),true);}
  private int dp(float value) { return Math.round(value * context.getResources().getDisplayMetrics().density); }
  private TextView label(String text) {
    TextView view = new TextView(context); view.setText(text); view.setTextColor(Color.WHITE);
    view.setTextSize(14); view.setGravity(Gravity.CENTER_VERTICAL); return view;
  }
  private LinearLayout row() { LinearLayout view=new LinearLayout(context); view.setGravity(Gravity.CENTER_VERTICAL); return view; }
  private Button button(String text, Runnable action) {
    Button button=new Button(context); button.setText(text); button.setAllCaps(false);
    button.setTextColor(Color.WHITE); button.setTextSize(13);
    LinearLayout.LayoutParams spacing=new LinearLayout.LayoutParams(-2,-2);
    spacing.setMargins(0,dp(4),dp(8),dp(4));button.setLayoutParams(spacing);
    button.setOnClickListener(v->action.run()); return button;
  }
  private EditText entry(int width) {
    EditText view=new EditText(context); view.setTextColor(Color.WHITE); view.setSingleLine(true);
    view.setTextSize(14); view.setSelectAllOnFocus(true); view.setPadding(dp(6),0,dp(6),0);
    view.setLayoutParams(new LinearLayout.LayoutParams(dp(width),dp(44))); return view;
  }
  private ColorPickerDialog(Context ctx,int primary,int secondary,boolean editSecondary,Listener listener){this(ctx,primary,secondary,editSecondary,listener,false);}
  private ColorPickerDialog(Context ctx,int primary,int secondary,boolean editSecondary,Listener listener,boolean single) {
    context=ctx; colors=new int[]{primary,secondary}; original=colors.clone(); selected=editSecondary?1:0;
    dialog=new Dialog(ctx); dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
    LinearLayout root=new LinearLayout(ctx); root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(dp(20),dp(12),dp(20),dp(16)); root.setBackgroundColor(0xff303030);
    LinearLayout header=row(); header.addView(button("Restablecer",()->{System.arraycopy(original,0,colors,0,2); load();}));
    TextView title=label(single?"Color del texto":"Selector de color"); title.setGravity(Gravity.CENTER);
    header.addView(title,new LinearLayout.LayoutParams(0,dp(48),1)); root.addView(header);
    ScrollView scroll=new ScrollView(ctx); LinearLayout contents=new LinearLayout(ctx); contents.setOrientation(LinearLayout.VERTICAL); contents.setPadding(0,dp(8),0,dp(8));
    scroll.addView(contents); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    HorizontalScrollView tabsScroll=new HorizontalScrollView(ctx); LinearLayout tabs=row();
    if(!single)tabs.addView(button("⇄",()->{int value=colors[0]; colors[0]=colors[1]; colors[1]=value;load();}));
    tabs.addView(button("Tono y saturación",()->{square=false;plane.invalidate();}));
    tabs.addView(button("Saturación y valor",()->{square=true;plane.invalidate();})); tabsScroll.addView(tabs); contents.addView(tabsScroll);
    LinearLayout hexRow=row(); hexRow.addView(label("Hexadecimal  ")); hex=entry(130);
    hex.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
    hex.setFilters(new InputFilter[]{new InputFilter.LengthFilter(8)}); hex.setContentDescription("Hexadecimal RGBA"); hexRow.addView(hex); contents.addView(hexRow);
    LinearLayout body=new LinearLayout(ctx);
    int availableDp=Math.min(ctx.getResources().getConfiguration().screenWidthDp-24,860);
    boolean wide=availableDp>=700 && ctx.getResources().getConfiguration().fontScale<=1.4f;
    body.setPadding(0,dp(14),0,dp(8));
    body.setOrientation(wide?LinearLayout.HORIZONTAL:LinearLayout.VERTICAL); contents.addView(body);
    LinearLayout palette=row(); LinearLayout swatchColumn=new LinearLayout(ctx); swatchColumn.setOrientation(LinearLayout.VERTICAL);
    for(int i=0;i<2;i++){ final int slot=i; swatches[i]=new View(ctx);swatches[i].setContentDescription(single?"Color del texto":i==0?"Color primario":"Color secundario");if(single && i==1)swatches[i].setVisibility(View.GONE);
      LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(48),dp(70));p.setMargins(0,dp(5),dp(10),dp(5));swatchColumn.addView(swatches[i],p);
      swatches[i].setOnClickListener(v->{selected=slot;load();}); }
    palette.addView(swatchColumn); LinearLayout surface=new LinearLayout(ctx); surface.setOrientation(LinearLayout.VERTICAL);
    plane=new Plane(ctx);plane.setContentDescription("Superficie de selección de color"); surface.addView(plane,new LinearLayout.LayoutParams(dp(200),dp(200)));
    CheckBox value=new CheckBox(ctx);value.setText("Mostrar valor");value.setTextColor(Color.WHITE);value.setChecked(true);
    value.setOnCheckedChangeListener((b,checked)->{showValue=checked;plane.invalidate();});surface.addView(value);palette.addView(surface);body.addView(palette);
    LinearLayout controls=new LinearLayout(ctx);controls.setOrientation(LinearLayout.VERTICAL);
    LinearLayout.LayoutParams controlsLayout=new LinearLayout.LayoutParams(wide?0:-1,-2,wide?1:0);
    controlsLayout.setMargins(wide?dp(28):0,wide?0:dp(20),0,0);
    body.addView(controls,controlsLayout);
    TextView measure=label("Saturación");
    int labelWidth=Math.max(dp(85),(int)Math.ceil(measure.getPaint().measureText("Saturación"))+dp(16));
    int numberWidth=Math.max(dp(58),(int)Math.ceil(measure.getPaint().measureText("888"))+dp(22));
    for(int i=0;i<7;i++){final int channel=i;if(i==3||i==6){View separator=new View(ctx);separator.setBackgroundColor(0xff555555);controls.addView(separator,new LinearLayout.LayoutParams(-1,dp(1)));}
      LinearLayout line=row();TextView name=label(names[i]);line.addView(name,new LinearLayout.LayoutParams(labelWidth,dp(48)));
      sliders[i]=new Channel(ctx,i);sliders[i].setContentDescription(names[i]);line.addView(sliders[i],new LinearLayout.LayoutParams(0,dp(48),1));
      numbers[i]=entry(Math.round(numberWidth/ctx.getResources().getDisplayMetrics().density));numbers[i].setInputType(android.text.InputType.TYPE_CLASS_NUMBER);numbers[i].setContentDescription("Valor de "+names[i]);line.addView(numbers[i]);controls.addView(line);
      numbers[i].addTextChangedListener(watcher(()->{try{int n=Integer.parseInt(numbers[channel].getText().toString());if(n>=0&&n<=maxima[channel]) change(channel,n);}catch(NumberFormatException ignored){}}));
      numbers[i].setOnFocusChangeListener((v,focus)->{if(!focus)refresh();}); }
    hex.addTextChangedListener(watcher(()->{String text=hex.getText().toString();if(text.matches("[0-9a-fA-F]{8}")){long rgba=Long.parseLong(text,16);colors[selected]=(int)((rgba>>>8)|((rgba&255)<<24));load();}}));
    hex.setOnFocusChangeListener((v,focus)->{if(!focus)refresh();});
    LinearLayout footer=row();Space space=new Space(ctx);footer.addView(space,new LinearLayout.LayoutParams(0,1,1));footer.addView(button("Cancelar",dialog::dismiss));
    Button accept=button("Aceptar",()->{listener.accept(colors[0],colors[1]);dialog.dismiss();});accept.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff39734d));footer.addView(accept);root.addView(footer);
    dialog.setContentView(root);load();dialog.show();
    Window window=dialog.getWindow();if(window!=null){int width=Math.min(ctx.getResources().getDisplayMetrics().widthPixels-dp(24),dp(860));int height=Math.min(ctx.getResources().getDisplayMetrics().heightPixels-dp(32),dp(560));window.setLayout(width,height);window.setBackgroundDrawableResource(android.R.color.transparent);window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);}
  }
  private TextWatcher watcher(Runnable change){return new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){if(!updating)change.run();}public void afterTextChanged(Editable e){}};}
  private void load(){Color.colorToHSV(colors[selected],hsv);alpha=Color.alpha(colors[selected]);refresh();}
  private int number(int channel){if(channel<3)return Math.round(hsv[channel]*(channel==0?1:100));int c=colors[selected];return channel==3?Color.red(c):channel==4?Color.green(c):channel==5?Color.blue(c):alpha;}
  private void change(int channel,int value){if(channel<3){hsv[channel]=value/(channel==0?1f:100f);colors[selected]=Color.HSVToColor(alpha,hsv);}else{int c=colors[selected];int r=Color.red(c),g=Color.green(c),b=Color.blue(c);if(channel==3)r=value;if(channel==4)g=value;if(channel==5)b=value;if(channel==6)alpha=value;colors[selected]=Color.argb(alpha,r,g,b);Color.colorToHSV(colors[selected],hsv);}refresh();}
  private void refresh(){updating=true;for(int i=0;i<7;i++){if(!numbers[i].hasFocus())numbers[i].setText(String.valueOf(number(i)));sliders[i].invalidate();}if(!hex.hasFocus())hex.setText(String.format(Locale.ROOT,"%08X",((long)(colors[selected]&0xffffff)<<8)|alpha));for(int i=0;i<2;i++){swatches[i].setBackground(new ColorSwatchDrawable(colors[i]));swatches[i].setAlpha(selected==i?1f:.65f);}plane.invalidate();updating=false;}
  private class Channel extends View {
    final int channel;final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    Channel(Context c,int index){super(c);channel=index;setFocusable(true);}
    protected void onDraw(Canvas canvas){float left=dp(5),right=getWidth()-dp(5),top=dp(12),bottom=getHeight()-dp(10);int[] gradient;
      if(channel==0)gradient=new int[]{Color.RED,Color.YELLOW,Color.GREEN,Color.CYAN,Color.BLUE,Color.MAGENTA,Color.RED};else{int start,end;if(channel<3){float[] h=hsv.clone();h[channel]=0;start=Color.HSVToColor(h);h[channel]=1;end=Color.HSVToColor(h);}else{int c=colors[selected];int shift=channel==3?16:channel==4?8:channel==5?0:24;start=(c&~(255<<shift));end=start|(255<<shift);if(channel!=6){start|=0xff000000;end|=0xff000000;}}gradient=new int[]{start,end};}
      paint.setShader(new LinearGradient(left,0,Math.max(left+1,right),0,gradient,null,Shader.TileMode.CLAMP));canvas.drawRect(left,top,right,bottom,paint);paint.setShader(null);paint.setColor(Color.WHITE);float x=left+(right-left)*number(channel)/maxima[channel];Path marker=new Path();marker.moveTo(x,top+dp(7));marker.lineTo(x-dp(5),top-dp(2));marker.lineTo(x+dp(5),top-dp(2));marker.close();canvas.drawPath(marker,paint);
    }
    public boolean onTouchEvent(MotionEvent event){if(event.getActionMasked()==MotionEvent.ACTION_DOWN){for(EditText field:numbers)field.clearFocus();hex.clearFocus();getParent().requestDisallowInterceptTouchEvent(true);}if(event.getActionMasked()==MotionEvent.ACTION_DOWN||event.getActionMasked()==MotionEvent.ACTION_MOVE){change(channel,Math.round(Math.max(0,Math.min(1,(event.getX()-dp(5))/Math.max(1,getWidth()-dp(10))))*maxima[channel]));return true;}if(event.getActionMasked()==MotionEvent.ACTION_UP||event.getActionMasked()==MotionEvent.ACTION_CANCEL){getParent().requestDisallowInterceptTouchEvent(false);performClick();return true;}return true;}
    public boolean performClick(){super.performClick();return true;}
    public boolean onKeyDown(int key,android.view.KeyEvent event){if(key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT){change(channel,Math.max(0,Math.min(maxima[channel],number(channel)+(key==KeyEvent.KEYCODE_DPAD_RIGHT?1:-1))));return true;}return super.onKeyDown(key,event);}
  }
  private class Plane extends View {
    final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);Plane(Context c){super(c);}
    protected void onDraw(Canvas canvas){float w=getWidth(),h=getHeight(),cx=w/2,cy=h/2,r=Math.min(w,h)/2-dp(3);float x,y;
      if(square){paint.setShader(new LinearGradient(0,0,w,0,Color.WHITE,Color.HSVToColor(new float[]{hsv[0],1,1}),Shader.TileMode.CLAMP));canvas.drawRect(0,0,w,h,paint);paint.setShader(new LinearGradient(0,0,0,h,Color.TRANSPARENT,Color.BLACK,Shader.TileMode.CLAMP));canvas.drawRect(0,0,w,h,paint);x=hsv[1]*w;y=(1-hsv[2])*h;}else{paint.setShader(new SweepGradient(cx,cy,new int[]{Color.RED,Color.YELLOW,Color.GREEN,Color.CYAN,Color.BLUE,Color.MAGENTA,Color.RED},null));canvas.drawCircle(cx,cy,r,paint);paint.setShader(new RadialGradient(cx,cy,r,Color.WHITE,Color.TRANSPARENT,Shader.TileMode.CLAMP));canvas.drawCircle(cx,cy,r,paint);paint.setShader(null);paint.setColor(Color.argb(showValue?Math.round(255*(1-hsv[2])):0,0,0,0));canvas.drawCircle(cx,cy,r,paint);double angle=Math.toRadians(hsv[0]);x=cx+(float)Math.cos(angle)*hsv[1]*r;y=cy+(float)Math.sin(angle)*hsv[1]*r;}
      paint.setShader(null);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(3));paint.setColor(Color.BLACK);canvas.drawCircle(x,y,dp(6),paint);paint.setStrokeWidth(dp(1.5f));paint.setColor(Color.WHITE);canvas.drawCircle(x,y,dp(6),paint);paint.setStyle(Paint.Style.FILL);
    }
    public boolean onTouchEvent(MotionEvent e){int action=e.getActionMasked();if(action==MotionEvent.ACTION_DOWN){for(EditText field:numbers)field.clearFocus();hex.clearFocus();getParent().requestDisallowInterceptTouchEvent(true);}if(action==MotionEvent.ACTION_DOWN||action==MotionEvent.ACTION_MOVE){if(square){hsv[1]=Math.max(0,Math.min(1,e.getX()/getWidth()));hsv[2]=1-Math.max(0,Math.min(1,e.getY()/getHeight()));}else{float dx=e.getX()-getWidth()/2f,dy=e.getY()-getHeight()/2f;hsv[0]=(float)((Math.toDegrees(Math.atan2(dy,dx))+360)%360);hsv[1]=Math.min(1,(float)Math.hypot(dx,dy)/(Math.min(getWidth(),getHeight())/2f-dp(3)));}colors[selected]=Color.HSVToColor(alpha,hsv);refresh();return true;}if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL){getParent().requestDisallowInterceptTouchEvent(false);performClick();}return true;}
    public boolean performClick(){super.performClick();return true;}
  }
}
