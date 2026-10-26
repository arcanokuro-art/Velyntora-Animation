package art.velyntora.core;

/** Toolbar visibility only; does not change tool settings or drawing algorithms. */
final class ToolWidthPolicy {
  static boolean usesWidth(int tool){
    return switch(tool){
      case DrawingView.BRUSH,DrawingView.PENCIL,DrawingView.ERASER,DrawingView.CLONE,
        DrawingView.RECOLOR,DrawingView.REMOVE_AI,DrawingView.LINE,DrawingView.FREEFORM,
        DrawingView.RECTANGLE,DrawingView.ROUNDED_RECTANGLE,DrawingView.ELLIPSE,
        DrawingView.TRIANGLE,DrawingView.CIRCLE -> true;
      default -> false;
    };
  }
  private ToolWidthPolicy(){}
}
