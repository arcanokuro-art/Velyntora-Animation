package art.velyntora.core;

import art.velyntora.core.document.CanvasLimits;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;


/** Owns document/model behavior; the host is the workspace composition facade. */
final class CanvasDocument {
  private final DrawingView host;
  CanvasDocument(DrawingView host) { this.host = host; }

  boolean writeRecovery(int fd, long expectedRevision) {
    return host.nativeSaveRecovery(fd, expectedRevision);
  }

  boolean writeProject(int fd) {
    return host.nativeSaveProject(fd);
  }

  boolean readProject(int fd) {
    return host.nativeOpenProject(fd);
  }

  void projectOpened() {
    host.resetDocumentTools();
    host.deselect();
    host.refresh();
    host.fitCanvas();
  }

  long revision() {
    return host.nativeRevision();
  }

  int documentWidth() {
    return host.readCanvasWidth();
  }

  int documentHeight() {
    return host.readCanvasHeight();
  }

  boolean resizeDocument(int w, int h, boolean scalePixels) {
    return host.resizeDocument(w, h, scalePixels, false, 0);
  }

  boolean resizeDocument(int w, int h, boolean scalePixels, boolean bilinear, int anchor) {
    if (!host.resizeDocumentPixels(w, h, scalePixels, bilinear, anchor)) return false;
    host.documentResized();
    return true;
  }

  boolean resizeDocumentPixels(
      int w, int h, boolean scalePixels, boolean bilinear, int anchor) {
    return host.nativeResizeDocumentOptions(w, h, scalePixels, bilinear, anchor);
  }

  void documentResized() {
    host.deselect();
    host.refresh();
    host.fitCanvas();
  }

  boolean newDocument(int w, int h) {
    if (w <= 0 || h <= 0 || w > CanvasLimits.MAX_SIDE || h > CanvasLimits.MAX_SIDE || (long) w * h > CanvasLimits.MAX_PIXELS || !host.nativeCreate(w, h))
      return false;
    host.resetDocumentTools();
    host.deselect();
    host.refresh();
    host.fitCanvas();
    return true;
  }

  boolean cropDocument() {
    if (!host.hasSelection()) return false;
    int left = (int) Math.floor(host.readSelectionLeft()), top = (int) Math.floor(host.readSelectionTop());
    if (!host.nativeCropDocument(
        left, top, (int) Math.ceil(host.readSelectionRight()) - left, (int) Math.ceil(host.readSelectionBottom()) - top))
      return false;
    host.deselect();
    host.refresh();
    host.fitCanvas();
    return true;
  }

  boolean pasteBitmap(Bitmap source) {
    if (source == null || source.isRecycled()) return false;
    int w = source.getWidth(), h = source.getHeight();
    if (w <= 0
        || h <= 0
        || w > host.readCanvasWidth()
        || h > host.readCanvasHeight()
        || ((long) w * h) > host.readCanvasWidth() * host.readCanvasHeight()) return false;
    int[] data = new int[2 + w * h];
    data[0] = w;
    data[1] = h;
    source.getPixels(data, 2, w, 0, 0, w, h);
    int x = host.hasSelection() ? (int) host.readSelectionLeft() : (host.readCanvasWidth() - w) / 2;
    int y = host.hasSelection() ? (int) host.readSelectionTop() : (host.readCanvasHeight() - h) / 2;
    x = Math.max(0, Math.min(host.readCanvasWidth() - w, x));
    y = Math.max(0, Math.min(host.readCanvasHeight() - h, y));
    boolean ok = host.nativePasteSelection(data, x, y);
    if (ok) {
      host.deselect();
      host.refresh();
    }
    return ok;
  }

  boolean loadBitmap(Bitmap source) {
    if (source == null || source.isRecycled()) return false;
    int w = source.getWidth(), h = source.getHeight();
    if (w > CanvasLimits.MAX_SIDE || h > CanvasLimits.MAX_SIDE || (long) w * h > CanvasLimits.MAX_PIXELS) return false;
    int[] pixels = new int[w * h];
    source.getPixels(pixels, 0, w, 0, 0, w, h);
    if (!host.nativeLoadBitmap(w, h, pixels)) return false;
    host.resetDocumentTools();
    host.deselect();
    host.refresh();
    host.fitCanvas();
    return true;
  }

  Bitmap snapshot() {
    if (host.readStrokeRefreshPending()) host.refresh();
    host.confirmCurve();
    return nativeSnapshot();
  }
  static Bitmap nativeSnapshot(){
    int width=DrawingView.nativeWidth(),height=DrawingView.nativeHeight();
    Bitmap result=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
    try{
      int rows=Math.max(1,Math.min(128,1048576/width));
      for(int y=0;y<height;y+=rows){int n=Math.min(rows,height-y);
        int[] pixels=DrawingView.nativeRegionPixels(0,y,width,n);
        result.setPixels(pixels,0,width,0,y,width,n);
      }
      return result;
    }catch(RuntimeException|OutOfMemoryError error){result.recycle();throw error;}
  }
}
