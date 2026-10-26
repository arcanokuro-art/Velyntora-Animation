package art.velyntora.core;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import java.io.InputStream;
import java.io.OutputStream;


/** Owns components/history behavior; the host is the workspace composition facade. */
final class HistoryActions {
  private final MainActivity host;
  private final java.util.ArrayDeque<Boolean> pending=new java.util.ArrayDeque<>();
  private boolean running;
  private void settled(){
    running=false;
    if(host.isDestroyed()){pending.clear();return;}
    if(!pending.isEmpty()){
      // Start the next queued request before accepting a newer input event.
      boolean redo=pending.removeFirst();if(redo)redo();else undo();
      if(!running)host.readDrawing().post(this::settled);
    }
  }
  HistoryActions(MainActivity host) { this.host = host; }

  void undo() {
    DrawingView drawing=host.readDrawing();
    if(host.readProjectProgress()!=null){if(running)pending.addLast(false);return;}
    if(drawing.curve!=null) {drawing.undo();host.refreshLayerPanel();return;}
    if((DrawingView.nativeHistoryAvailability()&1)==0)return;
    drawing.finishOpacityStroke();drawing.canceledCurve=null;drawing.curveHistory.clear();
    int tag=DrawingView.nativeCurveTag();
    running=true;
    LayerWorkQueue.run(host,DrawingView::nativeUndo,true,"Deshaciendo…","No se pudo deshacer el cambio",()->{
      drawing.deselect();
      CurveSnapshot saved=drawing.savedCurves.get(tag);
      if(saved!=null) {
        drawing.curve=saved.geometry.copy();drawing.color=saved.color;
        drawing.setBrushRadius(saved.radius);drawing.setTool(DrawingView.LINE);
        drawing.setToolOpacityPercent(Math.round(saved.opacity*100));
        drawing.curveHistory.clear();drawing.canceledCurve=null;saved.selection.restore(drawing);
        if(drawing.pickedColorListener!=null)drawing.pickedColorListener.accept(drawing.color);
      }
      drawing.viewportChanged();
    },this::settled);
  }

  void redo() {
    DrawingView drawing=host.readDrawing();
    if(host.readProjectProgress()!=null){if(running)pending.addLast(true);return;}
    if(drawing.canceledCurve!=null || drawing.curve!=null) {
      drawing.redo();host.refreshLayerPanel();return;
    }
    if((DrawingView.nativeHistoryAvailability()&2)==0)return;
    drawing.finishOpacityStroke();
    running=true;
    LayerWorkQueue.run(host,DrawingView::nativeRedo,true,"Rehaciendo…","No se pudo rehacer el cambio",()->{
      drawing.deselect();drawing.viewportChanged();
    },this::settled);
  }
}
