package app.velyntora.animation.frames;
import java.util.*;
/** Ordered frame identities; pixels and layers live in the shared drawing engine. */
public final class FrameSequence {
 private final ArrayList<String> frames=new ArrayList<>();
 private int selected, fps=12;
 public FrameSequence(){frames.add(UUID.randomUUID().toString());}
 public int size(){return frames.size();}
 public int selected(){return selected;}
 public String current(){return frames.get(selected);}
 public String id(int index){return frames.get(index);}
 public List<String> ids(){return Collections.unmodifiableList(frames);}
 public void select(int index){if(index<0||index>=size())throw new IllegalArgumentException("Fotograma inválido");selected=index;}
 public String insert(){String id=UUID.randomUUID().toString();frames.add(++selected,id);return id;}
 public String delete(){if(size()==1)throw new IllegalStateException("Debe quedar un fotograma");String id=frames.remove(selected);selected=Math.min(selected,size()-1);return id;}
 public void move(int delta){int next=selected+delta;if(next<0||next>=size())return;Collections.swap(frames,selected,next);selected=next;}
 public int fps(){return fps;}
 public void fps(int value){if(value<1||value>60)throw new IllegalArgumentException("FPS: 1–60");fps=value;}
 public void restore(List<String> ids,int selection,int rate){
  if(ids.isEmpty()||ids.size()>10000||new HashSet<>(ids).size()!=ids.size())throw new IllegalArgumentException("Secuencia inválida");
  for(String id:ids)if(!id.matches("[a-f0-9-]{36}"))throw new IllegalArgumentException("Identidad inválida");
  if(selection<0||selection>=ids.size()||rate<1||rate>60)throw new IllegalArgumentException("Metadatos inválidos");
  frames.clear();frames.addAll(ids);selected=selection;fps=rate;
 }
}
