package art.velyntora.core.components.color.palette.saved;
public final class SavedPaletteTests {
  static void check(boolean value){if(!value)throw new AssertionError();}
  public static void main(String[] args){
    SavedPalette p=new SavedPalette();check(p.add(0x8034abef));check(p.add(0));check(p.add(0xff000000));
    check(!p.add(0x8034abef));check(p.colors().get(0)==0x8034abef);
    SavedPalette restored=SavedPalette.decode(p.encode());check(restored.colors().equals(p.colors()));
    check(restored.remove(0));check(!restored.remove(0));check(restored.colors().size()==2);
    check(SavedPalette.decode("bad,8034abef,8034ABEF,FFFFFFFF,123,00000000").colors().size()==3);
    check(SavedPalette.decode(null).colors().isEmpty());check(SavedPalette.decode("A".repeat(1000)).colors().isEmpty());
    SavedPalette full=new SavedPalette();for(int i=0;i<64;i++)check(full.add(i));check(!full.add(65));
    check(SavedPalette.decode(full.encode()).colors().equals(full.colors()));
    full.remove(10);check(full.add(65));check(full.colors().get(63)==65);
    try{full.colors().clear();throw new AssertionError();}catch(UnsupportedOperationException expected){}
    System.out.println("Saved palette: ARGB, order, persistence codec, duplicates, removal, corruption and capacity passed");
  }
}
