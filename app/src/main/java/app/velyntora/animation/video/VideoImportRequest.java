package app.velyntora.animation.video;

import android.net.Uri;

/** Validated extraction interval; end is exclusive and timestamps never accumulate rounding. */
public final class VideoImportRequest {
  public final Uri uri;
  public final String title;
  public final long startUs, endUs;
  public final int fps, width, height, frameCount;

  public VideoImportRequest(Uri uri, String title, long startUs, long endUs,
      int fps, int width, int height) {
    if (uri == null || startUs < 0 || endUs <= startUs || fps < 1 || fps > 60
        || width < 1 || height < 1 || width > 6000 || height > 6000)
      throw new IllegalArgumentException("Revisa el intervalo, los FPS y las dimensiones");
    long span=endUs-startUs;
    if(span>2000L*1000000L/fps)
      throw new IllegalArgumentException("El fragmento supera el límite de 2000 fotogramas");
    long count = (span * fps + 999999L) / 1000000L;
    if (count < 1 || count > 2000)
      throw new IllegalArgumentException("El fragmento debe generar entre 1 y 2000 fotogramas");
    this.uri=uri;this.title=title;this.startUs=startUs;this.endUs=endUs;
    this.fps=fps;this.width=width;this.height=height;this.frameCount=(int)count;
  }
  public long timestampUs(int index) {
    if (index < 0 || index >= frameCount) throw new IndexOutOfBoundsException();
    return startUs + index * 1000000L / fps;
  }
}
