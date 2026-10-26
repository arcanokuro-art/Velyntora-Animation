package art.velyntora.core;

/** Layout policy in dp; overlays never consume canvas width. */
final class WorkspaceLayout {
  final boolean inline, compact;
  final int toolsWidth, layersWidth, canvasWidth;

  WorkspaceLayout(int widthDp) {
    this(widthDp, 800, 1f);
  }

  WorkspaceLayout(int widthDp, int heightDp, float fontScale) {
    if (widthDp < 1 || heightDp < 1 || !Float.isFinite(fontScale) || fontScale <= 0)
      throw new IllegalArgumentException("Invalid window dimensions");
    toolsWidth = Math.min(102, Math.max(48, widthDp - 48));
    layersWidth =
        Math.min(
            Math.min(320, Math.round(264 * Math.max(1f, Math.min(2f, fontScale)))),
            Math.max(48, widthDp - 48));
    inline = widthDp >= 720 && widthDp - toolsWidth - layersWidth >= 240;
    compact = heightDp < 480;
    canvasWidth = inline ? widthDp - toolsWidth - layersWidth : widthDp;
  }
}
