# Editor de animación — 0.3.0 piloto

## Responsabilidades

- `animation/model/AnimationSequence`: fotogramas, selección, orden, exposiciones y FPS. Sin dependencias Android.
- `animation/storage/AnimationProject`: manifiesto atómico, objetos nativos inmutables, sesiones recientes y archivos `.vlyanim`.
- `animation/session/AnimationSession`: comandos serializados, guardado, cambios de fotograma y exportación.
- `animation/session/FrameDocumentAdapter`: adaptador al motor nativo. Las clases adaptadoras comparten el paquete Java del motor para usar sus interfaces existentes; su código vive en la app de Animation.
- `animation/preview/FramePreviewCache`: decodificación en segundo plano y caché de hasta 24 MiB.
- `animation/playback`: reloj monotónico y reproducción sin reemplazar el documento editable. Las exposiciones determinan cuánto dura cada fotograma; si la decodificación se retrasa se salta a la posición temporal vigente.
- `animation/timeline`: celdas visibles dibujadas en un solo View, miniaturas y controles. No modifica píxeles.
- `animation/rendering/OnionSkin`: previsualización roja/azul de vecinos con el zoom, giro y desplazamiento del lienzo. No se incluye al exportar.
- `animation/setup`: nombre, tamaños predefinidos/personalizados y FPS.
- `art/velyntora/core/AnimationActivity`: composición, navegación y ciclo de vida.

## Persistencia

Cada fotograma conserva un documento nativo con sus capas; la miniatura PNG se limita a 1024 px por lado. Al duplicar, los fotogramas comparten un objeto inmutable hasta que se edita uno. El guardado escribe un objeto nuevo y sustituye el manifiesto atómicamente, manteniendo la última versión completa si falla la escritura. Los objetos huérfanos se recogen después del checkpoint correcto.

Las sesiones se guardan al cambiar de fotograma, al salir y después de un periodo sin interacción. Guardar animación crea un `.vlyanim` externo. Las sesiones internas se eliminan si Android borra los datos de la app o se desinstala; los archivos exportados se conservan fuera de la app.

El `.vlyanim` es un ZIP: `project.properties` en UTF-8 y `objects/<uuid>.vlycore`/`objects/<uuid>.png`. El manifiesto contiene formato `VLYANIM-1`, título, dimensiones iniciales, FPS, selección y los campos `frame.N=id,asset,exposure`. Solo se admiten rutas controladas, hasta 72 000 fotogramas, FPS de 1 a 30, manifiestos de hasta 32 MiB y 512 MiB por objeto. El presupuesto descomprimido depende del espacio libre con una reserva de 16 MiB. La importación de video admite fragmentos de hasta 40 minutos; a 30 FPS equivale a 72 000 fotogramas. El índice de tiempos usa búsquedas binarias y un caché acumulado.

La secuencia PNG se exporta a resolución completa mediante filas, sin reservar un bitmap completo por fotograma. `timing.csv` incluye exposición y FPS. Las herramientas de imagen del motor siguen actuando sobre el fotograma seleccionado, incluido su tamaño; las dimensiones del proyecto sirven como tamaño de los nuevos fotogramas.

## Alcance de esta etapa

Incluye creación/apertura, sesiones recientes, preferencias de FPS, capas por fotograma, altas/duplicación/borrado/reordenación, duración, reproducción en bucle, piel de cebolla y exportación PNG. Deshacer/rehacer del motor se aplica al fotograma editable; no existe todavía historial global de operaciones de la línea de tiempo.

Audio, MP4/GIF animado e interpolación vectorial/IA se integrarán en etapas posteriores. No se modifican las implementaciones protegidas de pinceles, Línea/Curva ni JNI. `main` se mantiene como versión anterior hasta revisar esta rama. No debe ejecutarse una regeneración de `engine/` sin conservar los tres puntos de extensión de Animation: entrada, actividad extensible y superposición del lienzo.
