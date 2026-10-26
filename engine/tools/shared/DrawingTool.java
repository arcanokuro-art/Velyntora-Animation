package art.velyntora.core;

import android.view.MotionEvent;

/** Workspace owns event lifecycle; a tool owns its document interaction. */
interface DrawingTool {
  default void preview(DrawingView v, android.graphics.Canvas canvas, float scale) {}

  default void cancel(DrawingView v) {}

  default void stroke(DrawingView v, float x0, float y0, float x1, float y1, float pressure) {}

  default boolean screen(DrawingView v, MotionEvent event) {
    return false;
  }

  default void down(DrawingView v, float x, float y, MotionEvent event) {}

  default void move(DrawingView v, float x, float y, MotionEvent event) {}

  default void up(DrawingView v, float x, float y, MotionEvent event) {}
}
