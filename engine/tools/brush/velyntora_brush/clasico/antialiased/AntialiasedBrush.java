package art.velyntora.core;

/** Edge coverage is rasterized in canvas pixels; zoom never changes the stroke. */
final class AntialiasedBrush {
  static void stroke(DrawingView v,float x0,float y0,float x1,float y1,float pressure) {
    DrawingView.nativeAntialiasedStroke(x0,y0,x1,y1,v.brushRadius*pressure,
        v.color,v.brushOpacity,v.brushHardness,v.squareBrush);
  }
}
