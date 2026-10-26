package art.velyntora.core;

/** Tracks only wand movement checkpoints. Never rewinds unrelated edits. */
final class SelectionConfirmation {
  private final DrawingView drawing;
  private boolean active;
  private int movements;
  private long expectedRevision;
  Runnable changed;
  SelectionConfirmation(DrawingView drawing){this.drawing=drawing;}
  boolean active(){return active && drawing.hasSelection() && drawing.selectionTool==DrawingView.MAGIC_WAND;}
  private void notifyChanged(){if(changed!=null)changed.run();}
  void begin(){
    active=drawing.hasSelection() && drawing.selectionTool==DrawingView.MAGIC_WAND;
    movements=0;expectedRevision=DrawingView.nativeRevision();notifyChanged();
  }
  void forget(){active=false;movements=0;notifyChanged();}
  void beforeMove(){
    if(drawing.selectionTool!=DrawingView.MAGIC_WAND)return;
    if(!active || expectedRevision!=DrawingView.nativeRevision())begin();
  }
  void moved(){if(active){movements++;expectedRevision=DrawingView.nativeRevision();notifyChanged();}}
  void toolChanged(int tool){
    if(tool!=DrawingView.MAGIC_WAND && tool!=DrawingView.MOVE_PIXELS && tool!=DrawingView.MOVE_SELECTION
        && tool!=DrawingView.PAN && tool!=DrawingView.ZOOM && tool!=DrawingView.PICKER)forget();
    else notifyChanged();
  }
  void confirm(){
    drawing.readSelectionDragPreview().clear();forget();drawing.deselect();
  }
  void cancel(){
    if(!(drawing.getContext() instanceof MainActivity host) || host.readProjectProgress()!=null)return;
    if(!active()){confirm();return;}
    if(expectedRevision!=DrawingView.nativeRevision()){
      host.message("Los movimientos ya se confirmaron al realizar otra edición");confirm();return;
    }
    int count=movements;
    if(count==0){confirm();return;}
    long revision=expectedRevision;
    LayerWorkQueue.run(host,()->{
      if(DrawingView.nativeRevision()!=revision)return false;
      int undone=0;
      while(undone<count){
        if(!DrawingView.nativeUndo()){
          while(undone-->0)DrawingView.nativeRedo();
          return false;
        }
        undone++;
      }
      return true;
    },true,"Cancelando movimientos de selección…","No se pudieron cancelar los movimientos",this::confirm);
  }
}
