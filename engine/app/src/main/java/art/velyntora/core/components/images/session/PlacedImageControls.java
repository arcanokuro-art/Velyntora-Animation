package art.velyntora.core;

import android.view.View;
import android.widget.*;
import java.util.IdentityHashMap;

final class PlacedImageControls {
  final MainActivity host;final LinearLayout options,row;final PlacedImageSession session;
  final TextView heading;final android.view.View crop,editText;
  final TextView opacityLabel;final SeekBar opacity;boolean syncing;
  final IdentityHashMap<View,Integer> hidden=new IdentityHashMap<>();
  PlacedImageControls(MainActivity host,LinearLayout options){
    this.host=host;this.options=options;session=host.readDrawing().placedImage();row=host.row();options.addView(row);
    heading=host.text("Imagen  ");row.addView(heading);
    editText=host.button(row,"Editar texto",()->TextDialog.edit(host));
    crop=host.iconButton(row,"Recortar imagen","rectangle",()->PlacedImageCropDialog.show(host,session));
    host.iconButton(row,"Rotar imagen","redo",session::rotate);
    CheckBox ratio=new CheckBox(host);ratio.setText("Proporciones");ratio.setChecked(true);ratio.setOnCheckedChangeListener((b,v)->session.keepRatio=v);row.addView(ratio);
    opacityLabel=host.text("Opacidad: 100 %");row.addView(opacityLabel);
    opacity=new SeekBar(host);opacity.setMax(100);opacity.setContentDescription("Opacidad de imagen insertada");row.addView(opacity,new LinearLayout.LayoutParams(host.dp(120),host.dp(48)));
    opacity.setOnTouchListener((v,e)->{v.getParent().requestDisallowInterceptTouchEvent(true);return false;});
    opacity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
      public void onProgressChanged(SeekBar s,int value,boolean user){if(!syncing && session.active()){session.state.opacity=value/100f;opacityLabel.setText("Opacidad: "+value+" %");session.notifyChanged();}}
      public void onStartTrackingTouch(SeekBar s){session.remember();}
      public void onStopTrackingTouch(SeekBar s){s.getParent().requestDisallowInterceptTouchEvent(false);}
    });
    host.iconButton(row,"Confirmar imagen","confirm",session::confirm);host.iconButton(row,"Cancelar imagen","cancel",session::cancel);
    row.setPadding(host.dp(8),0,host.dp(8),0);
    for(int i=0;i<row.getChildCount();i++){View child=row.getChildAt(i);LinearLayout.LayoutParams params=(LinearLayout.LayoutParams)child.getLayoutParams();params.setMargins(0,host.dp(4),host.dp(12),host.dp(4));child.setLayoutParams(params);}
    session.changed=this::sync;sync();
  }
  void sync(){
    boolean active=session.active();
    if(active){heading.setText(session.text!=null?"Texto  ":"Imagen  ");crop.setVisibility(session.text==null?View.VISIBLE:View.GONE);editText.setVisibility(session.text!=null?View.VISIBLE:View.GONE);
      for(int i=0;i<options.getChildCount();i++){View v=options.getChildAt(i);if(v==row)continue;if(!hidden.containsKey(v))hidden.put(v,v.getVisibility());v.setVisibility(View.GONE);}
      syncing=true;opacity.setProgress(Math.round(session.state.opacity*100));opacityLabel.setText("Opacidad: "+opacity.getProgress()+" %");syncing=false;
    }else{for(var entry:hidden.entrySet())entry.getKey().setVisibility(entry.getValue());hidden.clear();}
    row.setVisibility(active?View.VISIBLE:View.GONE);
  }
}
