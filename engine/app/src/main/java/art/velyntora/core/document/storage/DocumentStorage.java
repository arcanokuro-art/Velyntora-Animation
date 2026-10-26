package art.velyntora.core;

import art.velyntora.core.document.CanvasLimits;

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


/** Owns document/storage behavior; the host is the workspace composition facade. */
final class DocumentStorage {
  private final MainActivity host;
  DocumentStorage(MainActivity host) { this.host = host; }

  Bitmap decodeImage(Uri uri) throws java.io.IOException {
    BitmapFactory.Options bounds = new BitmapFactory.Options();
    bounds.inJustDecodeBounds = true;
    try (InputStream input = host.getContentResolver().openInputStream(uri)) {
      if (input == null) throw new java.io.IOException("No se puede abrir el archivo");
      BitmapFactory.decodeStream(input, null, bounds);
    }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0)
      throw new java.io.IOException("Dimensiones inválidas");
    int sample = ImageDecodePolicy.sampleSize(bounds.outWidth, bounds.outHeight);
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inSampleSize = sample;
    options.inPreferredConfig = Bitmap.Config.ARGB_8888;
    options.inMutable = true;
    try (InputStream input = host.getContentResolver().openInputStream(uri)) {
      if (input == null) throw new java.io.IOException("No se puede leer el archivo");
      Bitmap decoded = BitmapFactory.decodeStream(input, null, options);
      if (decoded == null) throw new java.io.IOException("Imagen no compatible");
      if (decoded.getWidth() > CanvasLimits.MAX_SIDE
          || decoded.getHeight() > CanvasLimits.MAX_SIDE
          || (long) decoded.getWidth() * decoded.getHeight() > CanvasLimits.MAX_PIXELS) {
        decoded.recycle();
        throw new java.io.IOException("Imagen demasiado grande");
      }
      int orientation = 1;
      try (InputStream metadata = host.getContentResolver().openInputStream(uri)) {
        if (metadata != null) orientation = ExifOrientation.read(metadata);
      } catch (java.io.IOException ignored) {
      }
      if (orientation == 1) return decoded;
      int width = decoded.getWidth(),
          height = decoded.getHeight(),
          outWidth = orientation >= 5 ? height : width,
          outHeight = orientation >= 5 ? width : height;
      int[] source = new int[width * height], target = new int[width * height];
      try {
        decoded.getPixels(source, 0, width, 0, 0, width, height);
        for (int y = 0; y < outHeight; y++)
          for (int x = 0; x < outWidth; x++)
            target[y * outWidth + x] =
                source[ExifOrientation.sourceIndex(x, y, width, height, orientation)];
        return Bitmap.createBitmap(target, outWidth, outHeight, Bitmap.Config.ARGB_8888);
      } finally {
        decoded.recycle();
      }
    }
  }

  void saveImage(String mime, String name, int request) {
    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    intent.setType(mime);
    intent.putExtra(Intent.EXTRA_TITLE, name);
    host.startActivityForResult(intent, request);
  }

  void projectPicker(boolean save) {
    Intent intent = new Intent(save ? Intent.ACTION_CREATE_DOCUMENT : Intent.ACTION_OPEN_DOCUMENT);
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    intent.setType("application/octet-stream");
    if (save) intent.putExtra(Intent.EXTRA_TITLE, "dibujo.vlycore");
    host.startActivityForResult(intent, save ? host.readSAVE_PROJECT() : host.readOPEN_PROJECT());
  }

  void openRasterPicker(boolean save) {
    Intent intent = new Intent(save ? Intent.ACTION_CREATE_DOCUMENT : Intent.ACTION_OPEN_DOCUMENT);
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    intent.setType(save ? "image/openraster" : "*/*");
    if (save) intent.putExtra(Intent.EXTRA_TITLE, "dibujo.ora");
    host.startActivityForResult(intent, save ? host.readSAVE_ORA() : host.readOPEN_ORA());
  }

  void transferOpenRaster(Uri uri, boolean save) {
    if (host.readProjectProgress() != null) {
      host.message("Espera a que termine la operación actual");
      return;
    }
    android.app.ProgressDialog progress = new android.app.ProgressDialog(host);
    progress.setMessage(save ? "Guardando OpenRaster…" : "Abriendo OpenRaster…");
    progress.setCancelable(false);
    host.writeProjectProgress(progress);
    progress.show();
    host.readDrawing().setEnabled(false);
    new Thread(
            () -> {
              host.fileName(uri);
              boolean success = false;
              try {
                OpenRasterProjects.transfer(host, host.readDrawing(), uri, save);
                success = true;
              } catch (Exception e) {
                success = false;
              }
              final boolean ok = success;
              host.runOnUiThread(
                  () -> {
                    if (!host.isDestroyed()) {
                      progress.dismiss();
                      host.writeProjectProgress(null);
                      host.readDrawing().setEnabled(true);
                      if (ok) {
                        if (!save) host.readDrawing().projectOpened();
                        host.markDocumentClean(host.fileName(uri));
                      }
                      host.message(
                          ok
                              ? "Proyecto OpenRaster listo"
                              : "OpenRaster incompatible, incompleto o demasiado grande; se admiten"
                                  + " capas normales sin grupos");
                    }
                  });
            },
            "velyntora-openraster")
        .start();
  }

  void transferProject(Uri uri, boolean save) {
    if (host.readProjectProgress() != null) {
      host.message("Espera a que termine la operación actual");
      return;
    }
    android.app.ProgressDialog progress = new android.app.ProgressDialog(host);
    progress.setMessage(save ? "Guardando proyecto…" : "Abriendo proyecto…");
    progress.setCancelable(false);
    host.writeProjectProgress(progress);
    progress.show();
    host.readDrawing().setEnabled(false);
    new Thread(
            () -> {
              host.fileName(uri);
              boolean success = false;
              try (android.os.ParcelFileDescriptor file =
                  host.getContentResolver().openFileDescriptor(uri, save ? "wt" : "r")) {
                if (file != null)
                  success =
                      save ? host.readDrawing().writeProject(file.getFd()) : host.readDrawing().readProject(file.getFd());
              } catch (Exception e) {
                success = false;
              }
              final boolean ok = success;
              host.runOnUiThread(
                  () -> {
                    if (!host.isDestroyed()) {
                      progress.dismiss();
                      host.writeProjectProgress(null);
                      host.readDrawing().setEnabled(true);
                      if (ok) {
                        if (!save) host.readDrawing().projectOpened();
                        host.markDocumentClean(host.fileName(uri));
                      }
                      host.message(
                          ok
                              ? (save
                                  ? "Proyecto guardado con capas"
                                  : "Proyecto abierto con capas")
                              : (save
                                  ? "No se pudo guardar el proyecto"
                                  : "Proyecto incompatible, incompleto o demasiado grande"));
                    }
                  });
            },
            "velyntora-project-io")
        .start();
  }

  void exportRaster(Uri uri, int request) {
    if (host.readProjectProgress() != null) {
      host.message("Espera a que termine la operación actual");
      return;
    }
    final int documentWidth=DrawingView.nativeWidth(),documentHeight=DrawingView.nativeHeight();
    final boolean streaming=request!=host.readSAVE_JPEG() && request!=host.readSAVE_WEBP() && request!=host.readSAVE_ICO();
    host.readDrawing().confirmCurve();
    android.app.ProgressDialog progress = new android.app.ProgressDialog(host);
    progress.setMessage("Exportando imagen…");
    progress.setCancelable(false);
    host.writeProjectProgress(progress);
    progress.show();
    host.readDrawing().setEnabled(false);
    new Thread(
            () -> {
              boolean success = false;
              Bitmap[] snapshot={null};
              try (OutputStream output = host.getContentResolver().openOutputStream(uri, "wt")) {
                if (output == null) throw new java.io.IOException("No output");
                java.io.BufferedOutputStream buffered = new java.io.BufferedOutputStream(output);
                final Bitmap image=streaming?null:CanvasDocument.nativeSnapshot();snapshot[0]=image;
                final int width=streaming?documentWidth:image.getWidth(),height=streaming?documentHeight:image.getHeight();
                RasterFileWriter.RowSource rows = (y,row)->{
                  if(streaming){int[] source=DrawingView.nativeRegionPixels(0,y,width,1);
                    if(source==null || source.length!=width)throw new IllegalStateException("Fila incompleta");
                    System.arraycopy(source,0,row,0,width);
                  }else image.getPixels(row,0,width,0,y,width,1);
                };
                if (request == host.readSAVE_TIFF())
                  TiffWriter.write(buffered, width, height, rows);
                else if (request == host.readSAVE_GIF())
                  GifWriter.write(buffered, width, height, rows);
                else if (request == host.readSAVE_PPM())
                  PpmCodec.write(buffered, width, height, rows);
                else if (request == host.readSAVE_ICO()) {
                  double scale =
                      Math.min(1.0, 256.0 / Math.max(image.getWidth(), image.getHeight()));
                  int w = Math.max(1, (int) Math.round(image.getWidth() * scale)),
                      h = Math.max(1, (int) Math.round(image.getHeight() * scale));
                  Bitmap icon = Bitmap.createScaledBitmap(image, w, h, true);
                  try {
                    IcoCodec.write(
                        buffered, w, h, (y, row) -> icon.getPixels(row, 0, w, 0, y, w, 1));
                  } finally {
                    if (icon != image) icon.recycle();
                  }
                } else if (request == host.readSAVE_BMP() || request == host.readSAVE_TGA())
                  RasterFileWriter.write(
                      buffered, width, height, request == host.readSAVE_TGA(), rows);
                else if(request==host.readSAVE_PNG())PngWriter.write(buffered,width,height,rows);
                else {
                  Bitmap encoded = image;
                  try {
                    if (request == host.readSAVE_JPEG()) {
                      encoded =
                          Bitmap.createBitmap(
                              image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
                      android.graphics.Canvas canvas = new android.graphics.Canvas(encoded);
                      canvas.drawColor(Color.WHITE);
                      canvas.drawBitmap(image, 0, 0, null);
                    }
                    Bitmap.CompressFormat format =
                        request == host.readSAVE_JPEG()
                            ? Bitmap.CompressFormat.JPEG
                            : request == host.readSAVE_WEBP()
                                ? (android.os.Build.VERSION.SDK_INT >= 30
                                    ? Bitmap.CompressFormat.WEBP_LOSSLESS
                                    : Bitmap.CompressFormat.WEBP)
                                : Bitmap.CompressFormat.PNG;
                    if (!encoded.compress(format, 100, buffered))
                      throw new java.io.IOException("No se pudo exportar la imagen");
                  } finally {
                    if (encoded != image) encoded.recycle();
                  }
                }
                buffered.flush();
                success = true;
              } catch (Exception | OutOfMemoryError e) {
                success = false;
              } finally {
                if(snapshot[0]!=null)snapshot[0].recycle();
              }
              final boolean ok = success;
              host.runOnUiThread(
                  () -> {
                    if (!host.isDestroyed()) {
                      progress.dismiss();
                      host.writeProjectProgress(null);
                      host.readDrawing().setEnabled(true);
                      host.message(ok ? "Imagen guardada" : "Error al guardar la imagen");
                    }
                  });
            },
            "velyntora-raster-export")
        .start();
  }

  void openTga() {
    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    intent.setType("*/*");
    host.startActivityForResult(intent, host.readOPEN_TGA());
  }

  void openTiff() {
    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    intent.setType("*/*");
    host.startActivityForResult(intent, host.readOPEN_TIFF());
  }

  void openIco() {
    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    intent.setType("*/*");
    host.startActivityForResult(intent, host.readOPEN_ICO());
  }

  void openPpm() {
    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    intent.setType("*/*");
    host.startActivityForResult(intent, host.readOPEN_PPM());
  }

  void importTga(Uri uri) {
    host.importRaster(uri, host.readOPEN_TGA());
  }

  void importRaster(Uri uri, int request) {
    if (host.readProjectProgress() != null) {
      host.message("Espera a que termine la operación actual");
      return;
    }
    final String format =
        request == host.readOPEN_TIFF()
            ? "TIFF"
            : request == host.readOPEN_ICO()
                ? "ICO"
                : request == host.readOPEN_PPM()
                    ? "Netpbm"
                    : request == host.readOPEN_BMP()
                        ? "BMP"
                        : request == host.readOPEN_PCX()
                            ? "PCX"
                            : request == host.readOPEN_XBM()
                                ? "XBM"
                                : request == host.readOPEN_XPM()
                                    ? "XPM"
                                    : request == host.readOPEN_SVG()
                                        ? "SVG"
                                        : request == host.readOPEN_CONTAINER() ? "ANI/ICNS/QuickTime" : "TGA";
    android.app.ProgressDialog progress = new android.app.ProgressDialog(host);
    progress.setMessage("Abriendo " + format + "…");
    progress.setCancelable(false);
    host.writeProjectProgress(progress);
    progress.show();
    host.readDrawing().setEnabled(false);
    new Thread(
            () -> {
              host.fileName(uri);
              int[] pixels = null;
              int width = 0, height = 0;
              try (InputStream input = host.getContentResolver().openInputStream(uri)) {
                if (input == null) throw new java.io.IOException("Sin archivo");
                if (request == host.readOPEN_PCX()) {
                  PcxReader.Image image = PcxReader.read(input);
                  pixels = image.pixels;
                  width = image.width;
                  height = image.height;
                } else if (request == host.readOPEN_XBM() || request == host.readOPEN_XPM()) {
                  XRasterReader.Image image =
                      request == host.readOPEN_XBM() ? XRasterReader.xbm(input) : XRasterReader.xpm(input);
                  pixels = image.pixels;
                  width = image.width;
                  height = image.height;
                } else if (request == host.readOPEN_SVG()) {
                  Bitmap bitmap = SvgRaster.read(input);
                  try {
                    width = bitmap.getWidth();
                    height = bitmap.getHeight();
                    pixels = new int[width * height];
                    bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
                  } finally {
                    bitmap.recycle();
                  }
                } else if (request == host.readOPEN_CONTAINER()) {
                  IconContainers.Image image =
                      IconContainers.read(input, host::decodeContainerImage);
                  pixels = image.pixels;
                  width = image.width;
                  height = image.height;
                } else if (request == host.readOPEN_BMP()) {
                  BmpReader.Image image = BmpReader.read(new java.io.BufferedInputStream(input));
                  pixels = image.pixels;
                  width = image.width;
                  height = image.height;
                } else if (request == host.readOPEN_TIFF()) {
                  TiffReader.Image image = TiffReader.read(new java.io.BufferedInputStream(input));
                  pixels = image.pixels;
                  width = image.width;
                  height = image.height;
                } else if (request == host.readOPEN_PPM()) {
                  PpmCodec.Image image = PpmCodec.read(new java.io.BufferedInputStream(input));
                  pixels = image.pixels;
                  width = image.width;
                  height = image.height;
                } else if (request == host.readOPEN_ICO()) {
                  IcoCodec.Image image =
                      IcoCodec.read(
                          new java.io.BufferedInputStream(input),
                          png -> {
                            Bitmap bitmap = BitmapFactory.decodeByteArray(png, 0, png.length);
                            if (bitmap == null) throw new java.io.IOException("PNG ICO inválido");
                            try {
                              int w = bitmap.getWidth(), h = bitmap.getHeight();
                              if (w > 256 || h > 256)
                                throw new java.io.IOException("ICO demasiado grande");
                              int[] p = new int[w * h];
                              bitmap.getPixels(p, 0, w, 0, 0, w, h);
                              return new IcoCodec.Image(w, h, p);
                            } finally {
                              bitmap.recycle();
                            }
                          });
                  pixels = image.pixels;
                  width = image.width;
                  height = image.height;
                } else {
                  TgaReader.Image image = TgaReader.read(new java.io.BufferedInputStream(input));
                  pixels = image.pixels;
                  width = image.width;
                  height = image.height;
                }
              } catch (Exception e) {
                pixels = null;
              }
              final int[] decoded = pixels;
              final int w = width, h = height;
              host.runOnUiThread(
                  () -> {
                    if (!host.isDestroyed()) {
                      progress.dismiss();
                      host.writeProjectProgress(null);
                      host.readDrawing().setEnabled(true);
                      if (decoded == null) {
                        host.message(format + " incompatible, incompleto o demasiado grande");
                        return;
                      }
                      Bitmap bitmap = Bitmap.createBitmap(decoded, w, h, Bitmap.Config.ARGB_8888);
                      try {
                        if (!host.readDrawing().loadBitmap(bitmap)) host.message("No se pudo abrir el " + format);
                        else host.markDocumentClean(host.fileName(uri));
                      } finally {
                        bitmap.recycle();
                      }
                    }
                  });
            },
            "velyntora-raster-import")
        .start();
  }

  void savePng() {
    host.saveImage("image/png", "dibujo.png", host.readSAVE_PNG());
  }

  void importImage(Uri uri) {
    if (host.readProjectProgress() != null) {
      host.message("Espera a que termine la operación actual");
      return;
    }
    android.app.ProgressDialog checking=new android.app.ProgressDialog(host);
    checking.setMessage("Comprobando tamaño de imagen…");checking.setCancelable(false);
    host.writeProjectProgress(checking);checking.show();host.readDrawing().setEnabled(false);
    new Thread(()->{
      ImageImportSize size=null;
      try{size=RegionImageImporter.probe(host,uri);}catch(Exception error){}
      final ImageImportSize plan=size;
      host.runOnUiThread(()->{
        checking.dismiss();host.writeProjectProgress(null);host.readDrawing().setEnabled(true);
        if(host.isDestroyed())return;
        if(plan==null){host.message("No se pudo leer el tamaño de la imagen");return;}
        if(plan.reduced()) {
          importConfirmation=new AlertDialog.Builder(host).setTitle("La imagen supera el límite")
              .setMessage(plan.confirmation()).setNegativeButton("Cancelar",(dialog,which)->{})
              .setPositiveButton("Reducir e importar",(dialog,which)->importImage(uri,plan)).create();
          importConfirmation.setOnDismissListener(dialog->importConfirmation=null);
          importConfirmation.show();
        }else importImage(uri,plan);
      });
    },"velyntora-image-size").start();
  }
  private AlertDialog importConfirmation;
  AlertDialog importConfirmation(){return importConfirmation;}
  private void importImage(Uri uri,ImageImportSize size) {
    if(host.readProjectProgress()!=null){host.message("Espera a que termine la operación actual");return;}
    android.app.ProgressDialog progress = new android.app.ProgressDialog(host);
    progress.setMessage("Abriendo imagen…");
    progress.setCancelable(false);
    host.writeProjectProgress(progress);
    progress.show();
    host.readDrawing().setEnabled(false);
    final DrawingView drawing = host.readDrawing();
    final int viewWidth = drawing.getWidth(), viewHeight = drawing.getHeight();
    new Thread(() -> {
      String name = host.fileName(uri);
      CanvasRendering.Frame prepared = null;
      try {
        RegionImageImporter.load(host,uri,size);
        int width=DrawingView.nativeWidth(),height=DrawingView.nativeHeight();
        float scale=Math.min(viewWidth/(float)width,viewHeight/(float)height)*.9f;
        prepared=CanvasRendering.prepareFrame(scale);
        DrawingView.nativeAcceptImageImport();
      } catch (Exception | OutOfMemoryError error) {
        if(prepared!=null)prepared.dispose();prepared=null;
        DrawingView.nativeCancelImageImport();
      }
      final CanvasRendering.Frame frame = prepared;
      host.runOnUiThread(() -> {
        try {
          if (host.isDestroyed()) {
            if (frame != null) frame.dispose();
            return;
          }
          if (frame == null) host.message("No se pudo abrir la imagen");
          else {
            drawing.importedFrameReady(frame);
            host.markDocumentClean(name);
          }
        } finally {
          progress.dismiss();
          host.writeProjectProgress(null);
          drawing.setEnabled(true);
          if (!host.isDestroyed()) host.refreshLayerPanel();
        }
      });
    }, "velyntora-image-import").start();
  }

  String fileName(Uri uri) {
    String cached = host.readFileNames().get(uri.toString());
    if (cached != null) return cached;
    if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper())
      return uri.getLastPathSegment();
    try (android.database.Cursor cursor =
        host.getContentResolver()
            .query(
                uri,
                new String[] {android.provider.OpenableColumns.DISPLAY_NAME},
                null,
                null,
                null)) {
      if (cursor != null && cursor.moveToFirst()) {
        String name = cursor.getString(0);
        if (name != null) {
          host.readFileNames().put(uri.toString(), name);
          return name;
        }
      }
    } catch (Exception ignored) {
    }
    return uri.getLastPathSegment();
  }

  IconContainers.Image decodeContainerImage(byte[] encoded) throws java.io.IOException {
    if (encoded.length >= 4
        && encoded[0] == 0
        && encoded[1] == 0
        && (encoded[2] == 1 || encoded[2] == 2)
        && encoded[3] == 0) {
      byte[] ico = encoded.clone();
      ico[2] = 1; // CUR embeds the same image data; hotspot is irrelevant to a drawing.
      IcoCodec.Image image =
          IcoCodec.read(
              new java.io.ByteArrayInputStream(ico),
              png -> {
                IconContainers.Image decoded = host.decodeContainerImage(png);
                return new IcoCodec.Image(decoded.width, decoded.height, decoded.pixels);
              });
      return new IconContainers.Image(image.width, image.height, image.pixels);
    }
    BitmapFactory.Options bounds = new BitmapFactory.Options();
    bounds.inJustDecodeBounds = true;
    BitmapFactory.decodeByteArray(encoded, 0, encoded.length, bounds);
    if (bounds.outWidth < 1 || bounds.outHeight < 1)
      throw new java.io.IOException("Imagen del contenedor incompatible");
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inSampleSize = ImageDecodePolicy.sampleSize(bounds.outWidth, bounds.outHeight);
    options.inPreferredConfig = Bitmap.Config.ARGB_8888;
    options.inMutable = true;
    Bitmap bitmap = BitmapFactory.decodeByteArray(encoded, 0, encoded.length, options);
    if (bitmap == null) throw new java.io.IOException("Imagen del contenedor inválida");
    try {
      int w = bitmap.getWidth(), h = bitmap.getHeight();
      if (w > CanvasLimits.MAX_SIDE || h > CanvasLimits.MAX_SIDE || (long) w * h > CanvasLimits.MAX_PIXELS)
        throw new java.io.IOException("Imagen demasiado grande");
      int[] pixels = new int[w * h];
      bitmap.getPixels(pixels, 0, w, 0, 0, w, h);
      return new IconContainers.Image(w, h, pixels);
    } finally {
      bitmap.recycle();
    }
  }

  void openDetected(Uri uri) {
    // Providers may perform network IO: never inspect their stream on the UI thread.
    if (host.readProjectProgress() != null) return;
    android.app.ProgressDialog progress = new android.app.ProgressDialog(host);
    progress.setMessage("Identificando archivo…");
    progress.setCancelable(false);
    host.writeProjectProgress(progress);
    progress.show();
    host.readDrawing().setEnabled(false);
    new Thread(
            () -> {
              int kind = RasterFormat.ANDROID;
              boolean ok = false;
              try (InputStream input = host.getContentResolver().openInputStream(uri)) {
                if (input == null) throw new java.io.IOException("Sin archivo");
                byte[] prefix = new byte[16];
                int count = 0;
                while (count < prefix.length) {
                  int value = input.read();
                  if (value < 0) break;
                  prefix[count++] = (byte) value;
                }
                kind = RasterFormat.detect(java.util.Arrays.copyOf(prefix, count), host.fileName(uri));
                ok = true;
              } catch (Exception ignored) {
              }
              final int format = kind;
              final boolean readable = ok;
              host.runOnUiThread(
                  () -> {
                    if (host.isDestroyed()) return;
                    progress.dismiss();
                    host.writeProjectProgress(null);
                    host.readDrawing().setEnabled(true);
                    if (!readable) {
                      host.message("No se pudo leer el archivo");
                      return;
                    }
                    switch (format) {
                      case RasterFormat.PCX:
                        host.importRaster(uri, host.readOPEN_PCX());
                        break;
                      case RasterFormat.XBM:
                        host.importRaster(uri, host.readOPEN_XBM());
                        break;
                      case RasterFormat.XPM:
                        host.importRaster(uri, host.readOPEN_XPM());
                        break;
                      case RasterFormat.SVG:
                        host.importRaster(uri, host.readOPEN_SVG());
                        break;
                      case RasterFormat.CONTAINER:
                        host.importRaster(uri, host.readOPEN_CONTAINER());
                        break;
                      case RasterFormat.BMP:
                        host.importRaster(uri, host.readOPEN_BMP());
                        break;
                      case RasterFormat.TIFF:
                        host.importRaster(uri, host.readOPEN_TIFF());
                        break;
                      case RasterFormat.ICO:
                        host.importRaster(uri, host.readOPEN_ICO());
                        break;
                      case RasterFormat.NETPBM:
                        host.importRaster(uri, host.readOPEN_PPM());
                        break;
                      case RasterFormat.TGA:
                        host.importRaster(uri, host.readOPEN_TGA());
                        break;
                      case RasterFormat.PROJECT:
                        host.transferProject(uri, false);
                        break;
                      case RasterFormat.ORA:
                        host.transferOpenRaster(uri, false);
                        break;
                      default:
                        host.importImage(uri);
                    }
                  });
            },
            "velyntora-format-detection")
        .start();
  }

  void openImage() {
    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    intent.setType("*/*");
    host.startActivityForResult(intent, host.readOPEN_IMAGE());
  }

  void acceptFileResult(int request, Uri uri) {
    if (request == host.readSAVE_ORA() || request == host.readOPEN_ORA()) {
      host.transferOpenRaster(uri, request == host.readSAVE_ORA());
    } else if (request == host.readSAVE_PROJECT() || request == host.readOPEN_PROJECT()) {
      host.transferProject(uri, request == host.readSAVE_PROJECT());
    } else if (request == host.readOPEN_TIFF()) {
      host.importRaster(uri, host.readOPEN_TIFF());
    } else if (request == host.readOPEN_PPM()) {
      host.importRaster(uri, host.readOPEN_PPM());
    } else if (request == host.readOPEN_ICO()) {
      host.importRaster(uri, host.readOPEN_ICO());
    } else if (request == host.readOPEN_TGA()) {
      host.importTga(uri);
    } else if (request == host.readOPEN_IMAGE()) {
      host.openDetected(uri);
    } else if (request == host.readSAVE_BMP()
        || request == host.readSAVE_TGA()
        || request == host.readSAVE_TIFF()
        || request == host.readSAVE_GIF()
        || request == host.readSAVE_ICO()
        || request == host.readSAVE_PPM()
        || request == host.readSAVE_PNG()
        || request == host.readSAVE_JPEG()
        || request == host.readSAVE_WEBP()) {
      host.exportRaster(uri, request);
    }
  }
}
