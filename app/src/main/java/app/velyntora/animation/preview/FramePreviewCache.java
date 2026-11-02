package app.velyntora.animation.preview;

import android.graphics.*;
import android.os.*;
import android.util.LruCache;
import java.io.File;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** Bounded thumbnails/playback; decoding is off the UI thread. Cache eviction uses normal GC. */
public final class FramePreviewCache {
  private final LruCache<String, Bitmap> cache =
      new LruCache<String, Bitmap>(24 * 1024 * 1024) {
        protected int sizeOf(String key, Bitmap value) {
          return value.getAllocationByteCount();
        }
      };
  private final ExecutorService worker = Executors.newSingleThreadExecutor();
  private final Handler ui = new Handler(Looper.getMainLooper());
  private final HashMap<String, ArrayList<Consumer<Bitmap>>> pending = new HashMap<>();
  private boolean closed;

  public Bitmap get(String asset, int side) {
    return cache.get(asset + "@" + side);
  }

  public void load(String asset, File file, int side, Consumer<Bitmap> callback) {
    if (closed || asset == null) return;
    String key = asset + "@" + side;
    Bitmap ready = cache.get(key);
    if (ready != null) {
      callback.accept(ready);
      return;
    }
    ArrayList<Consumer<Bitmap>> callbacks = pending.get(key);
    if (callbacks != null) {
      if (callbacks.size() < 32) callbacks.add(callback);
      return;
    }
    callbacks = new ArrayList<>();
    callbacks.add(callback);
    pending.put(key, callbacks);
    worker.execute(
        () -> {
          Bitmap bitmap = null;
          try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.toString(), o);
            o.inSampleSize = 1;
            while (o.outWidth / o.inSampleSize > side || o.outHeight / o.inSampleSize > side)
              o.inSampleSize *= 2;
            o.inJustDecodeBounds = false;
            bitmap = BitmapFactory.decodeFile(file.toString(), o);
          } catch (RuntimeException | OutOfMemoryError ignored) {
          }
          final Bitmap result = bitmap;
          ui.post(
              () -> {
                ArrayList<Consumer<Bitmap>> waiting = pending.remove(key);
                if (closed) return;
                if (result != null) cache.put(key, result);
                if (waiting != null)
                  for (Consumer<Bitmap> consumer : waiting) consumer.accept(result);
              });
        });
  }

  public void close() {
    closed = true;
    pending.clear();
    worker.shutdownNow();
    cache.evictAll();
  }
}
