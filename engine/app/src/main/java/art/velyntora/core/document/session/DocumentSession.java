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


/** Owns document/session behavior; the host is the workspace composition facade. */
final class DocumentSession {
  private final MainActivity host;
  DocumentSession(MainActivity host) { this.host = host; }

  boolean dirty() {
    return host.readDrawing().hasPendingCurve() || host.readDrawing().revision() != host.readSavedRevision();
  }

  void markDocumentClean(String name) {
    host.writeSavedRevision(host.readDrawing().revision());
    host.writeDocumentName(name == null ? "Sin título" : name);
    host.updateStatus();
  }

  void protectDocument(Runnable action) {
    if (host.readProjectProgress() != null) {
      host.message("Espera a que termine la operación actual");
      return;
    }
    if (!host.dirty()) {
      action.run();
      return;
    }
    new AlertDialog.Builder(host)
        .setTitle("Cambios sin guardar")
        .setMessage(
            "Guarda un proyecto VLYCORE u OpenRaster para conservar las capas antes de sustituir"
                + " este dibujo.")
        .setNegativeButton("Cancelar", null)
        .setNeutralButton("Guardar proyecto", (d, w) -> host.projectPicker(true))
        .setPositiveButton("Descartar cambios", (d, w) -> action.run())
        .show();
  }

  void recoverDocument() {
    android.util.AtomicFile recovery =
        new android.util.AtomicFile(new java.io.File(host.getFilesDir(), "document-recovery.vlycore"));
    if (!recovery.getBaseFile().exists()
        && !new java.io.File(recovery.getBaseFile() + ".bak").exists()) return;
    android.app.ProgressDialog progress = new android.app.ProgressDialog(host);
    progress.setMessage("Recuperando documento…");
    progress.setCancelable(false);
    host.writeProjectProgress(progress);
    progress.show();
    host.readDrawing().setEnabled(false);
    host.readRecoveryExecutor().execute(
        () -> {
          boolean ok = false;
          try (java.io.FileInputStream input = recovery.openRead();
              android.os.ParcelFileDescriptor descriptor =
                  android.os.ParcelFileDescriptor.dup(input.getFD())) {
            ok = host.readDrawing().readProject(descriptor.getFd());
          } catch (Exception ignored) {
          }
          final boolean restored = ok;
          host.runOnUiThread(
              () -> {
                if (host.isDestroyed()) return;
                progress.dismiss();
                host.writeProjectProgress(null);
                host.readDrawing().setEnabled(true);
                if (restored) {
                  host.readDrawing().projectOpened();
                  host.writeDocumentName(host.getSharedPreferences("recovery", android.content.Context.MODE_PRIVATE)
                          .getString("name", "Recuperado"));
                  host.writeSavedRevision(-1);
                  host.updateStatus();
                  host.message("Documento recuperado");
                } else host.message("No se pudo recuperar el documento; el archivo se conserva");
              });
        });
  }

  void saveRecovery() {

    
    if (host.readDrawing() == null || host.readProjectProgress() != null) return;
    host.readDrawing().confirmCurve();
    final long expectedRevision = host.readDrawing().revision();
    final String name = host.readDocumentName();
    final android.util.AtomicFile recovery =
        new android.util.AtomicFile(new java.io.File(host.getFilesDir(), "document-recovery.vlycore"));
    host.readRecoveryExecutor().execute(
        () -> {
          java.io.FileOutputStream stream = null;
          try {
            stream = recovery.startWrite();
            boolean ok;
            try (android.os.ParcelFileDescriptor descriptor =
                android.os.ParcelFileDescriptor.dup(stream.getFD())) {
              ok = host.readDrawing().writeRecovery(descriptor.getFd(), expectedRevision);
            }
            if (!ok) throw new java.io.IOException("Recuperación incompleta");
            recovery.finishWrite(stream);
            stream = null;
            host.getSharedPreferences("recovery", android.content.Context.MODE_PRIVATE).edit().putString("name", name).commit();
          } catch (Exception ignored) {
            if (stream != null) recovery.failWrite(stream);
          }
        });
  
  }
}
