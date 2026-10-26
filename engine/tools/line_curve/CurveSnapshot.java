package art.velyntora.core;

/** Confirmed curve metadata used to reopen its editable draft after undo. */
final class CurveSnapshot {
  final CurveDraft geometry;
  final int color;
  final float radius, opacity;
  final SelectionSnapshot selection;

  CurveSnapshot(CurveDraft g, int c, float r, float o, SelectionSnapshot sel) {
    geometry = g.copy();
    color = c;
    radius = r;
    opacity = o;
    selection = sel;
  }
}
