package app.velyntora.animation.model;

import java.util.*;

/** Pixel-independent animation state. Assets are immutable native documents. */
public final class AnimationSequence {
  public static final int MAX_FRAMES = AnimationLimits.MAX_FRAMES;

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
  private int[] tickEnds;

  public AnimationSequence(int fps) {
    fps(fps);
    frames.add(new Frame(uuid(), null, 1));
  }

  public static String uuid() {
    return UUID.randomUUID().toString();
  }

  private static final java.util.regex.Pattern ID_PATTERN = java.util.regex.Pattern.compile("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}");

  public static boolean validId(String id) {
    return id != null && ID_PATTERN.matcher(id).matches();
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
    if (value < 1 || value > AnimationLimits.MAX_FPS)
      throw new IllegalArgumentException("Los FPS deben estar entre 1 y 30");
    fps = value;
  }

  public void select(int i) {
    if (i < 0 || i >= size()) throw new IllegalArgumentException("Fotograma fuera de rango");
    selected = i;
  }

  public void insert(boolean duplicate) {
    if (size() >= MAX_FRAMES) throw new IllegalStateException("Límite de " + MAX_FRAMES + " fotogramas");
    tickEnds=null;
    Frame old = current();
    frames.add(
        ++selected, new Frame(uuid(), duplicate ? old.asset : null, duplicate ? old.exposure : 1));
  }

  public void delete() {
    if (size() == 1) throw new IllegalStateException("Debe quedar al menos un fotograma");
    tickEnds=null;
    frames.remove(selected);
    selected = Math.min(selected, size() - 1);
  }

  public void move(int delta) {
    int to = selected + delta;
    if (to < 0 || to >= size()) return;
    tickEnds=null;
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
    tickEnds=null;
  }

  private int[] timeline(){
    if(tickEnds==null){
      int[] ends=new int[frames.size()];int total=0;
      for(int i=0;i<ends.length;i++){total+=frames.get(i).exposure;ends[i]=total;}
      tickEnds=ends;
    }return tickEnds;
  }
  public int totalTicks(){int[] ends=timeline();return ends[ends.length-1];}
  public int firstTick(int index){
    if(index<0||index>frames.size())throw new IndexOutOfBoundsException();
    return index==0?0:timeline()[index-1];
  }
  public int frameAtTick(long tick){
    int[] ends=timeline();int at=(int)Math.floorMod(tick,(long)ends[ends.length-1]);
    int low=0,high=ends.length-1;
    while(low<high){int mid=(low+high)>>>1;if(at<ends[mid])high=mid;else low=mid+1;}
    return low;
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
        || rate > AnimationLimits.MAX_FPS) throw new IllegalArgumentException("Secuencia inválida");
    HashSet<String> ids = new HashSet<>();
    for (Frame f : source)
      if (f == null || !ids.add(f.id)) throw new IllegalArgumentException("Fotograma repetido");
    ArrayList<Frame> copy = new ArrayList<>(source);
    frames.clear();
    frames.addAll(copy);
    tickEnds=null;
    selected = selection;
    fps = rate;
  }
}
