package art.velyntora.core;

import art.velyntora.core.document.CanvasLimits;

/** Pixel dimensions and aspect-preserving calculations shared by document dialogs. */
final class DocumentDimensions {
  final int width, height;

  DocumentDimensions(int w, int h) {
    if (!CanvasLimits.accepts(w,h))
      throw new IllegalArgumentException("Usa hasta 6000 px por lado y 36 millones de píxeles");
    width = w;
    height = h;
  }

  private static int rounded(double value) {
    if (!Double.isFinite(value) || value < 0.5 || value >= CanvasLimits.MAX_SIDE + .5)
      throw new IllegalArgumentException("La dimensión calculada debe estar entre 1 y 6000 px");
    return (int) Math.round(value);
  }

  static DocumentDimensions fromWidth(int sourceWidth, int sourceHeight, int width) {
    if (sourceWidth <= 0 || sourceHeight <= 0)
      throw new IllegalArgumentException("Proporción inválida");
    return new DocumentDimensions(width, rounded((double) width * sourceHeight / sourceWidth));
  }

  static DocumentDimensions fromHeight(int sourceWidth, int sourceHeight, int height) {
    if (sourceWidth <= 0 || sourceHeight <= 0)
      throw new IllegalArgumentException("Proporción inválida");
    return new DocumentDimensions(rounded((double) height * sourceWidth / sourceHeight), height);
  }

  static DocumentDimensions fromPercent(int sourceWidth, int sourceHeight, double percentage) {
    if (sourceWidth <= 0 || sourceHeight <= 0 || !Double.isFinite(percentage) || percentage <= 0)
      throw new IllegalArgumentException("Introduce un porcentaje positivo");
    return new DocumentDimensions(
        rounded(sourceWidth * percentage / 100), rounded(sourceHeight * percentage / 100));
  }
}
