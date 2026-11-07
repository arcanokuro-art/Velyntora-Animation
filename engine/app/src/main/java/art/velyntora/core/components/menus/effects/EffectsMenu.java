package art.velyntora.core;

import android.widget.LinearLayout;

final class EffectsMenu {
  private final MainActivity host;
  EffectsMenu(MainActivity host) { this.host = host; }
  void populate(LinearLayout menus) { host.button(menus, "Efectos", this::show); }

  void show() {
    AdjustmentDialogs effects = new AdjustmentDialogs(host, host.readDrawing());
    MenuChoices.show(host, "Efectos", new String[]{
      "Desenfoque y enfoque", "Distorsiones", "Estilos de ilustración",
      "Texturas y patrones", "Bordes y objetos", "Fotografía"
    },
    () -> MenuChoices.show(host, "Desenfoque y enfoque", new String[]{
      "Gaussiano", "De caja", "Movimiento", "Radial", "Zoom", "De lente", "Enfocar"
    }, () -> effects.blurDialog(0), () -> host.configureEffect(0), () -> effects.blurDialog(1),
       () -> effects.blurDialog(2), () -> effects.blurDialog(3), () -> effects.utilityDialog(1),
       () -> host.configureEffect(1)),
    () -> MenuChoices.show(host, "Distorsiones", new String[]{
      "Remolino", "Abombar / pellizcar", "Ondas radiales", "Cristales", "Escarcha",
      "Fragmentar", "Abolladuras", "Inversión polar"
    }, () -> effects.distortionDialog(0), () -> effects.distortionDialog(1), () -> effects.distortionDialog(2),
       () -> effects.distortionDialog(3), () -> effects.distortionDialog(4), () -> effects.utilityDialog(0),
       () -> effects.utilityDialog(2), () -> effects.utilityDialog(3)),
    () -> MenuChoices.show(host, "Estilos de ilustración", new String[]{
      "Pintura al óleo", "Boceto a lápiz", "Boceto a tinta", "Resplandor", "Repujado", "Relieve direccional"
    }, () -> effects.artisticDialog(0), () -> effects.artisticDialog(1), () -> effects.artisticDialog(2),
       () -> effects.artisticDialog(3), () -> host.configureEffect(3), () -> effects.utilityDialog(6)),
    () -> MenuChoices.show(host, "Texturas y patrones", new String[]{
      "Generar patrones…", "Pixelar", "Ruido", "Tramado ordenado"
    }, effects::openRenderMenu, () -> host.configureEffect(4), () -> host.configureEffect(5), () -> effects.utilityDialog(7)),
    () -> MenuChoices.show(host, "Bordes y objetos", new String[]{
      "Detectar bordes", "Contorno de bordes", "Alinear objeto", "Suavizar borde alfa", "Contorno de objeto"
    }, () -> host.configureEffect(2), () -> effects.utilityDialog(5), () -> effects.objectDialog(0),
       () -> effects.objectDialog(1), () -> effects.objectDialog(2)),
    () -> MenuChoices.show(host, "Fotografía", new String[]{
      "Retrato suave", "Reducir ojos rojos", "Reducir ruido", "Mediana / percentil", "Viñeta"
    }, () -> effects.artisticDialog(4), () -> effects.artisticDialog(6), () -> effects.utilityDialog(4),
       () -> effects.artisticDialog(5), () -> host.configureEffect(6)));
  }
}
