# Editable text layers

TextDialog creates or edits Unicode text and typography. TextSpec owns the immutable model and TextSourceRenderer builds a bounded glyph cache off the UI thread. The shared object session provides transform handles, opacity, local undo/redo, confirm and cancel. Applying the dialog changes only the pending object; ✓ commits one independent text layer or replaces the same existing layer. X discards the pending changes.

Native text metadata belongs to core/components/text and TextElementBridge.inc. VLYCORE v4 preserves text, its style, source glyph cache and placement. v1/v2/v3 remain readable. Raster exports preserve the visible result. Pixel effects, merge and document resize/crop rasterize the affected element; undo restores editable metadata. Existing raster text insertion remains available for compatibility but the user-facing text tool uses retained layers.

Typography controls use local sliders, live vector preview and an independent single-color picker. Displayed font size and wrapping width account for the current transform. Applying typography normalizes the vertical scale and retains horizontal stretching and the top-left anchor. Glyphs are drawn directly into document-resolution tiles on confirmation; the small source PNG is never enlarged for the final result. Font size supports 4–6000 px, with source bounds validated before applying.
