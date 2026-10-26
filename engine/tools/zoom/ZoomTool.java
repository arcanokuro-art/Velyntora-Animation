package art.velyntora.core;

import android.graphics.*;
import android.view.MotionEvent;

/** Gesture controller for zoom. Shared document state belongs to DrawingView. */
final class ZoomTool implements DrawingTool {
  public boolean screen(DrawingView v, MotionEvent event) {
    return NavigationSupport.screen(v, event, true);
  }
}
