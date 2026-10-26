package art.velyntora.core;

import art.velyntora.core.document.CanvasLimits;

import android.content.Context;
import android.graphics.*;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import java.io.*;

/** Android PNG codec and atomic native-document handoff for flat OpenRaster projects. */
final class OpenRasterProjects {
  static void transfer(Context context, DrawingView drawing, Uri uri, boolean save)
      throws IOException {
    File project = File.createTempFile("ora-project-", ".vlycore", context.getCacheDir()),
        archive = null;
    try {
      if (save) {
        try (ParcelFileDescriptor fd =
            ParcelFileDescriptor.open(
                project,
                ParcelFileDescriptor.MODE_READ_WRITE | ParcelFileDescriptor.MODE_TRUNCATE)) {
          if (!drawing.writeProject(fd.getFd()))
            throw new IOException("No se pudo capturar el proyecto");
        }
        try (InputStream input = new FileInputStream(project);
            OutputStream output = context.getContentResolver().openOutputStream(uri, "wt")) {
          if (output == null) throw new IOException("No se puede escribir");
          OpenRasterArchive.write(
              input, new BufferedOutputStream(output), OpenRasterProjects::writePng);
        }
      } else {
        archive = File.createTempFile("ora-input-", ".ora", context.getCacheDir());
        try (InputStream input = context.getContentResolver().openInputStream(uri);
            OutputStream output = new BufferedOutputStream(new FileOutputStream(archive))) {
          if (input == null) throw new IOException("No se puede leer");
          byte[] buffer = new byte[8192];
          int n;
          long total = 0;
          while ((n = input.read(buffer)) != -1) {
            total += n;
            if (total > 134217728) throw new IOException("Archivo demasiado grande");
            output.write(buffer, 0, n);
          }
        }
        try (OutputStream output = new FileOutputStream(project)) {
          OpenRasterArchive.read(archive, output, OpenRasterProjects::readPng);
        }
        try (ParcelFileDescriptor fd =
            ParcelFileDescriptor.open(project, ParcelFileDescriptor.MODE_READ_ONLY)) {
          if (!drawing.readProject(fd.getFd())) throw new IOException("Proyecto incompatible");
        }
      }
    } finally {
      project.delete();
      if (archive != null) archive.delete();
    }
  }

  private static void writePng(int width, int height, int[] pixels, OutputStream output)
      throws IOException {
    Bitmap bitmap = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
    try {
      if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
        throw new IOException("No se pudo comprimir PNG");
    } finally {
      bitmap.recycle();
    }
  }

  private static int[] readPng(InputStream input, int width, int height, int x, int y)
      throws IOException {
    byte[] bytes = OpenRasterArchive.bounded(input, 33554432);
    byte[] signature = {(byte) 137, 80, 78, 71, 13, 10, 26, 10};
    if (bytes.length < 8) throw new IOException("PNG incompleto");
    for (int i = 0; i < 8; i++) if (bytes[i] != signature[i]) throw new IOException("Capa sin PNG");
    BitmapFactory.Options bounds = new BitmapFactory.Options();
    bounds.inJustDecodeBounds = true;
    BitmapFactory.decodeByteArray(bytes, 0, bytes.length, bounds);
    if (bounds.outWidth < 1
        || bounds.outHeight < 1
        || bounds.outWidth > CanvasLimits.MAX_SIDE
        || bounds.outHeight > CanvasLimits.MAX_SIDE
        || (long) bounds.outWidth * bounds.outHeight > CanvasLimits.MAX_PIXELS)
      throw new IOException("Capa demasiado grande");
    Bitmap source = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    if (source == null) throw new IOException("PNG inválido");
    Bitmap destination = null;
    try {
      destination = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
      Canvas canvas = new Canvas(destination);
      canvas.drawBitmap(source, x, y, null);
      int[] pixels = new int[width * height];
      destination.getPixels(pixels, 0, width, 0, 0, width, height);
      return pixels;
    } finally {
      source.recycle();
      if (destination != null) destination.recycle();
    }
  }
}
