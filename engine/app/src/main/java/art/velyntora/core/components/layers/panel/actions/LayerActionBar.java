package art.velyntora.core;

import android.util.TypedValue;
import android.view.Gravity;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

/** Compact layer controls: a single row, independent of the layer operation logic. */
final class LayerActionBar {
  private final MainActivity host;
  private final LinearLayout buttons;
  final HorizontalScrollView view;

  LayerActionBar(MainActivity host){
    this.host=host;
    view=new HorizontalScrollView(host);
    view.setHorizontalScrollBarEnabled(false);
    view.setFillViewport(true);
    buttons=host.row();
    buttons.setGravity(Gravity.CENTER_VERTICAL);
    buttons.setMinimumWidth(host.dp(240));
    buttons.setPadding(host.dp(8),host.dp(4),host.dp(8),host.dp(4));
    view.addView(buttons,new HorizontalScrollView.LayoutParams(-1,-2));
  }

  Button add(String label,String glyph,Runnable action){
    Button button=host.iconButton(buttons,label,glyph,action);
    button.setPadding(0,0,0,0);
    button.setMinWidth(0);button.setMinimumWidth(0);
    button.setMinHeight(0);button.setMinimumHeight(0);
    button.setGravity(Gravity.CENTER);
    TypedValue background=new TypedValue();
    host.getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless,background,true);
    button.setBackgroundResource(background.resourceId);
    android.graphics.drawable.Drawable icon=button.getCompoundDrawables()[0];
    if(icon!=null)icon.setBounds(0,0,host.dp(20),host.dp(20));
    button.setCompoundDrawablePadding(0);
    // Refresh TextView's cached drawable dimensions after keeping the 20 dp bounds.
    button.setCompoundDrawables(icon,null,null,null);
    // A left compound drawable is anchored to padding, even with CENTER gravity.
    // Center the unchanged glyph inside its measured touch target.
    button.addOnLayoutChangeListener((v,left,top,right,bottom,oldLeft,oldTop,oldRight,oldBottom)->{
      int inset=Math.max(0,((right-left)-host.dp(20))/2);
      if(button.getPaddingLeft()!=inset || button.getPaddingRight()!=inset)
        button.setPadding(inset,0,inset,0);
    });
    LinearLayout.LayoutParams cell=new LinearLayout.LayoutParams(0,host.dp(48),1);
    cell.setMargins(host.dp(2),0,host.dp(2),0);
    button.setLayoutParams(cell);
    return button;
  }
}
