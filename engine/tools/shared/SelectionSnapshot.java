package art.velyntora.core;

/** Independent copy; future selection edits cannot mutate curve history. */
final class SelectionSnapshot {
  final boolean selected;
  final int kind;
  final float left, top, right, bottom;
  final android.graphics.Region region;
  final android.graphics.Path path;

  SelectionSnapshot(DrawingView v) {
    selected = v.hasSelection();
    kind = v.selectionTool;
    left = v.selectionLeft;
    top = v.selectionTop;
    right = v.selectionRight;
    bottom = v.selectionBottom;
    region = new android.graphics.Region(v.freeRegion);
    path = new android.graphics.Path(v.freePath);
  }

  void restore(DrawingView v) {
    if (!selected) return;
    v.selectionTool = kind;
    v.hasSelection = true;
    v.selectionLeft = left;
    v.selectionTop = top;
    v.selectionRight = right;
    v.selectionBottom = bottom;
    v.freeRegion.set(region);
    v.freePath.set(path);
    v.freeSelectionReady = kind == DrawingView.SELECT_FREE;
    v.wandBoundary.set(region.getBoundaryPath());
  }
}
