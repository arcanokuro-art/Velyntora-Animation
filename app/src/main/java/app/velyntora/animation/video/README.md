# Video a fotogramas

Implementación propia de los requisitos documentados en Velyntora-Para-Velyntora/video_to_frames_animation.

Archivo → Importar video como animación… abre el selector Android. El diálogo permite previsualizar el fragmento, definir inicio/final en segundos, FPS (1–60), resolución y comprobar la cantidad de fotogramas (máximo 2000).

VideoImportRequest valida el intervalo y calcula timestamps sin acumular errores. VideoFrameDecoder usa MediaMetadataRetriever y procesa un bitmap cada vez. VideoImportDialog gestiona metadatos, previsualización y configuración. AnimationSession escribe un proyecto nuevo y publica su manifiesto solo cuando la extracción se completa. Cancelar o fallar restaura el documento anterior y su historial y elimina el proyecto temporal.

Los formatos dependen de los decodificadores Android del dispositivo. El audio no se importa. La resolución se conserva dentro del máximo elegido; las diferencias de relación de aspecto se ajustan sin deformación y con fondo negro. Los FPS son una frecuencia de muestreo: aumentar el valor puede repetir imágenes y no genera movimiento nuevo. El historial de edición de cada fotograma comienza al editarlo.
