# Velyntora Animation

Aplicación de animación independiente de la familia Velyntora. **Powered By Zaskar Startup**.

Versión piloto Android 0.1.0. El desarrollo se realiza en `Android-AN` y se propone a `main` mediante pull request.

## Base implementada

- Inicio con la ilustración original de Animation, marca transparente y modo inmersivo.
- Motor modular integrado de Velyntora Drawing/Core: herramientas, pinceles, capas, colores y formatos existentes.
- Secuencia de fotogramas con creación, duplicación, eliminación, reordenación y selección.
- Cada fotograma mantiene su proyecto nativo con capas; las vistas PNG se usan para reproducción.
- Reproducción en bucle de 1 a 60 FPS y piel de cebolla de los fotogramas vecinos.
- Proyectos `.vlyanim` (ZIP con metadatos y documentos nativos) y exportación de secuencia PNG con FPS.
- Sesiones locales recuperables y selector de documentos Android para abrir y guardar.

## Estructura

`app/.../ui` contiene el inicio; `app/.../animation/{frames,project,playback}` contiene el modelo, almacenamiento y reloj; `app/.../art/velyntora/core` adapta el editor a Animation. `engine` conserva una sola copia del motor integrado, sus herramientas y código nativo. No se modifica el repositorio de origen.

## Validación

GitHub Actions ejecuta las pruebas Java de Animation, las comprobaciones modulares, las pruebas C++, la compilación/lint de Android y pruebas instrumentadas. El APK piloto se adjunta al workflow; su disponibilidad depende de que la compilación termine correctamente.

## Pendiente

Audio, interpolación vectorial/IA (RIFE/Auto Inbetween), exportación MP4/GIF y adaptación a Windows/Linux. La reproducción utiliza PNG desde disco; aún no existe precarga avanzada. La captura y las operaciones de archivo son síncronas en este piloto y pueden demorar con lienzos grandes. No se declara equivalencia funcional completa ni validación en dispositivos físicos.

Base: `arcanokuro-art/Velyntora-Drawing` (main); ilustración: `arcanokuro-art/Velyntora`. Se conservan los avisos y licencias de los componentes reutilizados.
