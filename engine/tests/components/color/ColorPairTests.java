package art.velyntora.core.components.color;
public final class ColorPairTests {
  public static void main(String[] args) {
    ColorPair original = new ColorPair(0x00ff8000, 0xff204080);
    ColorPair swapped = original.swapped();
    if (swapped.primary()!=original.secondary() || swapped.secondary()!=original.primary())
      throw new AssertionError("Swap must preserve both complete ARGB values");
    if (original.primary()!=0x00ff8000 || original.secondary()!=0xff204080)
      throw new AssertionError("The original immutable pair must remain unchanged");
    ColorPair restored=swapped.swapped();
    if (restored.primary()!=original.primary() || restored.secondary()!=original.secondary())
      throw new AssertionError("Swapping twice must restore the original pair");
    System.out.println("Color pair tests passed");
  }
}
