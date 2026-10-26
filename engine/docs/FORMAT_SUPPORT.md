# Formatos y tamaños: alcance verificable

Pinta registra los decodificadores disponibles en GDK de la instalación. El catálogo no es una lista universal fija: cambia por plataforma y módulos instalados. Referencias de solo lectura: `Pinta.Core/Managers/ImageConverterManager.cs` del commit `2cfea9cf1eae94e45e9d0ef98d76d9ccf4a98481` de Pinta-2.0 y `gdk-pixbuf/meson.build` de GNOME/gdk-pixbuf. Core implementa una matriz portátil explícita; la presencia de una familia no significa admitir todas las variantes del estándar.

| Familia | Abrir | Guardar | Variantes y diferencias |
|---|---|---|---|
| PNG | Android | PNG ARGB | Conserva alfa; no conserva perfiles ni metadatos de impresión. |
| JPEG | Android + orientación EXIF 1–8 | JPEG sobre blanco | Imagen estática; se pierden EXIF, perfiles y semitransparencias al exportar. |
| WebP | Android | Lossless desde API 30; compresión anterior en API 23–29 | Imagen fija. |
| GIF | Primera imagen Android | GIF89a fijo | Exportación con paleta exacta o cubo RGB y alfa binario; sin animación. |
| BMP | CORE/Windows, 1/4/8/16/24/32 bits, RLE4/8 y bitfields | RGB 24 bits sobre blanco | No decodifica JPEG/PNG incrustados ni colorimetría calibrada. |
| TGA | Indexado, gris y RGB, raw/RLE, 8/15/16/24/32 bits según tipo | BGRA 32 bits | Orígenes de ambos ejes; sin metadatos TGA 2.0. |
| TIFF | RGB/gris/paleta de 8 bits, tiras, LZW/PackBits/Deflate | RGBA sin compresión | II/MM, predictor y orientación. No BigTIFF, mosaicos, CMYK, 16 bits ni ICC. |
| ICO/CUR | PNG o DIB indexado/RGB, máscara AND; CUR dentro de ANI | ICO de 32 bits hasta 256 px | Mayor representación compatible; sin DIB comprimido. |
| PBM/PGM/PPM | P1–P6, 8/16 bits de muestra | PPM P6 | No PAM ni secuencias; exportación sobre blanco. |
| XBM | X10 short / X11 char | — | Monocromo, bits LSB y relleno por fila. |
| XPM | XPM3, claves hasta 8 caracteres, alfa None y colores X11 | — | No XPM1/2 ni escapes octales de C. |
| PCX | RGB 8×3, indexado 8×1, planar 1×1–4, RLE | — | No DCX multipágina ni planos de 2/4 bits. |
| SVG | Rasterización estática mediante AndroidSVG 1.4 | — | Sin filtros SVG, animación, recursos externos ni declaraciones de entidades. |
| ANI | Primera imagen según secuencia, ICO/CUR incrustado | — | No reproducción de animaciones. |
| ICNS | PNG moderno; ARGB/RLE clásico con máscara | — | Mayor PNG compatible o representación clásica; no JPEG 2000 ni iconos CLUT antiguos. |
| QuickTime image | Átomos idat con imágenes compatibles con Android | — | No vídeo ni codecs QuickTime históricos. |
| OpenRaster | Capas normales, nombres, orden, visibilidad y alfa | ORA | No grupos ni modos de mezcla adicionales. |
| VLYCORE | Documento editable con capas | VLYCORE v1 | Formato nativo con validación atómica. |

Abrir archivo detecta firmas de formatos binarios; TGA/XBM/XPM/SVG usan nombre o cabecera textual cuando procede. Todas las decodificaciones se ejecutan fuera del hilo de interfaz. Un error mantiene el documento anterior. En la rama experimental, el documento admite hasta **6.000 × 6.000 px**. JPEG y PNG conservan su resolución dentro del límite. Si lo superan, se solicita confirmación antes de reducir proporcionalmente por franjas (6067 × 9030 → 4031 × 6000); cancelar conserva el documento. PNG con perfiles de color usa el decodificador Android. El motor y la vista usan páginas y teselas con cachés acotadas. Los formatos auxiliares que todavía necesitan un Bitmap completo mantienen sus presupuestos de memoria; esto no garantiza que cualquier códec o selección densa de 36 millones de píxeles quepa en cualquier dispositivo.

Crear documento: medidas personalizadas y cinco tamaños rápidos. Redimensionar imagen: proporción bloqueable, porcentajes decimales, vecino cercano/bilineal con alfa premultiplicado. Redimensionar lienzo: nueve anclajes y relleno transparente. Recorte multicapa e historial de dimensiones. No incorpora unidades físicas/DPI ni impresión.

La tabla debe mantenerse junto al código y las pruebas. Las extensiones GDK adicionales instaladas por terceros (por ejemplo HEIF/JPEG XL o lectores de Windows WMF/EMF) no constituyen formatos portátiles implementados por Core. No se debe describir esta matriz como paridad exhaustiva con cualquier instalación de Pinta.
