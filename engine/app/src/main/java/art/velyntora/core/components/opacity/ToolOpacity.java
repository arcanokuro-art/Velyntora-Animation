package art.velyntora.core;

/** Independent opacity values indexed by the stable tool IDs. */
final class ToolOpacity {
  private final int[] percent = new int[29];
  ToolOpacity() { java.util.Arrays.fill(percent, 100); }
  static boolean supports(int tool) {
    switch (tool) {
      case 0: case 1: case 2: case 3: case 4: case 5: case 7: case 8:
      case 15: case 16: case 17: case 18: case 19: case 20: case 23:
      case 24: case 25: case 26: case 27: return true;
      default: return false;
    }
  }
  int get(int tool) { return supports(tool) ? percent[tool] : 100; }
  void set(int tool, int value) {
    if (supports(tool)) percent[tool] = Math.max(0, Math.min(100, value));
  }
}
