package art.velyntora.core;
import android.widget.*;
/** A readable pixel value plus a manipulable slider; font range uses logarithmic precision. */
final class TextSlider {
 final SeekBar slider;final TextView label;final int min,max;final String name;final boolean logarithmic;
 int initialProgress,initialValue;
 TextSlider(MainActivity host,LinearLayout form,String name,int min,int max,int value,boolean logarithmic){this.name=name;this.min=min;this.max=max;this.logarithmic=logarithmic;
  label=host.text("");label.setPadding(0,host.dp(8),0,0);form.addView(label);slider=new SeekBar(host);slider.setContentDescription(name);slider.setMax(logarithmic?1000:max-min);slider.setProgress(logarithmic?Math.round((float)(Math.log(Math.max(min,value)/(double)min)/Math.log(max/(double)min)*1000)):value-min);form.addView(slider,new LinearLayout.LayoutParams(-1,host.dp(48)));initialProgress=slider.getProgress();initialValue=value;refresh();
 }
 int value(){if(slider.getProgress()==initialProgress)return initialValue;return logarithmic?Math.max(min,Math.min(max,(int)Math.round(min*Math.pow(max/(double)min,slider.getProgress()/1000.)))):min+slider.getProgress();}
 void refresh(){label.setText(name+": "+value()+" px");}
 void listen(Runnable changed){slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int n,boolean user){refresh();changed.run();}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});}
}
