package art.velyntora.core;

import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;

/** Single-tap actions for existing interior nodes; movement remains a drag. */
final class CurvePointMenu {
  private float startX,startY,originalX,originalY;
  private int node=-1;
  private boolean moved;
  private PopupWindow popup;
  void begin(DrawingView view,MotionEvent event,boolean existing){
    dismiss();node=-1;moved=false;
    if(!existing || view.curve==null || view.curveHandle<=0 || view.curveHandle>=view.curve.x.length-1
        || event.isButtonPressed(MotionEvent.BUTTON_SECONDARY))return;
    node=view.curveHandle;startX=event.getX();startY=event.getY();
    originalX=view.curve.x[node];originalY=view.curve.y[node];
  }
  void track(DrawingView view,MotionEvent event){
    if(Math.hypot(event.getX()-startX,event.getY()-startY)>ViewConfiguration.get(view.getContext()).getScaledTouchSlop())moved=true;
  }
  boolean tapped(DrawingView view,MotionEvent event){
    track(view,event);
    return node>0 && !moved && view.curve!=null && view.curveHandle==node
      && node<view.curve.x.length-1 && event.getEventTime()-event.getDownTime()<ViewConfiguration.getLongPressTimeout();
  }
  void restoreTap(DrawingView view){view.curve.move(node,originalX,originalY);}
  void show(DrawingView view,int index){
    if(view.curve==null || index<=0 || index>=view.curve.x.length-1)return;
    CurveDraft target=view.curve,atOpen=target.copy();
    Button delete=new Button(view.getContext());delete.setAllCaps(false);delete.setText("Borrar punto");
    delete.setContentDescription("Borrar punto interior");delete.setTextColor(0xFFFFFFFF);
    delete.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_delete,0,0,0);
    int padding=Math.round(12*view.getResources().getDisplayMetrics().density);delete.setPadding(padding,padding,padding,padding);
    GradientDrawable background=new GradientDrawable();background.setColor(0xFF383838);background.setCornerRadius(padding);
    delete.setBackground(background);
    popup=new PopupWindow(delete,ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT,true);
    popup.setBackgroundDrawable(background);popup.setOutsideTouchable(true);popup.setElevation(padding);
    delete.setOnClickListener(clicked->{
      if(view.curve==target && target.sameGeometry(atOpen)){
        CurveDraft before=target.copy();
        if(target.removeInterior(index)){view.curveHistory.record(before,target);view.viewportChanged();}
      }
      dismiss();
    });
    delete.measure(View.MeasureSpec.UNSPECIFIED,View.MeasureSpec.UNSPECIFIED);
    int[] location=new int[2];view.getLocationInWindow(location);
    int width=view.getResources().getDisplayMetrics().widthPixels,height=view.getResources().getDisplayMetrics().heightPixels;
    int x=location[0]+Math.round((float)view.viewport.screenX(target.x[index],target.y[index]));
    int y=location[1]+Math.round((float)view.viewport.screenY(target.x[index],target.y[index]))+padding;
    popup.showAtLocation(view,Gravity.TOP|Gravity.LEFT,Math.max(0,Math.min(width-delete.getMeasuredWidth(),x)),Math.max(0,Math.min(height-delete.getMeasuredHeight(),y)));
  }
  void dismiss(){if(popup!=null){popup.dismiss();popup=null;}}
}
