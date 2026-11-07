package art.velyntora.core;

import android.app.AlertDialog;

/** Shared list dialog for sections and their nested choices. */
final class MenuChoices {
  static AlertDialog show(MainActivity host, String title, String[] labels, Runnable... actions) {
    return new AlertDialog.Builder(host).setTitle(title).setItems(labels, (dialog, index) -> {
      if (host.readProjectProgress() == null) actions[index].run();
    }).setNegativeButton("Cerrar", null).show();
  }
  private MenuChoices() {}
}
