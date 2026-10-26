# Velyntora Core — progreso verificable

Reorganización por herramientas del 10 de octubre: **8/8 entregables, 100.00 %**, con APK y 13 pruebas Android aprobadas en cada uno de los dos tamaños de fuente. Evidencia y denominador independiente: [REORGANIZATION.md](REORGANIZATION.md). Organización actual: [TOOLS_ARCHITECTURE.md](TOOLS_ARCHITECTURE.md).

El plan mantiene **20 entregables de igual peso (5 puntos cada uno)**. Un hito se contabiliza al integrarse y superar compilación y pruebas; no se asignan puntos a implementaciones parciales. El plan mide una aplicación Android de dibujo inspirada en Pinta, no un inventario exhaustivo de todos sus comandos, variantes de codecs o igualdad de algoritmos.

**Programación del plan: 20/20 = 100.00 %.** Cerrados 09, 16 y 20 con los criterios y pruebas detallados abajo.

- [x] 01. Proyecto Android con compilación automatizada.
- [x] 02. Motor de lienzo C++ y pruebas básicas.
- [x] 03. Pincel y herramientas geométricas básicas.
- [x] 04. Abrir imágenes y exportar PNG.
- [x] 05. Capas básicas conectadas al lienzo.
- [x] 06. Deshacer/rehacer multicapa.
- [x] 07. Panel de capas con miniaturas y opacidad.
- [x] 08. Recursos originales de Pinta adaptados a la interfaz Android.
- [x] 09. Espacio de trabajo adaptable equivalente en sus regiones funcionales.
- [x] 10. Selecciones rectangulares y elípticas.
- [x] 11. Selección libre y varita mágica.
- [x] 12. Mover, recortar y transformar selecciones.
- [x] 13. Texto y formas adicionales.
- [x] 14. Pinceles, borrador alfa y configuración avanzada.
- [x] 15. Zoom, panorámica y rotación.
- [x] 16. Gestión completa de tamaños y formatos del catálogo portátil documentado.
- [x] 17. Ajustes de color.
- [x] 18. Efectos y filtros: cobertura de las 37 familias del catálogo.
- [x] 19. Proyectos editables con capas.
- [x] 20. Pulido, accesibilidad y corrección de defectos detectados durante desarrollo.

## Cierre de los tres hitos restantes

**09 — Interfaz adaptable.** Menús y formularios desplazables; herramientas/capas en columnas cuando queda lienzo suficiente y paneles superpuestos en teléfono. Se adapta a ventanas bajas y fuentes grandes. Rotación conserva capas y documento. Estado con nombre, cambios pendientes, dimensiones, cursor, zoom y rotación. Nombres Unicode de capas editables y con historial; colores primario/secundario, intercambio y cinco modos de degradado. Atajos de teclado y controles accesibles.

**16 — Tamaños y formatos.** Medidas personalizadas y rápidas, proporciones y porcentajes; remuestreo cercano/bilineal con alfa premultiplicado, nueve anclajes de lienzo y recorte multicapa con historial. Detección de archivos y lectores/escritores para las familias enumeradas en [FORMAT_SUPPORT.md](FORMAT_SUPPORT.md), incluyendo los lectores del núcleo GDK de referencia y SVG/WebP/PCX. La lista de formatos de Pinta depende de los módulos instalados; Core publica una matriz Android portátil explícita con sus límites y variantes rechazadas.

**20 — Calidad y seguridad de documentos.** Objetivos de toque de 48 dp, etiquetas y estado activo; trabajo de IO y remuestreo fuera de interfaz y bloqueo durante operaciones modales. Aviso antes de sustituir cambios pendientes; Guardar conserva capas en proyecto editable. Revisión nativa sigue Undo/Redo y guardado. Recuperación atómica en segundo plano, con rechazo de revisión obsoleta. Formas y cubeta respetan selección, alfa y opacidad; cancelar una forma no crea historial vacío. Corregidos los defectos hallados en las pruebas automatizadas de este cierre.

Criterios detallados: [DEVELOPMENT_CHECKS.md](DEVELOPMENT_CHECKS.md). Las implementaciones y pruebas anteriores permanecen en [PROGRESS_HISTORY.md](PROGRESS_HISTORY.md).

## Evidencia de desarrollo

- 23 suites Java: formatos, proyectos, dimensiones, geometría, navegación, colores y disposición.
- 16 suites C++ del motor, más integración de proyectos y JVM/JNI real.
- Compilación APK Android.
- Seis pruebas Android en emulador API 29, repetidas con fuentes 1.0 y 1.8: rotación/capas, cancelación de cambios sin guardar, recuperación, formas/degradado con historial, SVG/XPM y objetivos de toque.

Candidato de código verificado: `07f4ada281d25ba516e5f0696e7b6de09ef2a4f7`.

| Comprobación | Ejecución | Resultado |
|---|---|---|
| Core: Java, C++ y JNI | [38018987267](https://github.com/arcanokuro-art/Velyntora-Core/actions/runs/38018987267) | Aprobado |
| APK | [38018987274](https://github.com/arcanokuro-art/Velyntora-Core/actions/runs/38018987274) | Aprobado; artefacto adjunto |
| Android API 29, fuentes 1.0/1.8 | [38018987269](https://github.com/arcanokuro-art/Velyntora-Core/actions/runs/38018987269) | Seis pruebas aprobadas en cada ejecución |

Integración mediante [PR #3](https://github.com/arcanokuro-art/Velyntora-Core/pull/3). Los workflows se ejecutan también sobre el commit resultante en main; su estado se consulta en Actions.

## Qué significa el 100 %

Termina la **programación del plan de 20 hitos**. No promete paridad exhaustiva ni ausencia de defectos futuros. Las diferencias de formatos se conservan en FORMAT_SUPPORT.md; las de efectos, en [EFFECTS_PARITY.md](EFFECTS_PARITY.md). Curvas cúbicas con cuatro tiradores e historial de borrador; caché de reapertura limitada a 15 curvas. Los degradados se confirman al terminar el arrastre; no tienen edición posterior de tiradores. No hay animación, impresión/DPI ni decodificadores opcionales de terceros.

La instalación y revisión manual en el teléfono del usuario siguen siendo una fase posterior e independiente, fuera del denominador, según la decisión registrada en [CONTINUITY.md](CONTINUITY.md).
