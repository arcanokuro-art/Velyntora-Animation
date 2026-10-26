# Text previews

Dialog preview renders immutable typography drafts without touching document state. Existing text layers use vector glyphs between cached lower/upper planes, preserving layer order, opacity and supported native blend modes. Android below 29 and the custom RGB xor blend use the exact native compositor fallback. Confirmation always renders glyphs into document-resolution tiles, never enlarging the small source cache.
