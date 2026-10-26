# Almacenamiento experimental por bloques

Rama `codex/large-canvas-12000`. No está conectado todavía con la aplicación Android:
el límite de producción continúa en 16 millones de píxeles. No subir ese límite
hasta completar y validar la integración.

`TileSurface` admite 1–6000 píxeles por lado, bloques de 256 × 256, fondo
implícito y lecturas/escrituras de regiones de hasta 1048576 píxeles. Los bloques
modificados se escriben en un archivo temporal privado. Caché LRU de 8 MiB por
almacén; snapshots comparten registros inmutables y sólo reemplazan bloques
modificados. El llamador conserva el bloqueo de sesión durante las operaciones.

El presupuesto cuenta el caché, no el buffer de región del llamador ni los dos
bloques temporales durante una modificación. Un documento vacío no asigna bloques.
Errores de E/S abortan una escritura sin sustituir los píxeles del documento.

Este archivo temporal no sustituye el guardado del proyecto. `compact()` copia los
bloques vivos a otro almacén y conserva el antiguo para snapshots que todavía lo
usen. Falta definir su planificación y presupuesto global de disco. Falta conectar
capas/mezclas, historia, herramientas, selecciones, importación
regional, visualización por nivel de zoom, persistencia y exportación por tiras.
No distribuir esta base como soporte completo de lienzos grandes.

## Integración parcial de latencia

`LayerPixels` mantiene capas por bloques en memoria, con fondo implícito y copia
al escribir. `StrokeTiles` sustituye los buffers completos de opacidad por bloques
tocados. El final de un gesto continuo sólo refresca su región. Las capas admiten
hasta 32 entradas; el proyecto v2 conserva la representación de píxeles existente.
El lienzo activo y el Bitmap Android todavía son completos: abrir imágenes grandes
a resolución original sigue pendiente y no debe anunciarse como implementado.
