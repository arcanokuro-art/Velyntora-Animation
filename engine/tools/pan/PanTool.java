package art.velyntora.core;

import android.graphics.*;
import android.view.MotionEvent;

/** Gesture controller for pan. Shared document state belongs to DrawingView. */
final class PanTool implements DrawingTool {
  public boolean screen(DrawingView v, MotionEvent event) {
    return NavigationSupport.screen(v, event, false);
  }
}
