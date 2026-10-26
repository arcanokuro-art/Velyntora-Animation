# Retained text model

A text layer owns immutable Unicode UTF-8 content and five typed typography values. Its image element stores the glyph cache and placement. Metadata follows layer copies, order, opacity, blend and history. Actual pixel mutations clear both text and image metadata. ProjectIO v4 stores text after the image source, while older project versions remain readable.
