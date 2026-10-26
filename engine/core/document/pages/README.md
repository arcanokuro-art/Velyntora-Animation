# Paged document pixels

The document limit is **6,000 × 6,000 pixels**. Canvas and layers use the same
4096-pixel immutable/COW page representation. Blank layers store their background
color without allocating an image. The shared resident cache is limited to 16 MiB;
evicted pages use unlinked session files, segmented at 32 MiB so abandoned pages
can release their backing files. The native session mutex serializes page access.

History records reuse immutable page offsets and restore pages lazily. Stroke
coverage allocates only touched blocks. Effects and transforms use paged output
and paged intermediate values instead of full-document ARGB/float buffers.

JPEG and generic Android image import decode bounded regions. Noninterlaced RGB
and RGBA PNG import without custom color profiles uses a single inflater stream and bounded stripes. PNG color profiles and nonstandard gamma use the Android decoder. Imported
coordinates and EXIF orientation remain at original resolution within the limit. Oversized images require explicit confirmation and are proportionally filtered into bounded stripes. The display uses
native sampled tiles and a bounded preview; that preview is not document data.
PNG, BMP, TGA, TIFF, GIF and PPM export read original-resolution native rows.
Bitmap-based codecs and dense selection masks still have higher transient memory
requirements; large dimensions do not promise unlimited layers or disk capacity.

Only native storage adapter types changed in the protected shared raster and
selection transform files. Brush and Línea/Curva geometry remain unchanged.
