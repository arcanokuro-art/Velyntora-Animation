# Guardado y exportación de Animation

Entrada única: botón Guardar (flecha sobre bandeja) junto a Abrir en la barra superior. AnimationActivity redirige projectPicker(true) al menú AnimationSaveMenu. Archivo y Más de la línea de tiempo ya no duplican estas acciones.

Cada formato tiene su módulo: project (.vlyanim), mp4 (H.264), webm (VP8), three_gp (H.264), gif (paleta RGB332 y transparencia binaria), apng (RGBA sin pérdida), png_sequence (ZIP con timing.csv) y png (fotograma actual). video comparte la implementación MediaCodec/MediaMuxer; los contenedores están registrados por separado.

AnimationFrames define acceso secuencial a fotogramas completos, con capas compuestas, exposiciones y FPS. AnimationSession captura los cambios, conserva el historial nativo, ejecuta el exportador y restaura el documento seleccionado incluso cuando hay error o cancelación. El archivo se genera primero en caché y se copia al destino elegido una vez que termina la codificación. Los archivos temporales se eliminan al terminar.

MP4/WebM/3GP dependen de los codificadores instalados en Android; si el formato o la resolución no están disponibles se muestra un error real. No se incluyen opciones simuladas para otros contenedores. Los videos salen sin audio, sin transparencia y con fondo blanco. Se conserva la proporción con encaje centrado y dimensiones pares. GIF reduce a 256 colores; APNG conserva alfa y color. Los formatos animados respetan las exposiciones y se reproducen en bucle; GIF redondea los tiempos a centésimas.

Referencias de plataforma:
- https://developer.android.com/reference/android/media/MediaCodec
- https://developer.android.com/reference/android/media/MediaMuxer.OutputFormat
