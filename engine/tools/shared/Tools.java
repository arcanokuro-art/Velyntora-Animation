package art.velyntora.core;

import android.graphics.drawable.Drawable;

/** Stable tool IDs preserve project/UI/JNI compatibility. */
final class Tools {
  private static final DrawingTool[] CONTROLLERS = {
    new BrushTool(),
    new RectangleTool(),
    new EllipseTool(),
    new LineCurveTool(),
    new BucketTool(),
    new EraserTool(),
    new PickerTool(),
    new FilledRectangleTool(),
    new FilledEllipseTool(),
    new RectangleSelectionTool(),
    new EllipseSelectionTool(),
    new MoveSelectionTool(),
    new MovePixelsTool(),
    new LassoSelectionTool(),
    new MagicWandTool(),
    new TextTool(),
    new RoundedRectangleTool(),
    new FilledRoundedRectangleTool(),
    new TriangleTool(),
    new FilledTriangleTool(),
    new PencilTool(),
    new PanTool(),
    new ZoomTool(),
    new GradientTool(),
    new FreeformTool(),
    new CircleTool(),
    new CloneTool(),
    new RecolorTool(),
    new RemoveAiTool()
  };

  static DrawingTool controller(int id) {
    if (id < 0 || id >= CONTROLLERS.length)
      throw new IllegalArgumentException("Unknown tool: " + id);
    return CONTROLLERS[id];
  }

  static Drawable icon(int id, android.content.Context context) {
    if (id == DrawingView.REMOVE_AI) return RemoveAiIcon.load(context);
    return icon(id);
  }

  static Drawable icon(int id) {
    switch (id) {
      case DrawingView.BRUSH:
        return new BrushIcon();
      case DrawingView.RECTANGLE:
        return new RectangleIcon();
      case DrawingView.ELLIPSE:
        return new EllipseIcon();
      case DrawingView.LINE:
        return new LineCurveIcon();
      case DrawingView.BUCKET:
        return new BucketIcon();
      case DrawingView.ERASER:
        return new EraserIcon();
      case DrawingView.PICKER:
        return new PickerIcon();
      case DrawingView.FILLED_RECTANGLE:
        return new FilledRectangleIcon();
      case DrawingView.FILLED_ELLIPSE:
        return new FilledEllipseIcon();
      case DrawingView.SELECT_RECTANGLE:
        return new RectangleSelectionIcon();
      case DrawingView.SELECT_ELLIPSE:
        return new EllipseSelectionIcon();
      case DrawingView.MOVE_SELECTION:
        return new MoveSelectionIcon();
      case DrawingView.MOVE_PIXELS:
        return new MovePixelsIcon();
      case DrawingView.SELECT_FREE:
        return new LassoSelectionIcon();
      case DrawingView.MAGIC_WAND:
        return new MagicWandIcon();
      case DrawingView.TEXT:
        return new TextIcon();
      case DrawingView.ROUNDED_RECTANGLE:
        return new RoundedRectangleIcon();
      case DrawingView.FILLED_ROUNDED_RECTANGLE:
        return new FilledRoundedRectangleIcon();
      case DrawingView.TRIANGLE:
        return new TriangleIcon();
      case DrawingView.FILLED_TRIANGLE:
        return new FilledTriangleIcon();
      case DrawingView.PENCIL:
        return new PencilIcon();
      case DrawingView.PAN:
        return new PanIcon();
      case DrawingView.ZOOM:
        return new ZoomIcon();
      case DrawingView.GRADIENT:
        return new GradientIcon();
      case DrawingView.FREEFORM:
        return new FreeformIcon();
      case DrawingView.CIRCLE:
        return new CircleIcon();
      case DrawingView.CLONE:
        return new CloneIcon();
      case DrawingView.RECOLOR:
        return new RecolorIcon();
      default:
        throw new IllegalArgumentException("Unknown tool: " + id);
    }
  }
}
