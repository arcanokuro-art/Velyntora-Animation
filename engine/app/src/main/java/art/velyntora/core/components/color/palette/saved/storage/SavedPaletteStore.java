package art.velyntora.core;

import android.content.Context;
import android.content.SharedPreferences;
import art.velyntora.core.components.color.palette.saved.SavedPalette;

/** App-wide palette, independent of the active document and its undo history. */
final class SavedPaletteStore {
  private final SharedPreferences preferences;
  SavedPaletteStore(Context context){preferences=context.getSharedPreferences("saved-color-palette",Context.MODE_PRIVATE);}
  SavedPalette load(){return SavedPalette.decode(preferences.getString("colors-v1",""));}
  void save(SavedPalette palette){preferences.edit().putString("colors-v1",palette.encode()).apply();}
}
