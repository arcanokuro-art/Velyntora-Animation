# Session history

HistoryArchive stores immutable 4096-pixel blocks and complete document metadata in an unlinked app-cache file. Unchanged blocks reuse their existing file offsets. LayerPixels invalidates a block's archived offset whenever it changes. Undo and redo retain only archive handles, offsets and curve/revision tags; there is no step count or RAM-budget truncation. Import primes its baseline on the import worker. Starting a new document drops the old session history. The file is reclaimed on close or process exit.

Storage exhaustion falls back to copy-on-write RAM snapshots rather than silently discarding steps. Available device storage and memory remain practical limits. History is session-local, not restored after closing the process. Page contents, layer ordering, selection, names, visibility, opacity, blend modes and document dimensions are restored together.
