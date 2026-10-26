# Retained image elements

Selecting an image layer reopens its original source, crop and transform. Modal edits use bounded asynchronous previews composited in the original layer order with the native blend modes. Cancel leaves the document and history untouched. Confirm atomically replaces that layer, retaining the source for subsequent edits. Original encoded bytes are stored on disk and shared by undo/redo and duplicate layers; native projects embed them.

Pixel edits, combining layers and canvas resize/crop rasterize the affected layer; raster exports contain the visible result. Text remains outside this feature.
