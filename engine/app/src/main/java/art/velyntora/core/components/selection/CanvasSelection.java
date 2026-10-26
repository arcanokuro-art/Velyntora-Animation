package art.velyntora.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;


/** Owns components/selection behavior; the host is the workspace composition facade. */
final class CanvasSelection {
  private final DrawingView host;
  CanvasSelection(DrawingView host) { this.host = host; }

  byte[] selectionMask() {
    return SelectionSupport.selectionMask(host);
  }

  boolean transformSelection(float degrees, float sx, float sy) {
    return MovePixelsTool.transformSelection(host, degrees, sx, sy);
  }

  boolean hasSelection() {
    return host.readHasSelection()
        && host.readSelectionRight() - host.readSelectionLeft() >= 1f
        && host.readSelectionBottom() - host.readSelectionTop() >= 1f;
  }

  boolean shrinkSelectionOnePixel() {
    return SelectionSupport.shrinkSelectionOnePixel(host);
  }

  boolean expandSelectionOnePixel() {
    return SelectionSupport.expandSelectionOnePixel(host);
  }

  boolean invertSelection() {
    return SelectionSupport.invertSelection(host);
  }

  boolean selectionContains(int px, int py) {
    return SelectionSupport.selectionContains(host, px, py);
  }

  boolean trimActiveToFreeSelection() {
    return SelectionSupport.trimActiveToFreeSelection(host);
  }

  boolean trimActiveToEllipseSelection() {
    return SelectionSupport.trimActiveToEllipseSelection(host);
  }

  boolean trimActiveToRectSelection() {
    return SelectionSupport.trimActiveToRectSelection(host);
  }

  Bitmap copySelection() {
    return SelectionSupport.copySelection(host);
  }

  boolean eraseSelection() {
    return SelectionSupport.eraseSelection(host);
  }

  void selectAll() {
    SelectionSupport.selectAll(host);
  }

  void deselect() {
    host.readSelectionConfirmation().forget();
    SelectionSupport.deselect(host);
  }

  void enableSelectionMove() {
    host.writeTool(host.readMOVE_SELECTION());
    host.invalidate();
  }

  boolean moveSelectedPixels(int dx, int dy) {
    return PagedSelectionMove.run(host,dx,dy);
  }

  void selectMatchingRegion(int sx, int sy) {
    if((long)host.documentWidth()*host.documentHeight()>1048576)PagedWand.run(host,sx,sy);
    else {MagicWandTool.selectMatchingRegion(host,sx,sy);host.readSelectionConfirmation().begin();}
  }

  void translateSelectionMask(int dx, int dy) {
    MoveSelectionTool.translateSelectionMask(host, dx, dy);
  }

  void restoreMovedSelection() {
    MoveSelectionTool.restoreMovedSelection(host);
  }

  void updateMovedSelection(float x, float y) {
    MoveSelectionTool.updateMovedSelection(host, x, y);
  }

  void updateSelection(float x, float y) {
    SelectionSupport.updateSelection(host, x, y);
  }
}
