package art.velyntora.core;

import android.graphics.*;
import android.os.ParcelFileDescriptor;
import app.velyntora.animation.model.AnimationSequence;
import app.velyntora.animation.storage.AnimationProject;
import java.io.*;

/** Worker-side bridge: native layered documents + bounded proxies; no Android Views touched. */
final class FrameDocumentAdapter {
  private final DrawingView drawing;

  FrameDocumentAdapter(DrawingView drawing) {
    this.drawing = drawing;
  }

  String capture(AnimationProject project) throws IOException {
    String asset = AnimationSequence.uuid();
    try (ParcelFileDescriptor fd =
        ParcelFileDescriptor.open(
            project.nativeFile(asset),
            ParcelFileDescriptor.MODE_CREATE
                | ParcelFileDescriptor.MODE_TRUNCATE
                | ParcelFileDescriptor.MODE_READ_WRITE)) {
      if (!DrawingView.nativeSaveProject(fd.getFd()))
        throw new IOException("No se pudo guardar el fotograma con sus capas");
      fd.getFileDescriptor().sync();
    }
    int width = DrawingView.nativeWidth(), height = DrawingView.nativeHeight(), sample = 1;
    while ((width + sample - 1) / sample > 1024 || (height + sample - 1) / sample > 1024)
      sample *= 2;
    int w = (width + sample - 1) / sample, h = (height + sample - 1) / sample;
    int[] pixels = DrawingView.nativeSampleRegion(0, 0, sample, w, h);
    if (pixels == null || pixels.length != w * h) throw new IOException("Vista previa incompleta");
    Bitmap preview = Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888);
    try (FileOutputStream out = new FileOutputStream(project.previewFile(asset))) {
      if (!preview.compress(Bitmap.CompressFormat.PNG, 100, out))
        throw new IOException("No se pudo guardar la vista previa");
      out.getFD().sync();
    } finally {
      preview.recycle();
    }
    return asset;
  }

  void load(AnimationProject project, String asset) throws IOException {
    try (ParcelFileDescriptor fd =
        ParcelFileDescriptor.open(project.nativeFile(asset), ParcelFileDescriptor.MODE_READ_ONLY)) {
      if (!DrawingView.nativeOpenProject(fd.getFd()))
        throw new IOException("Fotograma nativo incompatible");
    }
  }

  void create(int width, int height) throws IOException {
    if (!DrawingView.nativeCreate(width, height))
      throw new IOException("No hay memoria suficiente para el lienzo");
  }

  long revision() {
    return DrawingView.nativeRevision();
  }

  void present() {
    drawing.projectOpened();
  }

  void writePng(OutputStream output) throws IOException {
    int width = DrawingView.nativeWidth(), height = DrawingView.nativeHeight();
    PngWriter.write(
        output,
        width,
        height,
        (y, row) -> {
          int[] pixels = DrawingView.nativeRegionPixels(0, y, width, 1);
          if (pixels == null || pixels.length != width)
            throw new IllegalStateException("Fila incompleta");
          System.arraycopy(pixels, 0, row, 0, width);
        });
  }
}
