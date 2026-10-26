package art.velyntora.core;

import android.graphics.*;
import android.view.MotionEvent;

/** Gesture controller for line_curve. Shared document state belongs to DrawingView. */
final class LineCurveTool implements DrawingTool {
  private boolean adjustingTension;
  private final CurvePointMenu pointMenu=new CurvePointMenu();
  public void down(DrawingView v, float x, float y, MotionEvent event) {
    v.setFocusableInTouchMode(true);
    v.requestFocus();
    float nodeRadius = 12 * v.getResources().getDisplayMetrics().density / (float) v.viewport.scale;
    float pathRadius = 8 * v.getResources().getDisplayMetrics().density / (float) v.viewport.scale;
    v.dragBackup = v.curve == null ? null : v.curve.copy();
    v.curveHandle = v.curve == null ? -1 : v.curve.hit(x,y,nodeRadius);
    boolean existingPoint=v.curveHandle>=0;
    if (v.curve != null && v.curveHandle < 0) {
      // A previously drawn line stays editable until the whole group is confirmed.
      for (int i=0; i<v.curve.others.size(); i++) {
        CurveDraft other = v.curve.others.get(i);
        int handle = other.hit(x,y,nodeRadius);
        if(handle>=0)existingPoint=true;
        if (handle < 0) handle = other.insert(x,y,pathRadius);
        if (handle >= 0) {
          float[] oldX=v.curve.x, oldY=v.curve.y, oldT=v.curve.tension;
          v.curve.x=other.x; v.curve.y=other.y; v.curve.tension=other.tension;
          other.x=oldX; other.y=oldY; other.tension=oldT;
          v.curveHandle=handle;
          break;
        }
      }
      if (v.curveHandle < 0) v.curveHandle = v.curve.insert(x,y,pathRadius);
    }
    if (v.curveHandle < 0) {
      v.canceledCurve = null;
      if (v.curve == null) {
        v.curveHistory.clear();
        v.curve = new CurveDraft(x,y,x,y);
      } else v.curve = v.curve.newLine(x,y);
      v.curveHandle = -2; // new line: move its endpoint, retaining other pending lines
    }
    pointMenu.begin(v,event,existingPoint);
    adjustingTension = v.curveHandle >= 0 && event.isButtonPressed(MotionEvent.BUTTON_SECONDARY);
    v.viewportChanged();
  }

  public void move(DrawingView v, float x, float y, MotionEvent event) {
    pointMenu.track(v,event);
    if (v.curveHandle == -2) v.curve.move(1,x,y);
    else if (v.curveHandle >= 0 && !adjustingTension) v.curve.move(v.curveHandle, x, y);
    v.invalidate();
  }

  public void up(DrawingView v, float x, float y, MotionEvent event) {
    if (v.curve != null) {
      int tensionNode = v.curveHandle;
      boolean tap=pointMenu.tapped(v,event);
      boolean adjustTension = tensionNode >= 0 &&
          (event.getEventTime() - event.getDownTime() >= 500 ||
           adjustingTension) &&
          Math.hypot(x-v.startX,y-v.startY) * v.viewport.scale < 8;
      if(tap)pointMenu.restoreTap(v);
      else if (v.curveHandle == -2) v.curve.move(1,x,y);
      else if (v.curveHandle >= 0 && !adjustingTension) v.curve.move(v.curveHandle, x, y);
      v.curveHistory.record(v.dragBackup, v.curve);
      v.drawing = false;
      v.dragBackup = null;
      v.curveHandle = -1;
      v.viewportChanged();
      if(tap)pointMenu.show(v,tensionNode);
      else if (adjustTension) showTension(v,tensionNode);
    }
  }

  private static void showTension(DrawingView v, int node) {
    CurveDraft target = v.curve;
    android.widget.SeekBar slider = new android.widget.SeekBar(v.getContext());
    slider.setMax(100);
    slider.setProgress(Math.round(target.tension[node]*100));
    new android.app.AlertDialog.Builder(v.getContext())
        .setTitle("Tensión del punto")
        .setView(slider)
        .setPositiveButton("Aplicar", (dialog, which) -> {
          if (v.curve != target) return;
          CurveDraft before = target.copy();
          target.setTension(node,slider.getProgress()/100f);
          v.curveHistory.record(before,target);
          v.viewportChanged();
        })
        .setNegativeButton("Cancelar",null).show();
  }

  static android.graphics.Path curvePath(DrawingView v) {
    android.graphics.Path p = new android.graphics.Path();
    for (CurveDraft other : v.curve.others) appendPath(p,other);
    appendPath(p,v.curve);
    return p;
  }

  private static void appendPath(android.graphics.Path p, CurveDraft curve) {
    p.moveTo(curve.x[0], curve.y[0]);
    for (int i = 0; i < curve.x.length - 1; i++)
      p.cubicTo(curve.x[i] + curve.tangent(i, true),
          curve.y[i] + curve.tangent(i, false),
          curve.x[i+1] - curve.tangent(i+1, true),
          curve.y[i+1] - curve.tangent(i+1, false),
          curve.x[i+1], curve.y[i+1]);
  }

  static boolean confirmCurve(DrawingView v) {
    if (v.curve == null) return false;
    if (v.brushOpacity <= 0) { cancelCurve(v); return false; }
    Bitmap image = Bitmap.createBitmap(v.canvasWidth, v.canvasHeight, Bitmap.Config.ARGB_8888);
    try {
      Paint style = new Paint(Paint.ANTI_ALIAS_FLAG);
      style.setColor(v.color);
      style.setAlpha(Math.round((v.color >>> 24) * v.brushOpacity));
      style.setStyle(Paint.Style.STROKE);
      style.setStrokeWidth(v.brushRadius * 2);
      style.setStrokeCap(Paint.Cap.SQUARE);
      new Canvas(image).drawPath(v.curvePath(), style);
      int[] data = new int[2 + v.canvasWidth * v.canvasHeight];
      data[0] = v.canvasWidth;
      data[1] = v.canvasHeight;
      image.getPixels(data, 2, v.canvasWidth, 0, 0, v.canvasWidth, v.canvasHeight);
      if (v.hasSelection())
        for (int row = 0; row < v.canvasHeight; row++)
          for (int col = 0; col < v.canvasWidth; col++)
            if (!v.selectionContains(col, row)) data[2 + row * v.canvasWidth + col] = 0;
      if (!DrawingView.nativePasteSelection(data, 0, 0)) return false;
      {
        int tag = v.nextCurveTag++;
        v.savedCurves.put(
            tag,
            new CurveSnapshot(
                v.curve, v.color, v.brushRadius, v.brushOpacity, new SelectionSnapshot(v)));
        while (v.savedCurves.size() > 100) v.savedCurves.removeAt(0);
        DrawingView.nativeMarkCurve(tag);
      }
      v.curve = null;
      v.canceledCurve = null;
      v.curveHistory.clear();
      v.curveHandle = -1;
      v.refresh();
      return true;
    } finally {
      image.recycle();
    }
  }

  static void cancelCurve(DrawingView v) {
    v.curve = null;
    v.canceledCurve = null;
    v.curveHistory.clear();
    v.curveHandle = -1;
    v.viewportChanged();
  }

  static void undo(DrawingView v) {
    if (v.curve != null) {
      CurveDraft previous = v.curveHistory.undo(v.curve);
      if (previous != null) v.curve = previous;
      else {
        v.canceledCurve = v.curve.copy();
        v.curve = null;
      }
      v.viewportChanged();
      return;
    }
    v.canceledCurve = null;
    v.curveHistory.clear();
    int tag = DrawingView.nativeCurveTag();
    if (DrawingView.nativeUndo()) {
      CurveSnapshot saved = v.savedCurves.get(tag);
      v.deselect();
      if (saved != null) {
        v.curve = saved.geometry.copy();
        v.color = saved.color;
        v.setBrushRadius(saved.radius);
        v.setTool(DrawingView.LINE);
        v.setToolOpacityPercent(Math.round(saved.opacity * 100));
        v.curveHistory.clear();
        v.canceledCurve = null;
        saved.selection.restore(v);
        if (v.pickedColorListener != null) v.pickedColorListener.accept(v.color);
      }
      v.refresh();
    }
  }

  static void redo(DrawingView v) {
    if (v.canceledCurve != null) {
      v.curve = v.canceledCurve;
      v.canceledCurve = null;
      v.viewportChanged();
      return;
    }
    if (v.curve != null) {
      CurveDraft next = v.curveHistory.redo(v.curve);
      if (next != null) {
        v.curve = next;
        v.viewportChanged();
        return;
      }
    }
    v.cancelCurve();
    if (DrawingView.nativeRedo()) {
      v.deselect();
      v.refresh();
    }
  }

  public void cancel(DrawingView v) {
    pointMenu.dismiss();
    if (v.drawing) {
      v.curve = v.dragBackup;
      v.dragBackup = null;
      v.curveHandle = -1;
    }
  }

  public void preview(DrawingView v, Canvas canvas, float scale) {
    if (v.curve == null) return;
    Paint preview = new Paint(Paint.ANTI_ALIAS_FLAG);
    preview.setColor(v.color);
    preview.setAlpha(Math.round((v.color >>> 24) * v.brushOpacity));
    preview.setStyle(Paint.Style.STROKE);
    preview.setStrokeWidth(v.brushRadius * 2);
    preview.setStrokeCap(Paint.Cap.SQUARE);
    canvas.drawPath(v.curvePath(), preview);
    preview.setStyle(Paint.Style.FILL);
    preview.setColor(0xff7040b0);
    preview.setAlpha(255);
    for (CurveDraft other : v.curve.others) drawNodes(canvas,other,scale,preview);
    drawNodes(canvas,v.curve,scale,preview);
  }

  private static void drawNodes(Canvas canvas, CurveDraft curve, float scale, Paint paint) {
    for (int i=0;i<curve.x.length;i++) {
      paint.setStyle(Paint.Style.FILL);
      paint.setColor(0xffffffff);
      canvas.drawCircle(curve.x[i],curve.y[i],5/scale,paint);
      paint.setStyle(Paint.Style.STROKE);
      paint.setStrokeWidth(1.5f/scale);
      paint.setColor(0xff7040b0);
      canvas.drawCircle(curve.x[i],curve.y[i],5/scale,paint);
    }
  }
}
