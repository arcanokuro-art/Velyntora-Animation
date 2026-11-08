# Importación de video persistente

Hasta 130 minutos a 1–30 FPS: 234 000 fotogramas. El selector inicia un servicio foreground en el proceso privado :video_import. Su lienzo nativo es independiente del editor, que guarda su documento antes de iniciar y puede seguir usándose.

VideoImportJob guarda la solicitud, estado y proyecto de destino usando archivos reemplazados atómicamente. VideoImportJournal registra cada fotograma con índice, UUID determinista y CRC, y hace fsync después de guardar y sincronizar el documento nativo y la previsualización. Al reiniciar se valida el registro, se trunca cualquier cola incompleta y se continúa desde el último registro válido. El proyecto aparece en recientes solo tras publicar el manifiesto completo.

La notificación permite ver el progreso y pausar. La pantalla de progreso ofrece segundo plano, pausa, cancelación y reanudación; al terminar permite abrir el proyecto. Salir de la actividad no cancela el servicio. Se pide acceso persistente al URI elegido con ACTION_OPEN_DOCUMENT. Una pausa solicitada por el usuario se respeta; un trabajo interrumpido vuelve a iniciarse al abrir Velyntora. No se ejecuta código mientras el dispositivo está apagado ni se elude la detención forzada del sistema.

Android 15 usa el tipo mediaProcessing y su límite de seis horas de foreground processing; versiones anteriores usan dataSync. onTimeout detiene el servicio y conserva el registro para reanudar desde la app. Una wakelock parcial mantiene el trabajo con pantalla apagada, hasta el límite del servicio. El sistema, el ahorro de batería del fabricante y el espacio disponible pueden interrumpirlo; el avance confirmado permanece en disco.

Referencias:
https://developer.android.com/develop/background-work/services/fgs/timeout
https://developer.android.com/develop/background-work/services/fgs/handle-user-stopping

## Ruta de extracción optimizada

VideoFrameDecoder hace un seek inicial al keyframe anterior al punto de reanudación y luego decodifica en orden con MediaExtractor/MediaCodec. Mantiene dos buffers YUV para elegir el fotograma más cercano al timestamp solicitado; solo convierte a RGBA los fotogramas elegidos. YuvFrameConverter convierte en C++ usando los strides, recorte, matriz BT.601/BT.709 y rango de color del decodificador.

LosslessFrameStore escribe el fotograma original en objects/<uuid>.frame.png con compresión PNG sin pérdida (zlib nivel 1, filtro Sub) y una miniatura independiente de hasta 256 px. El proyecto editable nativo se materializa al abrir o editar el fotograma; no se crea durante la extracción. La reproducción y la piel de cebolla pueden leer el PNG completo, no están limitadas por la miniatura.

El registro prepara entradas en memoria. Antes de confirmarlas, sincroniza los archivos de ese lote y después hace fsync del registro. Se confirma cada 24 fotogramas o aproximadamente cada segundo; una pausa normal confirma también el lote pendiente. Una caída brusca puede requerir repetir el último lote sin confirmar, sin perder los lotes confirmados. Los trabajos antiguos con fotogramas .vlycore se pueden reanudar y mezclar con nuevos fotogramas PNG.

La resolución Original es la predeterminada. Solo se reduce si el usuario elige otra resolución. El motor trabaja con SDR de 8 bits; HDR, BT.2020 o salidas de mayor profundidad no se convierten silenciosamente a SDR. La reconstrucción YUV→RGB es la decodificación de video, no una copia de los bytes comprimidos originales; PNG no añade una compresión con pérdida a los píxeles resultantes. Elegir menos FPS conserva menos instantes del movimiento.

El .vlyanim usa VLYANIM-2 para admitir fotogramas raster y nativos; se siguen leyendo proyectos VLYANIM-1.
