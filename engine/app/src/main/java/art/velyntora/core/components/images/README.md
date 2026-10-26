# Image placement and retained editing

Separate insertion, transformation, crop, session and elements modules. Initial confirmation creates one layer. Subsequent selection reopens the original encoded source and its transform; confirmation replaces the same layer with one undo checkpoint, cancellation makes no document/history changes.

Core metadata and immutable disk-backed source: core/components/images/ImageElement.hpp. Native projects with image elements use VLYCORE v3, with v1/v2 still readable; ordinary projects continue to use v2. Raster and OpenRaster exports preserve the visible result, not retained transforms. Drawing/effects, merge, and canvas dimension operations rasterize affected image layers. Explicit rasterization is available in layer properties and can be undone. Text is outside this module.
