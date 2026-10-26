package art.velyntora.core.components.color;

/** Immutable primary/secondary ARGB colors, independent of Android views. */
public final class ColorPair {
  private final int primary;
  private final int secondary;

  public ColorPair(int primary, int secondary) {
    this.primary = primary;
    this.secondary = secondary;
  }

  public int primary() {
    return primary;
  }

  public int secondary() {
    return secondary;
  }

  public ColorPair swapped() {
    return new ColorPair(secondary, primary);
  }
}
