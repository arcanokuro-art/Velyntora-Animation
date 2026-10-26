package art.velyntora.core;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import app.velyntora.animation.frames.FrameSequence;
import app.velyntora.animation.playback.PlaybackClock;
import app.velyntora.animation.project.AnimationProject;
import java.io.*;
import java.nio.file.*;
import java.util.UUID;

/** Animation composes the complete shared editor; frame documents preserve all native layers. */
public final class AnimationActivity extends MainActivity {
  private AnimationProject project;
  private LinearLayout timeline, bar;
  private ImageView preview;

  private final Handler clock = new Handler(Looper.getMainLooper());
  private final OnionSkin onion = new OnionSkin();
  private boolean playing, onionEnabled, ready;
  private PlaybackClock playback;
  private Bitmap playbackBitmap;
  private int shown = -1;
  private static final int SAVE_ANIMATION = 7101, EXPORT_PNG = 7102;
  private final Runnable tick =
      new Runnable() {
        public void run() {
          if (!playing) return;
          int index = playback.frame(SystemClock.uptimeMillis());
          if (index != shown) {
            shown = index;
            Bitmap image = decode(project.preview(project.sequence.id(index)), 1280);
            preview.setImageBitmap(image);
            if (playbackBitmap != null) playbackBitmap.recycle();
            playbackBitmap = image;
          }
          clock.postDelayed(this, 16);
        }
      };

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    LinearLayout root =
        (LinearLayout) ((ViewGroup) findViewById(android.R.id.content)).getChildAt(0);
    preview = new ImageView(this);
    preview.setBackgroundColor(0xff303030);
    preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
    preview.setVisibility(View.GONE);
    timeline = new LinearLayout(this);
    HorizontalScrollView strip = new HorizontalScrollView(this);
    strip.addView(timeline);
    root.addView(strip, new LinearLayout.LayoutParams(-1, dp(66)));
    bar = new LinearLayout(this);
    HorizontalScrollView buttons = new HorizontalScrollView(this);
    buttons.addView(bar);
    root.addView(buttons, new LinearLayout.LayoutParams(-1, dp(48)));
    // Preview covers only the editable workspace, leaving playback controls accessible.
    readWorkspaceFrame().addView(preview, new FrameLayout.LayoutParams(-1, -1));
    readDrawing().setAnimationOverlay(onion);
    try {
      String path =
          state == null ? getIntent().getStringExtra("session") : state.getString("session");
      project =
          new AnimationProject(
              path == null
                  ? new File(getFilesDir(), "animation/" + UUID.randomUUID())
                  : new File(path));
      if (path != null) {
        project.readMetadata();
        loadCurrent();
      } else if (getIntent().getData() != null) {
        try (InputStream in = getContentResolver().openInputStream(getIntent().getData())) {
          if (in == null) throw new IOException("No se puede abrir");
          project.importArchive(in);
        }
        loadCurrent();
      } else {
        sequence().fps(getSharedPreferences("animation", 0).getInt("fps", 12));
        if (!readDrawing().newDocument(1280, 720))
          throw new IOException("No se puede crear el lienzo");
        capture();
      }
      getPreferences(0)
          .edit()
          .putString("lastSession", project.directory.getAbsolutePath())
          .apply();
      ready = true;
      refreshTimeline();
    } catch (Exception e) {
      error(e);
      finish();
    }
  }

  private FrameSequence sequence() {
    return project.sequence;
  }

  Button control(String label, Runnable action) {
    Button b = new Button(this);
    b.setText(label);
    b.setTextSize(12);
    b.setOnClickListener(
        v -> {
          try {
            action.run();
          } catch (Exception e) {
            error(e);
          }
        });
    return b;
  }

  void refreshTimeline() {
    new TimelinePanel(this).render(timeline, bar, sequence(), playing, onionEnabled);
  }

  private void capture() throws IOException {
    readDrawing().confirmCurve();
    String id = sequence().current();
    File target = project.frame(id), tmp = new File(project.directory, id + ".tmp");
    try (ParcelFileDescriptor fd =
        ParcelFileDescriptor.open(
            tmp,
            ParcelFileDescriptor.MODE_CREATE
                | ParcelFileDescriptor.MODE_TRUNCATE
                | ParcelFileDescriptor.MODE_READ_WRITE)) {
      if (!readDrawing().writeProject(fd.getFd()))
        throw new IOException("No se pudo guardar el fotograma");
    }
    Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
    Bitmap image = readDrawing().snapshot();
    try (OutputStream out = new FileOutputStream(project.preview(id))) {
      if (!image.compress(Bitmap.CompressFormat.PNG, 100, out))
        throw new IOException("No se pudo guardar la vista previa");
    } finally {
      image.recycle();
    }
    project.checkpoint();
    markDocumentClean("Animación · " + (sequence().selected() + 1));
  }

  private void loadCurrent() throws IOException {
    try (ParcelFileDescriptor fd =
        ParcelFileDescriptor.open(
            project.frame(sequence().current()), ParcelFileDescriptor.MODE_READ_ONLY)) {
      if (!readDrawing().readProject(fd.getFd())) throw new IOException("Fotograma incompatible");
    }
    readDrawing().projectOpened();
    markDocumentClean("Animación · " + (sequence().selected() + 1));
    updateOnion();
  }

  void switchTo(int index) {
    stop();
    if (index == sequence().selected()) return;
    int old = sequence().selected();
    try {
      capture();
      sequence().select(index);
      loadCurrent();
      project.checkpoint();
      refreshTimeline();
    } catch (Exception e) {
      sequence().select(old);
      error(e);
    }
  }

  void add(boolean duplicate) {
    stop();
    try {
      capture();
      String old = sequence().current();
      String id = sequence().insert();
      try {
        if (duplicate) {
          Files.copy(project.frame(old).toPath(), project.frame(id).toPath());
          loadCurrent();
        } else if (!readDrawing()
            .newDocument(readDrawing().documentWidth(), readDrawing().documentHeight()))
          throw new IOException("No hay memoria para otro fotograma");
        capture();
        updateOnion();
        refreshTimeline();
      } catch (Exception e) {
        sequence().delete();
        loadCurrent();
        throw e;
      }
    } catch (Exception e) {
      error(e);
    }
  }

  void remove() {
    stop();
    if (sequence().size() == 1) {
      message("Debe quedar un fotograma");
      return;
    }
    try {
      capture();
      sequence().delete();
      loadCurrent();
      project.checkpoint();
      refreshTimeline();
    } catch (Exception e) {
      error(e);
    }
  }

  void move(int delta) {
    stop();
    try {
      capture();
      sequence().move(delta);
      project.checkpoint();
      updateOnion();
      refreshTimeline();
    } catch (Exception e) {
      error(e);
    }
  }

  void chooseFps() {
    stop();
    SeekBar slider = new SeekBar(this);
    slider.setMax(59);
    slider.setProgress(sequence().fps() - 1);
    TextView value = new TextView(this);
    value.setText(sequence().fps() + " FPS");
    LinearLayout box = new LinearLayout(this);
    box.setOrientation(android.widget.LinearLayout.VERTICAL);
    box.addView(value);
    box.addView(slider);
    slider.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {
          public void onProgressChanged(SeekBar b, int v, boolean u) {
            value.setText((v + 1) + " FPS");
          }

          public void onStartTrackingTouch(SeekBar b) {}

          public void onStopTrackingTouch(SeekBar b) {}
        });
    new AlertDialog.Builder(this)
        .setTitle("Velocidad de reproducción")
        .setView(box)
        .setPositiveButton(
            "Aplicar",
            (d, w) -> {
              sequence().fps(slider.getProgress() + 1);
              try {
                project.checkpoint();
              } catch (Exception e) {
                error(e);
              }
              refreshTimeline();
            })
        .setNegativeButton("Cancelar", null)
        .show();
  }

  private static Bitmap decode(File file, int max) {
    BitmapFactory.Options o = new BitmapFactory.Options();
    o.inJustDecodeBounds = true;
    BitmapFactory.decodeFile(file.toString(), o);
    o.inSampleSize = 1;
    while (o.outWidth / o.inSampleSize > max || o.outHeight / o.inSampleSize > max)
      o.inSampleSize *= 2;
    o.inJustDecodeBounds = false;
    return BitmapFactory.decodeFile(file.toString(), o);
  }

  void toggleOnion() {
    stop();
    onionEnabled = !onionEnabled;
    updateOnion();
    refreshTimeline();
  }

  void updateOnion() {
    int i = sequence().selected();
    onion.set(
        onionEnabled && i > 0 ? decode(project.preview(sequence().id(i - 1)), 1024) : null,
        onionEnabled && i + 1 < sequence().size()
            ? decode(project.preview(sequence().id(i + 1)), 1024)
            : null,
        onionEnabled);
    readDrawing().invalidate();
  }

  void togglePlayback() {
    if (playing) {
      stop();
      refreshTimeline();
      return;
    }
    try {
      capture();
      playing = true;
      shown = -1;
      preview.setVisibility(View.VISIBLE);
      preview.bringToFront();
      playback =
          new PlaybackClock(
              SystemClock.uptimeMillis(),
              sequence().selected(),
              sequence().size(),
              sequence().fps());
      clock.post(tick);
      refreshTimeline();
    } catch (Exception e) {
      error(e);
    }
  }

  void stop() {
    playing = false;
    clock.removeCallbacks(tick);
    if (preview != null) {
      preview.setVisibility(View.GONE);
      preview.setImageDrawable(null);
    }
    if (playbackBitmap != null) {
      playbackBitmap.recycle();
      playbackBitmap = null;
    }
  }

  void picker(int request, String name) {
    stop();
    try {
      capture();
      startActivityForResult(
          new Intent(Intent.ACTION_CREATE_DOCUMENT)
              .addCategory(Intent.CATEGORY_OPENABLE)
              .setType("application/zip")
              .putExtra(Intent.EXTRA_TITLE, name),
          request);
    } catch (Exception e) {
      error(e);
    }
  }

  @Override
  protected void onActivityResult(int request, int result, Intent data) {
    if (request != SAVE_ANIMATION && request != EXPORT_PNG) {
      super.onActivityResult(request, result, data);
      return;
    }
    if (result != RESULT_OK || data == null || data.getData() == null) return;
    try (OutputStream out = getContentResolver().openOutputStream(data.getData(), "wt")) {
      if (out == null) throw new IOException("No se puede escribir");
      project.export(out, request == EXPORT_PNG);
      message("Animación guardada");
    } catch (Exception e) {
      error(e);
    }
  }

  @Override
  public void onConfigurationChanged(android.content.res.Configuration configuration) {
    super.onConfigurationChanged(configuration);
    if (preview != null) {
      if (preview.getParent() instanceof ViewGroup)
        ((ViewGroup) preview.getParent()).removeView(preview);
      readWorkspaceFrame().addView(preview, new FrameLayout.LayoutParams(-1, -1));
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (ready) refreshTimeline();
  }

  @Override
  protected void onSaveInstanceState(Bundle state) {
    if (project != null) state.putString("session", project.directory.getAbsolutePath());
    super.onSaveInstanceState(state);
  }

  @Override
  protected void onPause() {
    stop();
    if (ready)
      try {
        capture();
      } catch (Exception e) {
        error(e);
      }
    super.onPause();
  }

  @Override
  protected void onDestroy() {
    stop();
    onion.clear();
    super.onDestroy();
  }

  @Override
  public void onBackPressed() {
    stop();
    try {
      if (ready) capture();
      super.onBackPressed();
    } catch (Exception e) {
      error(e);
    }
  }

  private void error(Exception e) {
    Toast.makeText(this, "No se pudo completar: " + e.getMessage(), Toast.LENGTH_LONG).show();
  }
}
