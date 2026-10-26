package art.velyntora.core;

import android.app.ProgressDialog;
import java.util.function.BooleanSupplier;

/** Serial UI entry point: native composition and display preparation run on a worker. */
final class LayerWorkQueue {
  static void run(MainActivity host, BooleanSupplier operation, boolean redraw, Runnable completed) {
    run(host,operation,redraw,"Actualizando capas…","No se pudo completar la operación de capa",completed);
  }
  static void run(MainActivity host, BooleanSupplier operation, boolean redraw,String message,String failure,Runnable completed) {
    run(host,operation,redraw,message,failure,completed,null);
  }
  static void run(MainActivity host,BooleanSupplier operation,boolean redraw,String message,String failure,Runnable completed,Runnable settled){
    if (host.readProjectProgress() != null) return;
    DrawingView drawing = host.readDrawing();
    drawing.confirmCurve();
    drawing.finishOpacityStroke();
    ProgressDialog progress = new ProgressDialog(host);
    progress.setMessage(message);
    progress.setCancelable(false);
    host.writeProjectProgress(progress);
    drawing.setEnabled(false);
    android.os.Handler ui=new android.os.Handler(android.os.Looper.getMainLooper());
    Runnable showProgress=()->{if(host.readProjectProgress()==progress && !host.isDestroyed())progress.show();};
    ui.postDelayed(showProgress,250);
    float scale = (float)drawing.readViewport().scale;
    android.graphics.Rect visible=CanvasRendering.visibleBounds(drawing);
    NativeTileDisplay.Preparation detail=drawing.captureCanvasDetail();
    new Thread(() -> {
      boolean result = false;
      CanvasRendering.Frame prepared = null;
      try {
        result = operation.getAsBoolean();
        if (result && redraw) prepared = CanvasRendering.prepareFrame(scale,visible,detail);
      } catch (RuntimeException | OutOfMemoryError error) {
        result = false;
      }
      final boolean success = result;
      final CanvasRendering.Frame frame = prepared;
      host.runOnUiThread(() -> {
        try {
          if (host.isDestroyed()) {
            if (frame != null) frame.dispose();
            return;
          }
          if (frame != null) drawing.installCanvasFrame(frame);
          else {drawing.viewportChanged();}
          if (!success) host.message(failure);
        } finally {
          ui.removeCallbacks(showProgress);progress.dismiss();
          host.writeProjectProgress(null);
          drawing.setEnabled(true);
          if (!host.isDestroyed()) { host.refreshLayerPanel(); host.updateStatus(); }
        }
        try{if (success && !host.isDestroyed() && completed != null) completed.run();}
        finally{if(settled!=null)settled.run();}
      });
    }, "velyntora-layer-operation").start();
  }
  static void select(MainActivity host, int index, Runnable completed) {
    run(host, () -> DrawingView.nativeSelectLayer(index), false, completed);
  }
  private LayerWorkQueue() {}
}
