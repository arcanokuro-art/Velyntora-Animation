# Criterios de cierre de los hitos abiertos

Los 20 hitos conservan su peso de 5 puntos. Esta lista permite verificar 09, 16 y 20; no redefine la aplicación como equivalente píxel a píxel a Pinta.

## 09 — Espacio de trabajo adaptable

- Nombres de capas editables en UTF-8, visibles y accesibles, con Undo/Redo y límites validados.
- Colores primario/secundario editables e intercambiables; cinco modos de degradado con alfa, opacidad, selección y un paso de historial.
- Regiones de menús, comandos, herramientas, lienzo, capas, paleta y estado conectadas a acciones reales.
- Herramientas y capas en columnas cuando queda un lienzo de al menos 240 dp; paneles superpuestos con apertura/cierre en teléfono.
- Menús y formularios desplazables, reducción de filas accesorias en ventanas bajas y política probada con escala de fuente.
- Conservar documento, capas e historial al rotar; coordenadas del cursor, dimensiones, zoom, rotación, nombre y cambios pendientes en estado.
- Pruebas de política Java y pruebas Android del lienzo real en ambas orientaciones y fuente 1.8.

## 16 — Tamaños y formatos

- Operaciones de tamaño, proporción, porcentaje, anclajes y recorte con validación e historial.
- Inventario de lectores/escritores y diferencias explícitas en FORMAT_SUPPORT.md; la dependencia de decodificadores GDK se documenta.
- Pruebas de lectura/escritura, límites, archivos corruptos y orientación; pruebas Android de PNG/SVG y recursos de colores.
- No afirmar soporte de variantes que el lector rechaza ni anunciar formatos de terceros como implementados.

## 20 — Calidad del desarrollo y accesibilidad

- Objetivos de toque de 48 dp, nombres accesibles y estado activo en herramientas/capas; lienzo y controles etiquetados.
- Evitar sustitución silenciosa de cambios pendientes; guardar proyectos, cancelar descarte y mantener documento ante error.
- Las formas canceladas no crean historial; formas y cubeta respetan la selección y el alfa.
- Identidad de revisión conservada por Undo/Redo; recuperación atómica y rechazo de escritura de una revisión obsoleta.
- Lectura, escritura y remuestreo en trabajo; impedir acciones de edición mientras hay operación modal en curso.
- Compilación APK, suites Core/Java/JNI y pruebas de dispositivo aprobadas sobre el cambio final. Todo defecto que estas comprobaciones detecten debe corregirse antes de cerrar el hito.

La revisión manual del usuario en su teléfono sigue siendo una fase posterior. Las diferencias conocidas de formatos, efectos y herramientas se conservan documentadas y el cierre de un hito no promete ausencia de cualquier defecto futuro.
