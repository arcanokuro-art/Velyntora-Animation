# Migración modular aplicada

Esta migración organiza el código existente de Velyntora-Core. No añade el motor de animación ni modifica el repositorio Velyntora. No cambia `tools/`.

| Dominio | Propietario de la implementación |
|---|---|
| Ventana y disposición | `app/workspace/WorkspaceComposition` y `WorkspaceLayout` |
| Colores | `components/color`: panel, picker, palette y modelo `ColorPair` |
| Capas | `components/layers`: `LayersPanel` y `LayerOperations` |
| Historial | `components/history/HistoryActions` y JNI `history/` |
| Selección | `components/selection`: máscaras, portapapeles y acciones |
| Barra superior y herramientas | `components/toolbar`: `CommandBar` y `ToolControls` |
| Estado | `components/statusbar/StatusBar` |
| Menús | `components/menus`, con un archivo por menú |
| Texto | `components/text/TextDialog` |
| Ajustes y efectos | `components/effects` y `engines/drawing/CanvasEffects` |
| Documento | `document/model`, `session`, `dimensions`, `project`, `openraster`, `storage` |
| Codecs | `formats/<formato>/`, con políticas comunes en `formats/shared` |
| Entrada | `input/CanvasInput` |
| Renderizado | `engines/rendering/CanvasRendering` y `WorkspaceRenderer` |
| UI común | `shared/ui/WidgetFactory` e iconos de la ventana |
| JNI | `app/src/main/cpp`: session, document, layers, selection, history, drawing, effects, rendering |

## Contratos y compatibilidad

`MainActivity` queda como fachada de composición, ciclo de vida y API de comunicación. Sus campos son privados: los componentes usan métodos explícitos de lectura, actualización y acciones. `DrawingView` conserva su nombre, declaraciones JNI, campos y adaptadores utilizados por las herramientas congeladas. Las implementaciones de documentos, capas, efectos, selección, renderizado y entrada se delegan a componentes.

Los paquetes Java anteriores se mantienen, excepto el modelo puro `ColorPair`, que conserva el paquete del PR #5. Cambiar todos los paquetes o la clase que declara los métodos nativos exigiría modificar los contratos de las herramientas. La ubicación física identifica al propietario; no representa un módulo Gradle separado ni un proceso independiente.

Los archivos JNI `.inc` se componen en una sola unidad C++ para conservar una única sesión y las firmas existentes. Separar archivos no duplica motores ni crea sesiones adicionales.

El modelo `ColorPair` del PR #5 se integra conservando las correcciones más recientes de `main`; no se reemplaza la Activity por la versión antigua de esa rama.

## Verificación reproducible

1. `python scripts/check_modular_boundaries.py`: compara 79 fuentes/recursos de herramientas y 89 exportaciones JNI con su contenido anterior a la migración; detecta acceso directo a campos de la fachada y documentación faltante.
2. Suite JVM de codecs, documentos, geometría, viewport, historial de curvas y modelo de colores.
3. Suite C++ del motor y pruebas del puente JNI con JVM.
4. Compilación completa del APK Android.
5. Pruebas de dispositivo con fuentes 1.0 y 1.8: gesto del pincel, controles, capas, rotación, deshacer, recuperación y sincronización de colores.

Las comprobaciones locales usan stubs solamente para recursos generados y AndroidSVG. La compilación Gradle en CI verifica esas dependencias reales. Una comprobación local no sustituye al APK ni a las pruebas del emulador.

## Alcance del resultado

Se separaron las responsabilidades existentes; los algoritmos y contratos protegidos se conservaron. Compilar y pasar pruebas no garantiza ausencia absoluta de errores en todos los dispositivos. No se asigna un porcentaje global a funciones futuras de Velyntora. El estado de validación final está en los resultados de GitHub Actions del commit integrado.
