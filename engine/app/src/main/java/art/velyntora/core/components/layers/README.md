# Capas

- `panel/`: lista compacta, miniaturas con transparencia y controles fijos.
- `actions/`: operaciones del documento.
- `properties/`: edición en borrador y desplegable de mezcla.
- `blending/`: 16 carpetas independientes, una por mezcla; matemática compartida en `shared/`.

El equivalente nativo de las mezclas está en `core/components/layers/blending/`. Los modelos y proyectos pertenecen a `core/document/` y `document/project/`. Los paquetes Java se mantienen compatibles con JNI. Las herramientas no se modifican.

Pruebas: alfa y composición, paridad Java/nativa, duplicar y combinar, persistencia VLYCORE v1/v2, OpenRaster y diálogo Android con Aceptar/Cancelar/deshacer y capturas.
