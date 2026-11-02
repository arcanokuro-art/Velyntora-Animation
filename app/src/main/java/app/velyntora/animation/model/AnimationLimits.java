package app.velyntora.animation.model;
/** Shared project and video-import limits. */
public final class AnimationLimits {
 public static final int MAX_FPS=30;
 public static final int MAX_VIDEO_MINUTES=130;
 public static final long MAX_VIDEO_DURATION_US=MAX_VIDEO_MINUTES*60L*1000000L;
 public static final int MAX_FRAMES=MAX_VIDEO_MINUTES*60*MAX_FPS;
 public static int clampFps(int value){return Math.max(1,Math.min(MAX_FPS,value));}
 private AnimationLimits(){}
}
