package art.velyntora.core.components.color.palette.saved;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Ordered unique ARGB colors; malformed stored entries are ignored. */
public final class SavedPalette {
  public static final int LIMIT=64;
  private final ArrayList<Integer> colors=new ArrayList<>();
  public List<Integer> colors(){return Collections.unmodifiableList(colors);}
  public boolean contains(int color){return colors.contains(color);}
  public boolean add(int color){
    if(contains(color)||colors.size()>=LIMIT)return false;
    colors.add(color);return true;
  }
  public boolean remove(int color){return colors.remove(Integer.valueOf(color));}
  public String encode(){
    StringBuilder out=new StringBuilder();
    for(int color:colors){if(out.length()>0)out.append(',');out.append(String.format(Locale.US,"%08X",color));}
    return out.toString();
  }
  public static SavedPalette decode(String encoded){
    SavedPalette out=new SavedPalette();if(encoded==null)return out;
    // Stored payload is bounded; never allocate from an untrusted arbitrary length.
    if(encoded.length()>LIMIT*9)return out;
    for(String token:encoded.split(",")){
      if(!token.matches("[0-9a-fA-F]{8}"))continue;
      try{out.add((int)Long.parseLong(token,16));}catch(NumberFormatException ignored){}
    }
    return out;
  }
}
