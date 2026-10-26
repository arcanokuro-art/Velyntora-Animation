package art.velyntora.core;
import android.content.Context;
import android.widget.ScrollView;
final class LayerPanelContainer extends ScrollView {
 LayerPanelContainer(Context c){super(c);setFillViewport(true);setVerticalScrollBarEnabled(false);}
 @Override protected void onMeasure(int w,int h){super.onMeasure(w,h);if(getChildCount()>0)getChildAt(0).measure(android.view.View.MeasureSpec.makeMeasureSpec(getMeasuredWidth(),android.view.View.MeasureSpec.EXACTLY),android.view.View.MeasureSpec.makeMeasureSpec(getMeasuredHeight(),android.view.View.MeasureSpec.EXACTLY));}
}
