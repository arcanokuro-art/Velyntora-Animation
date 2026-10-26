package app.velyntora.animation.playback;

/** Monotonic playback: delayed rendering skips expired frames rather than slowing time. */
public final class PlaybackClock {
  private final long start;
  private final int first, count, fps;

  public PlaybackClock(long start, int first, int count, int fps) {
    this.start = start;
    this.first = first;
    this.count = count;
    this.fps = fps;
  }

  public int frame(long now) {
    return (int) ((first + Math.max(0, now - start) * fps / 1000) % count);
  }
}
