package art.velyntora.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;


/** Owns input behavior; the host is the workspace composition facade. */
final class CanvasInput {
  private final DrawingView host;
  private boolean selectionStatusPending;
  private void notifyCursorChanged(){
    if(host.readTool()!=DrawingView.MOVE_PIXELS && host.readTool()!=DrawingView.MOVE_SELECTION){
      if(host.readViewportChangedListener()!=null)host.readViewportChangedListener().run();
      return;
    }
    if(selectionStatusPending)return;
    selectionStatusPending=true;
    host.postOnAnimation(()->{selectionStatusPending=false;if(host.readViewportChangedListener()!=null)host.readViewportChangedListener().run();});
  }
  CanvasInput(DrawingView host) { this.host = host; }

  void updateCursor(float x, float y) {
    int cx = (int) Math.floor(x), cy = (int) Math.floor(y);
    if (cx < 0 || cy < 0 || cx >= host.readCanvasWidth() || cy >= host.readCanvasHeight()) {
      cx = -1;
      cy = -1;
    }
    if (cx != host.readCursorX() || cy != host.readCursorY()) {
      host.writeCursorX(cx);
      host.writeCursorY(cy);
      notifyCursorChanged();
    }
  }

  boolean onHoverEvent(MotionEvent event) {
    if (event.getActionMasked() == MotionEvent.ACTION_HOVER_EXIT) host.updateCursor(-1, -1);
    else
      host.updateCursor(
          (float) host.readViewport().documentX(event.getX(), event.getY()),
          (float) host.readViewport().documentY(event.getX(), event.getY()));
    return true;
  }

  void recordGesture(MotionEvent event) {
    host.writeGestureX((event.getX(0) + event.getX(1)) / 2f);
    host.writeGestureY((event.getY(0) + event.getY(1)) / 2f);
    float dx = event.getX(1) - event.getX(0), dy = event.getY(1) - event.getY(0);
    host.writeGestureDistance((float) Math.hypot(dx, dy));
    host.writeGestureAngle((float) Math.toDegrees(Math.atan2(dy, dx)));
  }

  void cancelToolGesture() {
    if(host.placedImage().active())host.placedImage().cancelGesture();
    host.readSelectionDragPreview().clear();
    Tools.controller(host.readTool()).cancel(host);
    host.finishOpacityStroke();
    host.writeDrawing(false);
    host.writeMovingSelection(false);
    host.writeMovingPixels(false);
    host.invalidate();
  }

  boolean onTouchEvent(MotionEvent event) {
    if (!host.isEnabled() || host.getWidth() <= 0 || host.getHeight() <= 0) return false;
    int action = event.getActionMasked();
    if (action == MotionEvent.ACTION_POINTER_DOWN && event.getPointerCount() >= 2) {
      host.cancelToolGesture();
      host.writeNavigating(true);
      host.recordGesture(event);
      host.getParent().requestDisallowInterceptTouchEvent(true);
      return true;
    }
    if (host.readNavigating()) {
      if (action == MotionEvent.ACTION_MOVE && event.getPointerCount() == 2) {
        float oldX = host.readGestureX(),
            oldY = host.readGestureY(),
            oldDistance = host.readGestureDistance(),
            oldAngle = host.readGestureAngle();
        host.recordGesture(event);
        float delta = host.readGestureAngle() - oldAngle;
        while (delta > 180) delta -= 360;
        while (delta < -180) delta += 360;
        if (oldDistance > 1)
          host.readViewport().gesture(oldX, oldY, host.readGestureX(), host.readGestureY(), host.readGestureDistance() / oldDistance, delta);
        host.viewportChanged();
      } else if (action == MotionEvent.ACTION_POINTER_UP && event.getPointerCount() > 2) {
        host.writeGestureDistance(0);
      } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
        host.writeNavigating(false);
        host.getParent().requestDisallowInterceptTouchEvent(false);
      }
      return true;
    }
    if (!host.placedImage().active() && (host.readTool() == host.readPAN() || host.readTool() == host.readZOOM())) return Tools.controller(host.readTool()).screen(host, event);
    float x = (float) host.readViewport().documentX(event.getX(), event.getY());
    float y = (float) host.readViewport().documentY(event.getX(), event.getY());
    if(host.placedImage().active())return host.placedImage().touch(event,x,y);
    if(action==MotionEvent.ACTION_DOWN && host.placedImage().tapExisting(x,y))return true;
    host.updateCursor(x, y);
    switch (event.getActionMasked()) {
      case MotionEvent.ACTION_DOWN:
        if (x < 0 || y < 0 || x >= host.readCanvasWidth() || y >= host.readCanvasHeight()) return true;
        host.getParent().requestDisallowInterceptTouchEvent(true);
        host.writeDrawing(true);
        host.writeStrokeEditing(false);
        host.writeStartX(host.writePreviousX(x));
        host.writeStartY(host.writePreviousY(y));
        if (host.supportsToolOpacity() && host.toolOpacityPercent() == 0) {
          host.writeDrawing(false);
          return true;
        }
        Tools.controller(host.readTool()).down(host, x, y, event);
        if(host.readTool()==DrawingView.MOVE_PIXELS)host.readSelectionDragPreview().begin();
        return true;
      case MotionEvent.ACTION_MOVE:
        if (!host.readDrawing()) return true;
        Tools.controller(host.readTool()).move(host, x, y, event);
        host.writePreviousX(x);
        host.writePreviousY(y);
        return true;
      case MotionEvent.ACTION_UP:
        host.getParent().requestDisallowInterceptTouchEvent(false);
        if (host.readDrawing()) {
          if(host.readTool()==DrawingView.MOVE_PIXELS)host.readSelectionDragPreview().release(x,y);
          Tools.controller(host.readTool()).up(host, x, y, event);
          if(host.readTool()==DrawingView.MOVE_PIXELS && (!(host.getContext() instanceof MainActivity activity) || activity.readProjectProgress()==null))host.readSelectionDragPreview().clear();
          boolean regionalStroke=host.readStrokeEditing();
          host.finishOpacityStroke();
          host.writeDrawing(false);
          if (host.readTool() == DrawingView.MOVE_PIXELS || host.readTool() == DrawingView.MOVE_SELECTION)
            host.invalidate(); // The selection worker installs the completed frame once.
          else if (host.readTool() == DrawingView.REMOVE_AI) host.invalidate();
          else if (regionalStroke) host.finishStrokeRefresh();
          else host.refresh();
        }
        return true;
      case MotionEvent.ACTION_CANCEL:
        host.getParent().requestDisallowInterceptTouchEvent(false);
        host.cancelToolGesture();
        return true;
      default:
        return true;
    }
  }
}
