package art.velyntora.core;

import java.util.TreeMap;

/** Smooth monotone cubic interpolation between user control points. */
final class ToneCurve {
  final TreeMap<Integer, Integer> points = new TreeMap<>();

  ToneCurve() {
    reset();
  }

  void reset() {
    points.clear();
    points.put(0, 0);
    points.put(255, 255);
  }

  void set(int x, int y) {
    points.put(Math.max(0, Math.min(255, x)), Math.max(0, Math.min(255, y)));
  }

  void remove(int x) {
    if (x != 0 && x != 255) points.remove(x);
  }

  int[] table() {
    int n = points.size(), i = 0;
    double[] x = new double[n], y = new double[n], d = new double[n - 1], m = new double[n];
    for (var p : points.entrySet()) {
      x[i] = p.getKey();
      y[i++] = p.getValue();
    }
    for (i = 0; i < n - 1; ++i) d[i] = (y[i + 1] - y[i]) / (x[i + 1] - x[i]);
    m[0] = d[0];
    m[n - 1] = d[n - 2];
    for (i = 1; i < n - 1; ++i) {
      if (d[i - 1] * d[i] <= 0) m[i] = 0;
      else {
        double previous = x[i] - x[i - 1],
            next = x[i + 1] - x[i],
            w1 = 2 * next + previous,
            w2 = next + 2 * previous;
        m[i] = (w1 + w2) / (w1 / d[i - 1] + w2 / d[i]);
      }
    }
    int[] map = new int[256];
    int segment = 0;
    for (i = 0; i < 256; ++i) {
      while (segment < n - 2 && i > x[segment + 1]) ++segment;
      double h = x[segment + 1] - x[segment], t = (i - x[segment]) / h, t2 = t * t, t3 = t2 * t;
      double value =
          (2 * t3 - 3 * t2 + 1) * y[segment]
              + (t3 - 2 * t2 + t) * h * m[segment]
              + (-2 * t3 + 3 * t2) * y[segment + 1]
              + (t3 - t2) * h * m[segment + 1];
      map[i] = Math.max(0, Math.min(255, (int) Math.round(value)));
    }
    return map;
  }
}
