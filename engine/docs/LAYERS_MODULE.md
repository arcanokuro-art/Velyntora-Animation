# Capas modulares

Panel compacto con miniaturas y fondo de transparencia, nombres y casillas de visibilidad. Lista desplazable independiente; acciones fijas: añadir, eliminar, duplicar, combinar abajo, subir y bajar. Propiedades accesibles por doble toque, pulsación larga o menú Capas; edición en borrador, validación y aceptación atómica con un checkpoint de historial.

## Propiedad del código

- components/layers/panel: composición de filas y contenedor.
- components/layers/actions: operaciones del lienzo.
- components/layers/properties: diálogo y selección del modo.
- components/layers/blending: 16 adaptadores Java independientes para exportación.
- core/components/layers/blending: 16 implementaciones nativas independientes, composición alfa compartida.
- core/document: estado, duplicación, combinación y persistencia.

VLYCORE v2 conserva modo de mezcla; se siguen leyendo proyectos v1 (Normal). Versiones antiguas de la aplicación no leen proyectos v2. OpenRaster conserva los 16 modos; O exclusivo utiliza extensión `velyntora:xor`. Combinar abajo hornea las dos capas en una capa Normal; con mezclas que dependen de capas inferiores el resultado puede cambiar, como ocurre al eliminar su dependencia del fondo.

Herramientas sin modificaciones. El manifiesto JNI incorpora cuatro nuevas funciones del módulo de capas; los cuerpos anteriores se conservan.
