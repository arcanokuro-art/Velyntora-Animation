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



final class FileMenu {
 private final MainActivity host;
 FileMenu(MainActivity host) {this.host=host;}

  void populate(LinearLayout menus) {
    host.menu(
        menus,
        "Archivo",
        new String[] {
          "Nuevo…",
          "Abrir archivo (detección automática)",
          "Insertar imagen como capa…",
          "Abrir TGA",
          "Abrir TIFF",
          "Abrir ICO",
          "Abrir Netpbm (PBM/PGM/PPM)",
          "Abrir OpenRaster",
          "Guardar OpenRaster",
          "Guardar PNG",
          "Guardar JPEG",
          "Guardar WebP",
          "Guardar BMP",
          "Guardar TGA",
          "Guardar TIFF",
          "Guardar GIF (imagen fija)",
          "Guardar ICO (hasta 256 px)",
          "Guardar PPM",
          "Abrir proyecto",
          "Guardar proyecto"
        },
        new Runnable[] {
          () -> host.protectDocument(() -> host.configureDimensions(0)),
          host::openImage,
          () -> host.readDrawing().placedImage().pick(),
          host::openTga,
          host::openTiff,
          host::openIco,
          host::openPpm,
          () -> host.openRasterPicker(false),
          () -> host.openRasterPicker(true),
          host::savePng,
          () -> host.saveImage("image/jpeg", "dibujo.jpg", host.readSAVE_JPEG()),
          () -> host.saveImage("image/webp", "dibujo.webp", host.readSAVE_WEBP()),
          () -> host.saveImage("image/bmp", "dibujo.bmp", host.readSAVE_BMP()),
          () -> host.saveImage("image/x-tga", "dibujo.tga", host.readSAVE_TGA()),
          () -> host.saveImage("image/tiff", "dibujo.tiff", host.readSAVE_TIFF()),
          () -> {
            host.message("GIF: colores reducidos y transparencia sin semitransparencias");
            host.saveImage("image/gif", "dibujo.gif", host.readSAVE_GIF());
          },
          () -> host.saveImage("image/vnd.microsoft.icon", "dibujo.ico", host.readSAVE_ICO()),
          () -> host.saveImage("image/x-portable-pixmap", "dibujo.ppm", host.readSAVE_PPM()),
          () -> host.projectPicker(false),
          () -> host.projectPicker(true)
        });
  }
}
