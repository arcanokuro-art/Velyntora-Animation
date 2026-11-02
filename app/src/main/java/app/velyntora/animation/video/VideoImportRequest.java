package app.velyntora.animation.video;

import android.net.Uri;
import app.velyntora.animation.model.AnimationLimits;

/** Validated extraction interval; end is exclusive and timestamps never accumulate rounding. */
public final class VideoImportRequest {
  public final Uri uri;
  public final String title;
  public final long startUs, endUs;
  public final int fps, width, height, frameCount;

  public VideoImportRequest(Uri uri, String title, long startUs, long endUs,
      int fps, int width, int height) {
    if (uri == null || startUs < 0 || endUs <= startUs || fps < 1 || fps > AnimationLimits.MAX_FPS
        || width < 1 || height < 1 || width > 6000 || height > 6000)
      throw new IllegalArgumentException("Revisa el intervalo, los FPS y las dimensiones");
    long span=endUs-startUs;
    if(span>AnimationLimits.MAX_VIDEO_DURATION_US)
      throw new IllegalArgumentException("El fragmento puede durar como máximo 130 minutos");
    long count=(span*fps+999999L)/1000000L;
    if(count<1||count>AnimationLimits.MAX_FRAMES)
      throw new IllegalArgumentException("El fragmento supera los 234 000 fotogramas");
    this.uri=uri;this.title=title;this.startUs=startUs;this.endUs=endUs;
    this.fps=fps;this.width=width;this.height=height;this.frameCount=(int)count;
  }
  public long timestampUs(int index) {
    if (index < 0 || index >= frameCount) throw new IndexOutOfBoundsException();
    return startUs + index * 1000000L / fps;
  }
}
