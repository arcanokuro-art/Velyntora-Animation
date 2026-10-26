package art.velyntora.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;


/** Owns components/layers behavior; the host is the workspace composition facade. */
final class LayerOperations {
  private final DrawingView host;
  LayerOperations(DrawingView host) { this.host = host; }

  boolean setLayerOpacity(float opacity) {
    boolean ok = host.nativeSetLayerOpacity(opacity);
    if (ok) host.refresh();
    return ok;
  }

  float layerOpacity() {
    return host.nativeLayerOpacity();
  }

  Bitmap layerThumbnail(int index) {
    int[] pixels = host.nativeLayerThumbnail(index);
    if (pixels == null || pixels.length != 48 * 48) return null;
    return Bitmap.createBitmap(pixels, 48, 48, Bitmap.Config.ARGB_8888);
  }

  String layerName(int index) {
    String name = host.nativeLayerName(index);
    return name == null ? "Capa " + (index + 1) : name;
  }

  boolean renameLayer(int index, String name) {
    if (name == null || name.trim().isEmpty()) return false;
    boolean ok = host.nativeRenameLayer(index, name.trim());
    if (ok) host.refresh();
    return ok;
  }

  int layerCount() {
    return host.nativeLayerCount();
  }

  int activeLayer() {
    return host.nativeActiveLayer();
  }

  boolean layerVisible(int index) {
    return host.nativeLayerVisible(index);
  }

  boolean addLayer() {
    boolean ok = host.nativeAddLayer();
    if (ok) host.refresh();
    return ok;
  }

  boolean selectLayer(int index) {
    host.confirmCurve();
    boolean ok = host.nativeSelectLayer(index);
    if (ok) host.refresh();
    return ok;
  }

  boolean deleteLayer() {
    boolean ok = host.nativeDeleteLayer();
    if (ok) host.refresh();
    return ok;
  }

  boolean toggleLayer() {
    boolean ok = host.nativeToggleLayer();
    if (ok) host.refresh();
    return ok;
  }

  boolean moveLayer(int direction) {
    boolean ok = host.nativeMoveLayer(direction);
    if (ok) host.refresh();
    return ok;
  }

  boolean applyLayerProperties(int index,String name,boolean visible,float opacity,int mode) {
    boolean ok=host.nativeLayerProperties(index,name,visible,opacity,mode);
    if(ok)host.refresh();
    return ok;
  }
  int layerBlendMode(int index) {return host.nativeLayerBlendMode(index);}
  float layerOpacity(int index) {return host.nativeIndexedLayerOpacity(index);}
  boolean layerAction(int action) {
    host.confirmCurve();
    boolean ok=host.nativeLayerAction(action);
    if(ok)host.refresh();
    return ok;
  }
}
