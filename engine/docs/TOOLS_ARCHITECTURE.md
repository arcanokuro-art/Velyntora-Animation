# Herramientas de Velyntora Core

La carpeta raíz `tools/` reúne código Android, iconos y código nativo por herramienta. Se conservan los 29 identificadores existentes y la firma JNI de `DrawingView`: los proyectos y los accesos de la interfaz no necesitan migración.

## Inventario

| Herramienta | Carpeta | Controlador | Icono |
|---|---|---|---|
| Pincel | `tools/brush/` | `BrushTool.java` | `BrushIcon.java` |
| Cubeta de pintura | `tools/bucket/` | `BucketTool.java` | `BucketIcon.java` |
| Círculo | `tools/figuras/sin_relleno/circulo/` | `CircleTool.java` | `CircleIcon.java` |
| Tampón de clonar | `tools/clone/` | `CloneTool.java` | `CloneIcon.java` |
| Elipse | `tools/figuras/sin_relleno/elipse/` | `EllipseTool.java` | `EllipseIcon.java` |
| Selección elíptica | `tools/ellipse_selection/` | `EllipseSelectionTool.java` | `EllipseSelectionIcon.java` |
| Borrador | `tools/eraser/` | `EraserTool.java` | `EraserIcon.java` |
| Elipse rellena | `tools/figuras/con_relleno/elipse/` | `FilledEllipseTool.java` | `FilledEllipseIcon.java` |
| Rectángulo relleno | `tools/figuras/con_relleno/rectangulo/` | `FilledRectangleTool.java` | `FilledRectangleIcon.java` |
| Rectángulo redondeado relleno | `tools/figuras/con_relleno/rectangulo_redondeado/` | `FilledRoundedRectangleTool.java` | `FilledRoundedRectangleIcon.java` |
| Triángulo relleno | `tools/figuras/con_relleno/triangulo/` | `FilledTriangleTool.java` | `FilledTriangleIcon.java` |
| Forma libre | `tools/freeform/` | `FreeformTool.java` | `FreeformIcon.java` |
| Degradado | `tools/gradient/` | `GradientTool.java` | `GradientIcon.java` |
| Selección libre | `tools/lasso_selection/` | `LassoSelectionTool.java` | `LassoSelectionIcon.java` |
| Línea/Curva | `tools/line_curve/` | `LineCurveTool.java` | `LineCurveIcon.java` |
| Varita mágica | `tools/magic_wand/` | `MagicWandTool.java` | `MagicWandIcon.java` |
| Mover los píxeles seleccionados | `tools/move_pixels/` | `MovePixelsTool.java` | `MovePixelsIcon.java` |
| Mover selección | `tools/move_selection/` | `MoveSelectionTool.java` | `MoveSelectionIcon.java` |
| Desplazamiento | `tools/pan/` | `PanTool.java` | `PanIcon.java` |
| Lápiz | `tools/pencil/` | `PencilTool.java` | `PencilIcon.java` |
| Cuentagotas | `tools/picker/` | `PickerTool.java` | `PickerIcon.java` |
| Recoloración | `tools/recolor/` | `RecolorTool.java` | `RecolorIcon.java` |
| Rectángulo | `tools/figuras/sin_relleno/rectangulo/` | `RectangleTool.java` | `RectangleIcon.java` |
| Selección rectangular | `tools/rectangle_selection/` | `RectangleSelectionTool.java` | `RectangleSelectionIcon.java` |
| Rectángulo redondeado | `tools/figuras/sin_relleno/rectangulo_redondeado/` | `RoundedRectangleTool.java` | `RoundedRectangleIcon.java` |
| Texto | `tools/text/` | `TextTool.java` | `TextIcon.java` |
| Triángulo | `tools/figuras/sin_relleno/triangulo/` | `TriangleTool.java` | `TriangleIcon.java` |
| Zoom | `tools/zoom/` | `ZoomTool.java` | `ZoomIcon.java` |

## Código compartido

- `tools/shared/DrawingTool.java`: contrato de inicio, movimiento, final, cancelación, trazo y previsualización.
- `tools/shared/Tools.java`: registro de controladores sin estado propio, reutilizados sin asignaciones en cada movimiento; crea los iconos de cada módulo.
- `tools/shared/StrokeSupport.java`: inicio de historial, máscara seleccionada y presión. Pincel, borrador, lápiz, clonación y recoloración implementan su trazo en su propio controlador.
- `tools/shared/ShapeRaster.java`: composición, alfa y selección reutilizados por las formas geométricas.
- `tools/shared/SelectionSupport.java`: máscaras, portapapeles, recorte, selección completa/inversa y dibujo del contorno; `SelectionSnapshot.java` conserva copias independientes para el historial de curvas.
- `tools/shared/NavigationSupport.java`, `NavigationTool.java` y `Viewport.java`: transformación de coordenadas, desplazamiento y zoom utilizados por navegación de uno o dos dedos.
- `tools/shared/native/Raster.cpp`: mezcla de pinceles y muestreo C++ compartidos; evita duplicar algoritmos entre pincel, borrador, clonación y recoloración.

Línea/Curva concentra borrador, geometría, tiradores, historial, previsualización y confirmación en `tools/line_curve/`. Enter confirma; Ctrl+Z reabre una curva confirmada mientras sus metadatos sigan en el caché existente. Escape cancela. Se conservan metadatos de hasta 100 grupos confirmados para recuperar su edición. La geometría cardinal, inserción de nodos y grupos pendientes se describen en `tools/line_curve/README.md`.

## Aplicación y motor

`MainActivity` construye la interfaz y conecta los comandos; `DrawingView` conserva el documento, el estado de interacción por instancia, el ciclo de eventos y la fachada JNI. Los controladores nunca guardan el estado de otro lienzo. Se conserva el paquete Java `art.velyntora.core` para permitir colaboración interna sin convertir el estado del lienzo en API pública ni cambiar símbolos JNI. Gradle agrega `tools/` como raíz de fuentes Java.

En `app/src/main/java/art/velyntora/core/`, `document/` gestiona proyectos y tamaños, `formats/` codecs, `effects/` ajustes y `workspace/` disposición y renderizado del lienzo. `WorkspaceRenderer` conserva el filtro cercano al ampliar y delega las previsualizaciones al módulo activo.

El motor compartido permanece en `core/include`, `core/src`, `core/document` y `core/effects`. CMake enumera explícitamente los archivos de `tools/*/native`: un archivo desaparecido falla durante la configuración. Las pruebas del puente JNI usan las mismas rutas reorganizadas. `reference/pinta-assets/` conserva material de referencia y licencias; los iconos activos se implementan en los módulos de herramientas.

## Mantenimiento

1. Corregir la herramienta en su carpeta. Si el defecto afecta alfa, máscaras, historial, documentos o coordenadas, revisar también el componente compartido responsable.
2. Para una herramienta nueva, agregar controlador e icono y registrarlos en `Tools`; conservar los identificadores existentes. Añadir fuentes nativas explícitas a CMake cuando corresponda.
3. Ejecutar Core tests, Android APK y Android device tests. Las pruebas Android comprueban píxeles ampliados, formas/undo, selección frente a movimiento de píxeles, Enter/Ctrl+Z y los 28 iconos, además de ciclo de vida y formatos.

La interfaz de dos columnas aprobada continúa vigente. Remove AI se integra en `tools/remove_ai/` y Core no añade animación.

## Variantes de pincel

`tools/brush/` conserva una sola herramienta y contiene `classic/`, `antialiased/` y `selector/`. Cada pincel tiene su renderer separado; el selector muestra las variantes y recuerda la última elección. El clásico mantiene su algoritmo original; el suavizado añade cobertura subpíxel en los bordes. Las futuras variantes se agregan como carpetas independientes.

## Selector de figuras

`tools/figuras/` agrupa nueve herramientas sin cambiar sus identificadores ni algoritmos. `sin_relleno/` contiene rectángulo, redondeado, elipse, triángulo y círculo; `con_relleno/` contiene rectángulo, elipse, redondeado y triángulo. Cada figura tiene su propia carpeta. `FigurasModule.java` es el catálogo; `selector/FigurasSelector.java` abre los dos grupos desde el botón Figuras. La barra conserva 21 accesos visibles al reunir las nueve figuras en uno.
