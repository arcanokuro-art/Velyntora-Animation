package art.velyntora.core;

/** Original brush renderer, preserved without changes to its parameters. */
final class ClassicBrush {
  static void stroke(DrawingView v,float x0,float y0,float x1,float y1,float pressure) {
    DrawingView.nativeStyledStroke(
        x0,
        y0,
        x1,
        y1,
        v.brushRadius * pressure,
        v.color,
        v.brushOpacity,
        v.brushHardness,
        v.squareBrush,
        false);
  }
}
