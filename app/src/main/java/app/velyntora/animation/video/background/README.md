# Importación de video persistente

Hasta 130 minutos a 1–30 FPS: 234 000 fotogramas. El selector inicia un servicio foreground en el proceso privado :video_import. Su lienzo nativo es independiente del editor, que guarda su documento antes de iniciar y puede seguir usándose.

VideoImportJob guarda la solicitud, estado y proyecto de destino usando archivos reemplazados atómicamente. VideoImportJournal registra cada fotograma con índice, UUID determinista y CRC, y hace fsync después de guardar y sincronizar el documento nativo y la previsualización. Al reiniciar se valida el registro, se trunca cualquier cola incompleta y se continúa desde el último registro válido. El proyecto aparece en recientes solo tras publicar el manifiesto completo.

La notificación permite ver el progreso y pausar. La pantalla de progreso ofrece segundo plano, pausa, cancelación y reanudación; al terminar permite abrir el proyecto. Salir de la actividad no cancela el servicio. Se pide acceso persistente al URI elegido con ACTION_OPEN_DOCUMENT. Una pausa solicitada por el usuario se respeta; un trabajo interrumpido vuelve a iniciarse al abrir Velyntora. No se ejecuta código mientras el dispositivo está apagado ni se elude la detención forzada del sistema.

Android 15 usa el tipo mediaProcessing y su límite de seis horas de foreground processing; versiones anteriores usan dataSync. onTimeout detiene el servicio y conserva el registro para reanudar desde la app. Una wakelock parcial mantiene el trabajo con pantalla apagada, hasta el límite del servicio. El sistema, el ahorro de batería del fabricante y el espacio disponible pueden interrumpirlo; el avance confirmado permanece en disco.

Referencias:
https://developer.android.com/develop/background-work/services/fgs/timeout
https://developer.android.com/develop/background-work/services/fgs/handle-user-stopping
