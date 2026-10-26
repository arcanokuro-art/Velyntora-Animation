package art.velyntora.core.document;

/** Shared document budget for creation, resizing, import and export. */
public final class CanvasLimits {
  public static final int MAX_SIDE=6000;
  public static final long MAX_PIXELS=36_000_000L;
  private CanvasLimits(){}
  public static boolean accepts(int width,int height){
    return width>0 && height>0 && width<=MAX_SIDE && height<=MAX_SIDE
        && (long)width*height<=MAX_PIXELS;
  }
}
