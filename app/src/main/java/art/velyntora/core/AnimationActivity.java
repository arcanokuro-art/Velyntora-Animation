package art.velyntora.core;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import app.velyntora.animation.playback.PlaybackController;
import app.velyntora.animation.preview.FramePreviewCache;
import app.velyntora.animation.setup.AnimationSetupDialog;
import app.velyntora.animation.setup.AnimationSetupDialog.Settings;
import app.velyntora.animation.storage.AnimationProject;
import app.velyntora.animation.timeline.TimelinePanel;
import java.io.*;
import java.util.concurrent.*;

/** Composition facade. Native tools stay in Core; animation state stays in its own modules. */
public final class AnimationActivity extends MainActivity implements TimelinePanel.Actions {
  public static final String ACTION = "app.velyntora.animation.TIMELINE";
  static final int OPEN = 8101, SAVE = 8102, EXPORT = 8103;
  private static final ExecutorService IO = Executors.newSingleThreadExecutor();
  private final FramePreviewCache cache = new FramePreviewCache();
  private final PlaybackController playback = new PlaybackController(cache);
  private final OnionSkin onion = new OnionSkin();
  private AnimationSession session;
  private FrameDocumentAdapter adapter;
  private TimelinePanel timeline;
  private ImageView preview;
  private TextView progress;
  private boolean busy, ready, onionEnabled, closeAfterJob;
  private int onionGeneration;
  private Runnable pendingEntry;
  private String stableSessionPath;
  private final Handler autosave = new Handler(Looper.getMainLooper());
  private final Runnable saveWhenIdle =
      new Runnable() {
        public void run() {
          if (isDestroyed() || !ready) return;
          if (busy || readProjectProgress() != null || !readDrawing().isEnabled()) {
            autosave.postDelayed(this, 1000);
            return;
          }
          if (session.hasChanges()) runIO("Guardando sesión…", session::capture, null);
        }
      };

  interface Job {
    void run() throws Exception;
  }

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    adapter = new FrameDocumentAdapter(readDrawing());
    session = new AnimationSession(adapter, new File(getFilesDir(), "animations"));
    LinearLayout root =
        (LinearLayout) ((ViewGroup) findViewById(android.R.id.content)).getChildAt(0);
    progress = new TextView(this);
    progress.setTextColor(Color.WHITE);
    progress.setBackgroundColor(0xff174e8e);
    progress.setPadding(dp(8), dp(4), dp(8), dp(4));
    progress.setVisibility(View.GONE);
    root.addView(progress, 0, new LinearLayout.LayoutParams(-1, -2));
    timeline = new TimelinePanel(this, cache, this);
    root.addView(timeline, new LinearLayout.LayoutParams(-1, -2));
    preview = new ImageView(this);
    preview.setBackgroundColor(0xff2b2b2b);
    preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
    preview.setVisibility(View.GONE);
    attachPreview();
    readDrawing().setAnimationOverlay(onion);
    if (state != null && state.getString("session") != null) {
      final String folder = state.getString("session");
      runIO("Abriendo animación…", () -> session.resume(folder), null);
    } else enter(getIntent());
  }

  private void enter(Intent intent) {
    String folder = intent.getStringExtra("session");
    Uri uri = intent.getData();
    if (folder != null) runIO("Abriendo animación…", () -> session.resume(folder), null);
    else if (uri != null)
      runIO(
          "Abriendo proyecto…",
          () -> {
            try (InputStream in = getContentResolver().openInputStream(uri)) {
              if (in == null) throw new IOException("No se puede leer el proyecto");
              session.open(in);
            }
          },
          null);
    else {
      Settings settings =
          new Settings(
              intent.getStringExtra("title") == null
                  ? "Sin título"
                  : intent.getStringExtra("title"),
              intent.getIntExtra("width", 1280),
              intent.getIntExtra("height", 720),
              intent.getIntExtra("fps", 12));
      runIO("Creando animación…", () -> session.create(settings), null);
    }
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);
    Runnable entry = () -> enter(intent);
    if (busy) pendingEntry = entry;
    else entry.run();
  }

  @Override
  protected boolean usesCoreRecovery() {
    return false;
  }

  private void attachPreview() {
    if (preview.getParent() instanceof ViewGroup)
      ((ViewGroup) preview.getParent()).removeView(preview);
    readWorkspaceFrame().addView(preview, new android.widget.FrameLayout.LayoutParams(-1, -1));
  }

  @Override
  public void onConfigurationChanged(android.content.res.Configuration config) {
    stopPlayback();
    super.onConfigurationChanged(config);
    attachPreview();
    if (!busy) refreshAnimation();
  }

  private void runIO(String label, Job task, Runnable success) {
    if (busy) {
      message("Espera a que termine la operación actual");
      return;
    }
    if (readProjectProgress() != null || !readDrawing().isEnabled()) {
      message("Espera a que termine la operación del editor");
      return;
    }
    if (readDrawing().placedImage().active()) {
      message("Confirma o cancela la imagen insertada antes de cambiar de fotograma");
      return;
    }
    stopPlayback();
    readDrawing().confirmCurve();
    busy = true;
    progress.setText(label);
    progress.setVisibility(View.VISIBLE);
    IO.execute(
        () -> {
          Throwable failure = null;
          try {
            task.run();
          } catch (Exception | OutOfMemoryError e) {
            failure = e;
          }
          final Throwable error = failure;
          runOnUiThread(
              () -> {
                if (isDestroyed()) return;
                busy = false;
                progress.setVisibility(View.GONE);
                if (session.changedDocument) {
                  adapter.present();
                  session.changedDocument = false;
                }
                ready = session.project() != null;
                if (ready) {
                  stableSessionPath = session.project().directory.getAbsolutePath();
                  markDocumentClean(
                      session.project().title
                          + " · "
                          + (session.project().sequence.selected() + 1));
                  refreshAnimation();
                }
                if (error != null) {
                  closeAfterJob = false;
                  new AlertDialog.Builder(this)
                      .setTitle("No se pudo completar")
                      .setMessage(
                          error.getMessage() == null
                              ? "Memoria insuficiente o archivo incompatible"
                              : error.getMessage())
                      .setPositiveButton(
                          "Aceptar",
                          (d, w) -> {
                            if (!ready) finish();
                          })
                      .show();
                } else {
                  if (success != null) success.run();
                  if (closeAfterJob) finish();
                }
                if (pendingEntry != null && !isFinishing()) {
                  Runnable next = pendingEntry;
                  pendingEntry = null;
                  next.run();
                }
              });
        });
  }

  private void refreshAnimation() {
    if (timeline == null || session == null) return;
    AnimationProject project = session.project();
    timeline.bind(project, playback.running(), onionEnabled);
    int generation = ++onionGeneration;
    onion.set(null, null);
    readDrawing().invalidate();
    if (!ready || !onionEnabled || playback.running()) return;
    int selected = project.sequence.selected();
    final Bitmap[] neighbors = new Bitmap[2];
    for (int offset : new int[] {-1, 1}) {
      int i = selected + offset, slot = offset < 0 ? 0 : 1;
      if (i < 0 || i >= project.sequence.size()) continue;
      String asset = project.sequence.frame(i).asset;
      cache.load(
          asset,
          project.previewFile(asset),
          1024,
          image -> {
            if (generation != onionGeneration || isDestroyed()) return;
            neighbors[slot] = image;
            onion.set(neighbors[0], neighbors[1]);
            readDrawing().invalidate();
          });
    }
  }

  private void stopPlayback() {
    playback.stop();
    if (preview != null) {
      preview.setVisibility(View.GONE);
      preview.setImageDrawable(null);
    }
  }

  @Override
  public boolean dispatchTouchEvent(MotionEvent event) {
    if (busy) return true;
    if (playback.running()) {
      Rect rect = new Rect();
      timeline.getGlobalVisibleRect(rect);
      if (!rect.contains((int) event.getRawX(), (int) event.getRawY())) return true;
    }
    if (event.getActionMasked() == MotionEvent.ACTION_DOWN) autosave.removeCallbacks(saveWhenIdle);
    boolean handled = super.dispatchTouchEvent(event);
    if (event.getActionMasked() == MotionEvent.ACTION_UP) autosave.postDelayed(saveWhenIdle, 2000);
    return handled;
  }

  @Override
  public boolean onKeyDown(int key, KeyEvent event) {
    if (busy) return true;
    if (playback.running()) {
      if (key == KeyEvent.KEYCODE_SPACE) {
        stopPlayback();
        refreshAnimation();
      }
      return true;
    }
    return super.onKeyDown(key, event);
  }

  public void select(int index) {
    if (!ready) return;
    if (index == session.project().sequence.selected()) {
      stopPlayback();
      refreshAnimation();
      return;
    }
    runIO("Cambiando fotograma…", () -> session.mutate(s -> s.select(index), false, true), null);
  }

  public void add(boolean duplicate) {
    if (ready)
      runIO(
          duplicate ? "Duplicando fotograma…" : "Creando fotograma…",
          () -> session.mutate(s -> s.insert(duplicate), !duplicate, duplicate),
          null);
  }

  public void delete() {
    if (!ready) return;
    stopPlayback();
    if (session.project().sequence.size() == 1) {
      message("Debe quedar al menos un fotograma");
      return;
    }
    new AlertDialog.Builder(this)
        .setTitle("Eliminar fotograma")
        .setMessage("¿Eliminar el fotograma actual y sus capas?")
        .setPositiveButton(
            "Eliminar",
            (d, w) ->
                runIO(
                    "Eliminando fotograma…",
                    () -> session.mutate(s -> s.delete(), false, true),
                    null))
        .setNegativeButton("Cancelar", null)
        .show();
  }

  public void move(int delta) {
    if (ready)
      runIO(
          "Reordenando fotogramas…", () -> session.mutate(s -> s.move(delta), false, false), null);
  }

  public void playback() {
    if (playback.running()) {
      stopPlayback();
      refreshAnimation();
      return;
    }
    if (!ready) return;
    runIO(
        "Preparando reproducción…",
        session::capture,
        () -> {
          onionGeneration++;
          onion.set(null, null);
          preview.setImageDrawable(null);
          preview.setVisibility(View.VISIBLE);
          preview.bringToFront();
          playback.start(
              session.project(),
              (bitmap, index) -> {
                if (!isDestroyed()) {
                  preview.setImageBitmap(bitmap);
                  timeline.setPlaybackFrame(index);
                }
              });
          timeline.bind(session.project(), true, onionEnabled);
        });
  }

  public void onion() {
    stopPlayback();
    onionEnabled = !onionEnabled;
    refreshAnimation();
  }

  public void setFps(int value) {
    stopPlayback();
    runIO("Guardando velocidad…", () -> session.mutate(s -> s.fps(value), false, false), null);
  }

  public void fps() {
    slider(
        "Fotogramas por segundo",
        session.project().sequence.fps(),
        60,
        value ->
            runIO(
                "Guardando velocidad…",
                () -> session.mutate(s -> s.fps(value), false, false),
                null));
  }

  public void exposure() {
    slider(
        "Duración en fotogramas",
        session.project().sequence.current().exposure,
        120,
        value ->
            runIO(
                "Guardando duración…",
                () -> session.mutate(s -> s.exposure(value), false, false),
                null));
  }

  private void slider(
      String title, int initial, int maximum, java.util.function.IntConsumer apply) {
    stopPlayback();
    LinearLayout form = new LinearLayout(this);
    form.setOrientation(LinearLayout.VERTICAL);
    TextView value = new TextView(this);
    value.setText("" + initial);
    form.addView(value);
    SeekBar bar = new SeekBar(this);
    bar.setMax(maximum - 1);
    bar.setProgress(initial - 1);
    form.addView(bar);
    bar.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {
          public void onProgressChanged(SeekBar b, int n, boolean user) {
            value.setText("" + (n + 1));
          }

          public void onStartTrackingTouch(SeekBar b) {}

          public void onStopTrackingTouch(SeekBar b) {}
        });
    new AlertDialog.Builder(this)
        .setTitle(title)
        .setView(form)
        .setPositiveButton("Aplicar", (d, w) -> apply.accept(bar.getProgress() + 1))
        .setNegativeButton("Cancelar", null)
        .show();
  }

  public void save() {
    chooseDestination(SAVE, "animacion.vlyanim");
  }

  public void export() {
    chooseDestination(EXPORT, "fotogramas.zip");
  }

  private void chooseDestination(int request, String name) {
    if (!ready) return;
    stopPlayback();
    startActivityForResult(
        new Intent(Intent.ACTION_CREATE_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("application/zip")
            .putExtra(Intent.EXTRA_TITLE, name),
        request);
  }

  @Override
  void projectPicker(boolean save) {
    if (save) save();
    else openImage();
  }

  @Override
  void openImage() {
    stopPlayback();
    startActivityForResult(
        new Intent(Intent.ACTION_OPEN_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("*/*"),
        OPEN);
  }

  @Override
  void configureDimensions(int mode) {
    if (mode != 0) {
      super.configureDimensions(mode);
      return;
    }
    stopPlayback();
    AnimationSetupDialog.show(
        this,
        ready ? session.project().sequence.fps() : 12,
        settings -> runIO("Creando animación…", () -> session.create(settings), null));
  }

  @Override
  void menu(LinearLayout parent, String title, String[] labels, Runnable[] actions) {
    if (!"Archivo".equals(title)) {
      super.menu(parent, title, labels, actions);
      return;
    }
    super.menu(
        parent,
        title,
        new String[] {
          "Nueva animación…",
          "Abrir animación…",
          "Guardar animación…",
          "Exportar secuencia PNG…",
          "Insertar imagen como capa…",
          "Exportar fotograma PNG…"
        },
        new Runnable[] {
          () -> configureDimensions(0),
          this::openImage,
          this::save,
          this::export,
          () -> readDrawing().placedImage().pick(),
          super::savePng
        });
  }

  @Override
  protected void onActivityResult(int request, int result, Intent data) {
    if (request < OPEN || request > EXPORT) {
      super.onActivityResult(request, result, data);
      return;
    }
    if (result != RESULT_OK || data == null || data.getData() == null) return;
    if (busy) {
      pendingEntry = () -> onActivityResult(request, result, data);
      return;
    }
    Uri uri = data.getData();
    if (request == OPEN)
      runIO(
          "Abriendo animación…",
          () -> {
            try (InputStream in = getContentResolver().openInputStream(uri)) {
              if (in == null) throw new IOException("No se pudo leer el archivo");
              session.open(in);
            }
          },
          null);
    else
      runIO(
          request == SAVE ? "Guardando animación…" : "Exportando secuencia PNG…",
          () -> {
            try (OutputStream out = getContentResolver().openOutputStream(uri, "wt")) {
              if (out == null) throw new IOException("No se pudo escribir el archivo");
              if (request == SAVE) session.exportArchive(out);
              else session.exportPng(out);
            }
          },
          () -> message("Archivo guardado"));
  }

  @Override
  protected void onSaveInstanceState(Bundle state) {
    if (stableSessionPath != null) state.putString("session", stableSessionPath);
    super.onSaveInstanceState(state);
  }

  @Override
  protected void onPause() {
    stopPlayback();
    if (ready && !isFinishing()) {
      autosave.removeCallbacks(saveWhenIdle);
      autosave.post(saveWhenIdle);
    }
    super.onPause();
  }

  @Override
  public void onBackPressed() {
    if (!busy && readDrawing().placedImage().active()) {
      readDrawing().placedImage().cancel();
      return;
    }
    if (busy) {
      closeAfterJob = true;
      return;
    }
    if (!ready) {
      finish();
      return;
    }
    runIO("Guardando sesión…", session::capture, this::finish);
  }

  @Override
  protected void onDestroy() {
    autosave.removeCallbacks(saveWhenIdle);
    stopPlayback();
    cache.close();
    super.onDestroy();
  }
}
