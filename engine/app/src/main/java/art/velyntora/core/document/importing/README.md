# Image import

ImageImportSize owns proportional fitting to 6000 px per side / 49 million pixels and EXIF-aware displayed dimensions. DocumentStorage probes metadata on a worker, then asks permission to reduce an oversized image. Cancellation leaves the document unchanged; there is no silent reduction. 6067 × 9030 becomes 4031 × 6000.

RegionImageImporter imports supported Android region formats in bounded stripes. Oversized JPEG/PNG/WebP is filtered into bounded output stripes, preserving aspect ratio and all eight EXIF orientations. Images inside the limit retain original pixels and can use PngStripeImporter. Decoder and file access run on the import worker. Failed import restores the previous native document. Unsupported region formats use a sampled Bitmap fallback capped at four million decoded pixels before producing the confirmed output size. The fallback can lose more detail than the region decoder. Unreadable formats are rejected without changing the document.
