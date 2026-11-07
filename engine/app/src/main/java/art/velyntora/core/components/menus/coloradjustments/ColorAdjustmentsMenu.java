package art.velyntora.core;

import android.widget.LinearLayout;

/** Basic color adjustments and grouped access to the existing advanced operations. */
final class ColorAdjustmentsMenu {
  private final MainActivity host;
  private final AdjustmentDialogs adjustments;
  ColorAdjustmentsMenu(MainActivity host) {
    this.host = host;
    this.adjustments = new AdjustmentDialogs(host, host.readDrawing());
  }
  void populate(LinearLayout menus) { host.button(menus, "Color", this::show); }

  private void show() {
    MenuChoices.show(host, "Color", new String[]{
      "Brillo y contraste…", "Tono, saturación y luminosidad…", "Niveles…", "Curvas…", "Escala de grises (capa)", "Blanco y negro por umbral…", "Invertir colores de capa", "Sepia (capa)", "Avanzado"
    }, () -> adjustments.openColorAdjustment(5),
    () -> adjustments.openColorAdjustment(4),
    this::levels,
    () -> adjustments.openColorAdjustment(0),
    () -> {
            if (!host.readDrawing().grayscaleActive())
              host.message("La capa ya está en escala de grises o está vacía");
          },
    host::configureThreshold,
    () -> {
            if (!host.readDrawing().invertActiveColors()) host.message("No hay colores visibles para invertir");
          },
    () -> {
            if (!host.readDrawing().sepiaActive()) host.message("La capa no tiene cambios para aplicar sepia");
          },
    this::advanced);
  }

  private void levels() {
    MenuChoices.show(host, "Niveles", new String[]{
      "Niveles por canal…", "Niveles automáticos…", "Niveles personalizados…", "Normalizar colores de capa"
    }, () -> adjustments.openColorAdjustment(1),
    () -> adjustments.openColorAdjustment(2),
    host::configureLevels,
    () -> {
            if (!host.readDrawing().normalizeActive()) host.message("No hay cambios para normalizar");
          });
  }

  private void advanced() {
    MenuChoices.show(host, "Color avanzado", new String[]{
      "Luz y exposición", "Canales RGB", "Paleta y tratamientos", "Transparencia de píxeles"
    }, this::light,
    this::channels,
    this::palette,
    this::alpha);
  }

  private void light() {
    MenuChoices.show(host, "Luz y exposición", new String[]{
      "Gamma personalizada…", "Exposición personalizada…", "Límite de luces personalizado…", "Elevar canales RGB a 32"
    }, host::configureGamma,
    host::configureExposure,
    host::configureHighlightCeiling,
    () -> {
            if (!host.readDrawing().liftShadowsActive(32)) host.message("No hay cambios al elevar sombras");
          });
  }

  private void channels() {
    MenuChoices.show(host, "Canales RGB", new String[]{
      "Canales RGB personalizados…", "Balance RGB personalizado…", "Tono cálido (+15 rojo)", "Tono frío (+15 azul)", "Intercambiar canales", "Eliminar un canal", "Grises desde un canal"
    }, host::configureRgbOffsets,
    host::configureRgbBalance,
    () -> {
            if (!host.readDrawing().tintActive(15, 0, 0)) host.message("No hay cambios de tono");
          },
    () -> {
            if (!host.readDrawing().tintActive(0, 0, 15)) host.message("No hay cambios de tono");
          },
    this::swap,
    this::remove,
    this::gray);
  }

  private void swap() {
    MenuChoices.show(host, "Intercambiar canales", new String[]{
      "Intercambiar rojo y verde", "Intercambiar rojo y azul", "Intercambiar verde y azul"
    }, () -> {
            if (!host.readDrawing().swapChannelsActive(0)) host.message("No hay cambios de canales");
          },
    () -> {
            if (!host.readDrawing().swapChannelsActive(1)) host.message("No hay cambios de canales");
          },
    () -> {
            if (!host.readDrawing().swapChannelsActive(2)) host.message("No hay cambios de canales");
          });
  }

  private void remove() {
    MenuChoices.show(host, "Eliminar un canal", new String[]{
      "Eliminar canal rojo", "Eliminar canal verde", "Eliminar canal azul"
    }, () -> {
            if (!host.readDrawing().removeChannelActive(0)) host.message("El canal rojo ya está vacío");
          },
    () -> {
            if (!host.readDrawing().removeChannelActive(1)) host.message("El canal verde ya está vacío");
          },
    () -> {
            if (!host.readDrawing().removeChannelActive(2)) host.message("El canal azul ya está vacío");
          });
  }

  private void gray() {
    MenuChoices.show(host, "Grises desde un canal", new String[]{
      "Escala de grises desde rojo", "Escala de grises desde verde", "Escala de grises desde azul"
    }, () -> {
            if (!host.readDrawing().grayscaleFromChannelActive(0))
              host.message("No hay cambios de escala de grises");
          },
    () -> {
            if (!host.readDrawing().grayscaleFromChannelActive(1))
              host.message("No hay cambios de escala de grises");
          },
    () -> {
            if (!host.readDrawing().grayscaleFromChannelActive(2))
              host.message("No hay cambios de escala de grises");
          });
  }

  private void palette() {
    MenuChoices.show(host, "Paleta y tratamientos", new String[]{
      "Posterización RGB…", "Posterización uniforme…", "Solarizar personalizado…", "Cuantización personalizada…"
    }, () -> adjustments.openColorAdjustment(3),
    host::configurePosterization,
    host::configureSolarization,
    host::configureQuantization);
  }

  private void alpha() {
    MenuChoices.show(host, "Transparencia de píxeles", new String[]{
      "Reducir alfa de píxeles (80 %)", "Aumentar alfa de píxeles (120 %)"
    }, () -> {
            if (!host.readDrawing().adjustAlphaActive(80)) host.message("No hay cambios de alfa");
          },
    () -> {
            if (!host.readDrawing().adjustAlphaActive(120)) host.message("No hay cambios de alfa");
          });
  }
}
