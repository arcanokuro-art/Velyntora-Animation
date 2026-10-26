package art.velyntora.core;

import java.util.ArrayDeque;

/** Session history of completed handle drags, independent of pixel history. */
final class CurveHistory {
  private final ArrayDeque<CurveDraft> undo = new ArrayDeque<>(), redo = new ArrayDeque<>();

  void clear() {
    undo.clear();
    redo.clear();
  }

  void record(CurveDraft before, CurveDraft after) {
    if (before == null
        || after == null
        || before.sameGeometry(after)) return;
    undo.addLast(before.copy());
    redo.clear();
  }

  CurveDraft undo(CurveDraft current) {
    if (current == null || undo.isEmpty()) return null;
    redo.addLast(current.copy());
    return undo.removeLast().copy();
  }

  CurveDraft redo(CurveDraft current) {
    if (current == null || redo.isEmpty()) return null;
    undo.addLast(current.copy());
    return redo.removeLast().copy();
  }
}
