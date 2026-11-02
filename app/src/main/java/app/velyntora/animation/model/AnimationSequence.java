package app.velyntora.animation.model;

import java.util.*;

/** Pixel-independent animation state. Assets are immutable native documents. */
public final class AnimationSequence {
  public static final int MAX_FRAMES = 2000;

  public static final class Frame {
    public final String id, asset;
    public final int exposure;

    public Frame(String id, String asset, int exposure) {
      if (!validId(id) || (asset != null && !validId(asset)) || exposure < 1 || exposure > 120)
        throw new IllegalArgumentException("Fotograma inválido");
      this.id = id;
      this.asset = asset;
      this.exposure = exposure;
    }
  }

  private final ArrayList<Frame> frames = new ArrayList<>();
  private int selected, fps;

  public AnimationSequence(int fps) {
    fps(fps);
    frames.add(new Frame(uuid(), null, 1));
  }

  public static String uuid() {
    return UUID.randomUUID().toString();
  }

  public static boolean validId(String id) {
    return id != null && id.matches("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}");
  }

  public int size() {
    return frames.size();
  }

  public int selected() {
    return selected;
  }

  public Frame frame(int i) {
    return frames.get(i);
  }

  public Frame current() {
    return frame(selected);
  }

  public int fps() {
    return fps;
  }

  public void fps(int value) {
    if (value < 1 || value > 60)
      throw new IllegalArgumentException("Los FPS deben estar entre 1 y 60");
    fps = value;
  }

  public void select(int i) {
    if (i < 0 || i >= size()) throw new IllegalArgumentException("Fotograma fuera de rango");
    selected = i;
  }

  public void insert(boolean duplicate) {
    if (size() >= MAX_FRAMES) throw new IllegalStateException("Límite de 2000 fotogramas");
    Frame old = current();
    frames.add(
        ++selected, new Frame(uuid(), duplicate ? old.asset : null, duplicate ? old.exposure : 1));
  }

  public void delete() {
    if (size() == 1) throw new IllegalStateException("Debe quedar al menos un fotograma");
    frames.remove(selected);
    selected = Math.min(selected, size() - 1);
  }

  public void move(int delta) {
    int to = selected + delta;
    if (to < 0 || to >= size()) return;
    Collections.swap(frames, selected, to);
    selected = to;
  }

  public void asset(String value) {
    Frame f = current();
    frames.set(selected, new Frame(f.id, value, f.exposure));
  }

  public void exposure(int ticks) {
    Frame f = current();
    frames.set(selected, new Frame(f.id, f.asset, ticks));
  }

  public int totalTicks() {
    int n = 0;
    for (Frame f : frames) n += f.exposure;
    return n;
  }

  public int firstTick(int index) {
    int n = 0;
    for (int i = 0; i < index; i++) n += frame(i).exposure;
    return n;
  }

  public int frameAtTick(long tick) {
    long at = Math.floorMod(tick, totalTicks());
    for (int i = 0; i < size(); i++) {
      at -= frame(i).exposure;
      if (at < 0) return i;
    }
    return size() - 1;
  }

  public AnimationSequence copy() {
    AnimationSequence c = new AnimationSequence(fps);
    c.restore(this);
    return c;
  }

  public void restore(AnimationSequence source) {
    replace(source.frames, source.selected, source.fps);
  }

  public void replace(List<Frame> source, int selection, int rate) {
    if (source.isEmpty()
        || source.size() > MAX_FRAMES
        || selection < 0
        || selection >= source.size()
        || rate < 1
        || rate > 60) throw new IllegalArgumentException("Secuencia inválida");
    HashSet<String> ids = new HashSet<>();
    for (Frame f : source)
      if (f == null || !ids.add(f.id)) throw new IllegalArgumentException("Fotograma repetido");
    ArrayList<Frame> copy = new ArrayList<>(source);
    frames.clear();
    frames.addAll(copy);
    selected = selection;
    fps = rate;
  }
}
