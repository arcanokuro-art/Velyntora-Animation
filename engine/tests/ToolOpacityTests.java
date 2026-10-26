package art.velyntora.core;
public final class ToolOpacityTests {
  public static void main(String[] args) {
    ToolOpacity state = new ToolOpacity();int supported=0;
    for(int i=0;i<29;i++) {
      if(ToolOpacity.supports(i)) {
        supported++;
        if(state.get(i)!=100) throw new AssertionError();
        state.set(i,i);
      }
    }
    if(supported!=19) throw new AssertionError(supported);
    for(int i=0;i<29;i++) if(state.get(i)!=(ToolOpacity.supports(i)?i:100)) throw new AssertionError(i);
    state.set(0,-1);state.set(3,101);state.set(28,20);
    if(state.get(0)!=0||state.get(3)!=100||state.get(28)!=100||state.get(-1)!=100) throw new AssertionError();
    System.out.println("Independent tool opacity and supported IDs passed");
  }
}
