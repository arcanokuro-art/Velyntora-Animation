package art.velyntora.core;

import android.app.AlertDialog;
import android.graphics.*;
import android.view.*;
import android.widget.*;

/** Crop the inserted source, never the canvas or existing layers. */
final class PlacedImageCropDialog {
  static void show(MainActivity host,PlacedImageSession session){
    if(!session.active())return;
    PlacedImageSource original=session.source;CropView view=new CropView(host,original,new RectF(session.state.crop));
    LinearLayout form=new LinearLayout(host);form.setOrientation(LinearLayout.VERTICAL);
    TextView hint=host.text("Arrastra los bordes para recortar. Arrastra dentro para mover el recorte.");form.addView(hint);
    Spinner ratio=new Spinner(host);ratio.setContentDescription("Proporción de recorte");ratio.setAdapter(new ArrayAdapter<>(host,android.R.layout.simple_spinner_dropdown_item,new String[]{"Libre","Cuadrado 1:1","3:4","4:3","16:9","9:16"}));form.addView(ratio);
    ratio.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){}public void onItemSelected(android.widget.AdapterView<?> p,View v,int position,long id){view.ratio=new float[]{0,1,.75f,4f/3,16f/9,9f/16}[position];view.fitRatio();view.invalidate();}});
    form.addView(view,new LinearLayout.LayoutParams(-1,Math.max(host.dp(180),Math.min(host.dp(360),host.getResources().getDisplayMetrics().heightPixels/2))));
    new AlertDialog.Builder(host).setTitle("Recortar imagen").setView(form).setNegativeButton("Cancelar",null)
      .setPositiveButton("Aplicar recorte",(d,w)->{if(session.source==original)session.cropTo(view.crop);}).show();
  }
  static final class CropView extends View {
    final PlacedImageSource source;final float width,height;RectF crop,start;
    final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);float ratio,scale,left,top,downX,downY;int handle;
    CropView(android.content.Context context,PlacedImageSource source,RectF crop){super(context);this.source=source;this.crop=crop;width=source.size.displayWidth();height=source.size.displayHeight();setContentDescription("Marco de recorte de imagen");}
    void fitRatio(){
      if(ratio<=0)return;float cx=crop.centerX(),cy=crop.centerY(),w=crop.width(),h=crop.height();
      if(w/h>ratio)w=h*ratio;else h=w/ratio;
      crop.set(cx-w/2,cy-h/2,cx+w/2,cy+h/2);clamp();
    }
    void clamp(){
      float w=Math.max(1,Math.min(width,crop.width())),h=Math.max(1,Math.min(height,crop.height()));
      float x=Math.max(0,Math.min(width-w,crop.left)),y=Math.max(0,Math.min(height-h,crop.top));crop.set(x,y,x+w,y+h);
    }
    protected void onDraw(Canvas c){
      c.drawColor(0xFF202020);scale=Math.min((getWidth()-24)/width,(getHeight()-24)/height);if(scale<=0)return;
      left=(getWidth()-width*scale)/2;top=(getHeight()-height*scale)/2;c.save();c.translate(left,top);c.scale(scale,scale);
      Checkerboard.draw(c,(int)width,(int)height);paint.setAlpha(255);c.drawBitmap(source.preview,null,new RectF(0,0,width,height),paint);
      paint.setColor(0x99000000);paint.setStyle(Paint.Style.FILL);
      c.drawRect(0,0,width,crop.top,paint);c.drawRect(0,crop.bottom,width,height,paint);c.drawRect(0,crop.top,crop.left,crop.bottom,paint);c.drawRect(crop.right,crop.top,width,crop.bottom,paint);
      paint.setColor(Color.WHITE);paint.setStrokeWidth(1/scale);paint.setStyle(Paint.Style.STROKE);c.drawRect(crop,paint);
      for(int i=1;i<3;i++){float x=crop.left+crop.width()*i/3,y=crop.top+crop.height()*i/3;c.drawLine(x,crop.top,x,crop.bottom,paint);c.drawLine(crop.left,y,crop.right,y,paint);}
      paint.setStyle(Paint.Style.FILL);float r=5*getResources().getDisplayMetrics().density/scale;
      for(float x:new float[]{crop.left,crop.centerX(),crop.right})for(float y:new float[]{crop.top,crop.centerY(),crop.bottom})if(x!=crop.centerX() || y!=crop.centerY())c.drawCircle(x,y,r,paint);
      c.restore();
    }
    public boolean onTouchEvent(MotionEvent e){
      if(scale<=0)return true;float x=(e.getX()-left)/scale,y=(e.getY()-top)/scale;
      if(e.getActionMasked()==MotionEvent.ACTION_DOWN){
        start=new RectF(crop);downX=x;downY=y;float radius=24*getResources().getDisplayMetrics().density/scale;handle=0;
        if(Math.abs(x-crop.left)<radius)handle|=1;else if(Math.abs(x-crop.right)<radius)handle|=2;
        if(Math.abs(y-crop.top)<radius)handle|=4;else if(Math.abs(y-crop.bottom)<radius)handle|=8;
        if(handle==0 && !crop.contains(x,y))start=null;
        getParent().requestDisallowInterceptTouchEvent(true);return true;
      }
      if(e.getActionMasked()==MotionEvent.ACTION_CANCEL){if(start!=null)crop=start;start=null;invalidate();return true;}
      if(start!=null && (e.getActionMasked()==MotionEvent.ACTION_MOVE || e.getActionMasked()==MotionEvent.ACTION_UP)){
        crop=new RectF(start);float dx=x-downX,dy=y-downY;
        if(handle==0)crop.offset(dx,dy);
        else{
          if((handle&1)!=0)crop.left=Math.max(0,Math.min(crop.right-1,start.left+dx));
          if((handle&2)!=0)crop.right=Math.min(width,Math.max(crop.left+1,start.right+dx));
          if((handle&4)!=0)crop.top=Math.max(0,Math.min(crop.bottom-1,start.top+dy));
          if((handle&8)!=0)crop.bottom=Math.min(height,Math.max(crop.top+1,start.bottom+dy));
          if(ratio>0){float w=crop.width(),h=crop.height();if((handle&3)!=0)h=w/ratio;else w=h*ratio;
            float factor=Math.min(1,Math.min(width/w,height/h));w*=factor;h*=factor;
            if((handle&1)!=0)crop.left=crop.right-w;else crop.right=crop.left+w;
            if((handle&4)!=0)crop.top=crop.bottom-h;else crop.bottom=crop.top+h;
          }
        }
        clamp();invalidate();if(e.getActionMasked()==MotionEvent.ACTION_UP){start=null;getParent().requestDisallowInterceptTouchEvent(false);}return true;
      }
      return true;
    }
  }
  private PlacedImageCropDialog(){}
}
