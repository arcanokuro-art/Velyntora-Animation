package art.velyntora.core;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import java.io.InputStream;
import java.io.OutputStream;



final class ColorAdjustmentsMenu {
 private final MainActivity host;
 ColorAdjustmentsMenu(MainActivity host) {this.host=host;}

  void populate(LinearLayout menus) {
    host.menu(
        menus,
        "Extras de color",
        new String[] {
          "Invertir colores de capa",
          "Escala de grises (capa)",
          "Sepia (capa)",
          "Aumentar brillo (+20)",
          "Reducir brillo (-20)",
          "Brillo personalizado…",
          "Aumentar contraste (+20)",
          "Reducir contraste (-20)",
          "Contraste personalizado…",
          "Blanco y negro (umbral 128)",
          "Umbral personalizado…",
          "Posterizar (4 niveles)",
          "Posterizar personalizado…",
          "Solarizar (umbral 128)",
          "Solarizar personalizado…",
          "Aumentar saturación (+20)",
          "Reducir saturación (-20)",
          "Saturación personalizada…",
          "Gamma clara (120 %)",
          "Gamma oscura (80 %)",
          "Gamma personalizada…",
          "Tono cálido (+15 rojo)",
          "Tono frío (+15 azul)",
          "Canales RGB personalizados…",
          "Intercambiar rojo y verde",
          "Intercambiar rojo y azul",
          "Intercambiar verde y azul",
          "Aumentar rojo (120 %)",
          "Aumentar verde (120 %)",
          "Aumentar azul (120 %)",
          "Balance RGB personalizado…",
          "Rotar tono (+30°)",
          "Rotar tono (-30°)",
          "Rotación de tono personalizada…",
          "Ajustar niveles (16–239)",
          "Niveles personalizados…",
          "Exposición +20 %",
          "Exposición -20 %",
          "Exposición personalizada…",
          "Escala de grises desde rojo",
          "Escala de grises desde verde",
          "Escala de grises desde azul",
          "Reducir alfa de píxeles (80 %)",
          "Aumentar alfa de píxeles (120 %)",
          "Eliminar canal rojo",
          "Eliminar canal verde",
          "Eliminar canal azul",
          "Normalizar colores de capa",
          "Cuantizar colores (paso 16)",
          "Cuantizar colores (paso 32)",
          "Cuantización personalizada…",
          "Limitar canales RGB a 224",
          "Límite de luces personalizado…",
          "Elevar canales RGB a 32",
          "Añadir rojo (+20)",
          "Añadir verde (+20)",
          "Añadir azul (+20)"
        },
        new Runnable[] {
          () -> {
            if (!host.readDrawing().invertActiveColors()) host.message("No hay colores visibles para invertir");
          },
          () -> {
            if (!host.readDrawing().grayscaleActive())
              host.message("La capa ya está en escala de grises o está vacía");
          },
          () -> {
            if (!host.readDrawing().sepiaActive()) host.message("La capa no tiene cambios para aplicar sepia");
          },
          () -> {
            if (!host.readDrawing().brightnessActive(20)) host.message("No hay cambios de brillo");
          },
          () -> {
            if (!host.readDrawing().brightnessActive(-20)) host.message("No hay cambios de brillo");
          },
          host::configureBrightness,
          () -> {
            if (!host.readDrawing().contrastActive(20)) host.message("No hay cambios de contraste");
          },
          () -> {
            if (!host.readDrawing().contrastActive(-20)) host.message("No hay cambios de contraste");
          },
          host::configureContrast,
          () -> {
            if (!host.readDrawing().thresholdActive(128)) host.message("La capa ya es blanco y negro o está vacía");
          },
          host::configureThreshold,
          () -> {
            if (!host.readDrawing().posterizeActive(4)) host.message("La capa no tiene cambios para posterizar");
          },
          host::configurePosterization,
          () -> {
            if (!host.readDrawing().solarizeActive(128)) host.message("La capa no tiene cambios para solarizar");
          },
          host::configureSolarization,
          () -> {
            if (!host.readDrawing().saturationActive(20)) host.message("No hay cambios de saturación");
          },
          () -> {
            if (!host.readDrawing().saturationActive(-20)) host.message("No hay cambios de saturación");
          },
          host::configureSaturation,
          () -> {
            if (!host.readDrawing().gammaActive(120)) host.message("No hay cambios de gamma");
          },
          () -> {
            if (!host.readDrawing().gammaActive(80)) host.message("No hay cambios de gamma");
          },
          host::configureGamma,
          () -> {
            if (!host.readDrawing().tintActive(15, 0, 0)) host.message("No hay cambios de tono");
          },
          () -> {
            if (!host.readDrawing().tintActive(0, 0, 15)) host.message("No hay cambios de tono");
          },
          host::configureRgbOffsets,
          () -> {
            if (!host.readDrawing().swapChannelsActive(0)) host.message("No hay cambios de canales");
          },
          () -> {
            if (!host.readDrawing().swapChannelsActive(1)) host.message("No hay cambios de canales");
          },
          () -> {
            if (!host.readDrawing().swapChannelsActive(2)) host.message("No hay cambios de canales");
          },
          () -> {
            if (!host.readDrawing().colorBalanceActive(120, 100, 100)) host.message("No hay cambios de balance");
          },
          () -> {
            if (!host.readDrawing().colorBalanceActive(100, 120, 100)) host.message("No hay cambios de balance");
          },
          () -> {
            if (!host.readDrawing().colorBalanceActive(100, 100, 120)) host.message("No hay cambios de balance");
          },
          host::configureRgbBalance,
          () -> {
            if (!host.readDrawing().hueRotateActive(30)) host.message("No hay cambios de tono");
          },
          () -> {
            if (!host.readDrawing().hueRotateActive(-30)) host.message("No hay cambios de tono");
          },
          host::configureHue,
          () -> {
            if (!host.readDrawing().levelsActive(16, 239)) host.message("No hay cambios de niveles");
          },
          host::configureLevels,
          () -> {
            if (!host.readDrawing().exposureActive(120)) host.message("No hay cambios de exposición");
          },
          () -> {
            if (!host.readDrawing().exposureActive(80)) host.message("No hay cambios de exposición");
          },
          host::configureExposure,
          () -> {
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
          },
          () -> {
            if (!host.readDrawing().adjustAlphaActive(80)) host.message("No hay cambios de alfa");
          },
          () -> {
            if (!host.readDrawing().adjustAlphaActive(120)) host.message("No hay cambios de alfa");
          },
          () -> {
            if (!host.readDrawing().removeChannelActive(0)) host.message("El canal rojo ya está vacío");
          },
          () -> {
            if (!host.readDrawing().removeChannelActive(1)) host.message("El canal verde ya está vacío");
          },
          () -> {
            if (!host.readDrawing().removeChannelActive(2)) host.message("El canal azul ya está vacío");
          },
          () -> {
            if (!host.readDrawing().normalizeActive()) host.message("No hay cambios para normalizar");
          },
          () -> {
            if (!host.readDrawing().quantizeActive(16)) host.message("No hay cambios al cuantizar");
          },
          () -> {
            if (!host.readDrawing().quantizeActive(32)) host.message("No hay cambios al cuantizar");
          },
          host::configureQuantization,
          () -> {
            if (!host.readDrawing().clampHighlightsActive(224)) host.message("No hay cambios al limitar colores");
          },
          host::configureHighlightCeiling,
          () -> {
            if (!host.readDrawing().liftShadowsActive(32)) host.message("No hay cambios al elevar sombras");
          },
          () -> {
            if (!host.readDrawing().adjustChannelActive(0, 20)) host.message("No hay cambios en rojo");
          },
          () -> {
            if (!host.readDrawing().adjustChannelActive(1, 20)) host.message("No hay cambios en verde");
          },
          () -> {
            if (!host.readDrawing().adjustChannelActive(2, 20)) host.message("No hay cambios en azul");
          }
        });
  }
}
