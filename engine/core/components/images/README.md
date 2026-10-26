# Retained image elements

Each Layer may own an immutable image Element: nine crop/transform values plus an original encoded Source. Source bytes are streamed into an unlinked disk file, shared across document copies, duplicates and history. ProjectIO v3 streams sources into native projects; v1/v2 remain supported. HistoryArchive retains immutable element references while raster pages use existing disk history.

Actual pixel mutations, merge and document geometry operations discard retained metadata so reopening can never overwrite unrelated edits with an obsolete original. Opacity, visibility, blending, names, layer order and duplication preserve metadata. Explicit rasterization is undoable.
