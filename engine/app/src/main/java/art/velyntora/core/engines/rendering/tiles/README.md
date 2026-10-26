# Canvas tile rendering

Small documents use TileDisplay; large documents use NativeTileDisplay with a bounded, complete overview. A cold cache always draws this overview while native detail tiles arrive asynchronously. Prepared frames no longer decode the whole document a second time to warm tiles. Partial edits update both the overview footprint and cached detail; large changes refresh the overview on a worker.

Tile gutters overlap without individual cell clips, inside an isolated SRC layer. This avoids rotation seams while preventing duplicate alpha at overlaps. Outside the document the workspace remains clipped. Temporary loss of detail during refinement is allowed; fabricated checkerboard holes are not. Device regressions cover cold 6000px caches at several zooms and rotations and opaque/translucent tile boundaries.
