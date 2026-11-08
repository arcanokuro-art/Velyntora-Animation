package art.velyntora.core;

import android.widget.LinearLayout;

final class ImageMenu {
  private final MainActivity host;
  ImageMenu(MainActivity host) { this.host = host; }
  void populate(LinearLayout menus) {
    host.button(menus, "Lienzo e imagen", () -> MenuChoices.show(host, "Lienzo e imagen",
      new String[]{"Redimensionar dibujo…", "Cambiar área del lienzo…",
        "Recortar documento a selección", "Girar o voltear capa…", "Recortar capa a selección…"},
      () -> host.configureDimensions(1), () -> host.configureDimensions(2),
      () -> { if (!host.readDrawing().cropDocument()) host.message("Selecciona el área del documento que quieres conservar"); },
      this::orientation, this::trim));
  }
  private void orientation() {
    MenuChoices.show(host, "Girar o voltear capa", new String[]{
      "Voltear horizontalmente", "Voltear verticalmente", "Girar 180°", "Girar 90° a la derecha", "Girar 90° a la izquierda"
    }, () -> changed(host.readDrawing().flipActiveHorizontal()),
       () -> changed(host.readDrawing().flipActiveVertical()),
       () -> changed(host.readDrawing().rotateActive180()),
       () -> changed(host.readDrawing().rotateActive90(true)),
       () -> changed(host.readDrawing().rotateActive90(false)));
  }
  private void trim() {
    MenuChoices.show(host, "Recortar capa a selección", new String[]{
      "Conservar selección rectangular", "Conservar selección elíptica", "Conservar selección libre o varita"
    }, () -> changed(host.readDrawing().trimActiveToRectSelection()),
       () -> changed(host.readDrawing().trimActiveToEllipseSelection()),
       () -> changed(host.readDrawing().trimActiveToFreeSelection()));
  }
  private void changed(boolean changed) {
    if (!changed) host.message("No hay cambios. Comprueba la capa o la selección activa.");
  }
}
