package app.velyntora.animation.playback;

import app.velyntora.animation.model.AnimationSequence;

/** Monotonic time + exposures; rendering delay does not change animation speed. */
public final class PlaybackClock {
  private final AnimationSequence sequence;
  private final long started;
  private final int firstTick;

  public PlaybackClock(AnimationSequence sequence, long started) {
    this.sequence = sequence.copy();
    this.started = started;
    firstTick = sequence.firstTick(sequence.selected());
  }

  public int frame(long now) {
    return sequence.frameAtTick(firstTick + Math.max(0, now - started) * sequence.fps() / 1000);
  }
}
