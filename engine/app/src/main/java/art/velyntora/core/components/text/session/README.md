# Text editing coordinator

Regenerates glyph sources off the UI thread and applies immutable TextSpec updates to the pending object. Rewriting preserves the rotated top-left anchor and scale, with the shared transform limit of 24000 px. The object session owns gestures, modal history and atomic confirmation. Cancelling a text dialog does nothing; cancelling the object restores the confirmed layer.
