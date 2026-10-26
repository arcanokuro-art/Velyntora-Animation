package art.velyntora.core;

import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

/** Wand-only confirmation controls; independent from Line/Curve. */
final class SelectionConfirmationControls {
  static void attach(MainActivity host,LinearLayout options){
    DrawingView drawing=host.readDrawing();SelectionConfirmation session=drawing.readSelectionConfirmation();
    Button confirm=host.iconButton(options,"Confirmar selección","confirm",session::confirm);
    Button cancel=host.iconButton(options,"Cancelar movimientos de selección","cancel",session::cancel);
    session.changed=()->{
      int tool=drawing.currentTool();
      boolean selectionTool=tool==DrawingView.MAGIC_WAND || tool==DrawingView.MOVE_PIXELS || tool==DrawingView.MOVE_SELECTION;
      int visibility=session.active() && selectionTool?View.VISIBLE:View.GONE;
      confirm.setVisibility(visibility);cancel.setVisibility(visibility);
    };
    session.changed.run();
  }
  private SelectionConfirmationControls(){}
}
