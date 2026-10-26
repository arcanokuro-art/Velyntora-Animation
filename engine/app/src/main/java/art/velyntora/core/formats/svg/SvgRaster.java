package art.velyntora.core;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;
import com.caverock.androidsvg.SVG;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Static SVG rasterization at intrinsic size; no external files or entity declarations. */
final class SvgRaster {
  static Bitmap read(InputStream input) throws IOException {
    byte[] bytes = OpenRasterArchive.bounded(input, 4194304);
    String text = new String(bytes, StandardCharsets.UTF_8);
    if (text.contains("<!DOCTYPE") || text.contains("<!ENTITY"))
      throw new IOException("SVG con declaraciones externas no compatible");
    try {
      SVG svg = SVG.getFromInputStream(new ByteArrayInputStream(bytes));
      float width = svg.getDocumentWidth(), height = svg.getDocumentHeight();
      RectF box = svg.getDocumentViewBox();
      if (!(width > 0 && Float.isFinite(width)))
        width = box != null && box.width() > 0 ? box.width() : 512;
      if (!(height > 0 && Float.isFinite(height)))
        height = box != null && box.height() > 0 ? box.height() : 512;
      if (width > Integer.MAX_VALUE || height > Integer.MAX_VALUE)
        throw new IOException("SVG demasiado grande");
      int sample = ImageDecodePolicy.sampleSize((int) Math.ceil(width), (int) Math.ceil(height));
      int w = Math.max(1, (int) Math.ceil(width / sample)),
          h = Math.max(1, (int) Math.ceil(height / sample));
      Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
      try {
        Canvas canvas = new Canvas(bitmap);
        canvas.drawPicture(svg.renderToPicture(w, h));
        return bitmap;
      } catch (RuntimeException error) {
        bitmap.recycle();
        throw error;
      }
    } catch (Exception e) {
      if (e instanceof IOException) throw (IOException) e;
      throw new IOException("SVG no compatible", e);
    }
  }
}
