package art.velyntora.core;

import android.view.MotionEvent;

final class NavigationSupport {
  static boolean screen(DrawingView v, MotionEvent event, boolean zoom) {
    int action = event.getActionMasked();
    if (action == MotionEvent.ACTION_DOWN) {
      v.getParent().requestDisallowInterceptTouchEvent(true);
      v.navigationTool.begin(event.getX(), event.getY());
    } else if (action == MotionEvent.ACTION_MOVE) {
      v.navigationTool.move(v.viewport, event.getX(), event.getY(), zoom);
      v.viewportChanged();
    } else if (action == MotionEvent.ACTION_UP) {
      v.navigationTool.finish(v.viewport, zoom, event.getEventTime() - event.getDownTime() >= 500);
      v.viewportChanged();
      v.getParent().requestDisallowInterceptTouchEvent(false);
    } else if (action == MotionEvent.ACTION_CANCEL)
      v.getParent().requestDisallowInterceptTouchEvent(false);
    return true;
  }
}
