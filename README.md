# Velyntora Animation — editor de animación piloto

Aplicación Android independiente de Zaskar. **Powered By Zaskar Startup**.

- Inicio propio de Animation y creación con nombre, dimensiones y FPS.
- Herramientas y pinceles de Velyntora Core, con capas editables por fotograma.
- Línea de tiempo con miniaturas, creación, duplicación, eliminación y reordenación.
- Exposición de 1 a 120 unidades por fotograma y reproducción en bucle de 1 a 60 FPS.
- Piel de cebolla roja/azul de los fotogramas adyacentes.
- Guardado de sesiones, proyectos recientes y archivos `.vlyanim` con capas.
- Exportación de secuencia PNG a resolución completa con `timing.csv`.

La rama `feature/animation-editor` contiene el piloto 0.3.0. Se valida antes de proponer su incorporación a `main`. El APK de pruebas se publica bajo `animation-preview` cuando termina correctamente GitHub Actions.

## Arquitectura

`app/.../animation/` separa modelo, almacenamiento, sesión, caché, línea de tiempo, reproducción, piel de cebolla y configuración. `engine/` conserva el motor y sus herramientas. Detalles y límites en [docs/animation/README.md](docs/animation/README.md).

## Compilación y validación

JDK 17, Gradle 8.12, SDK 35, CMake 3.22.1. Compilar: `gradle :app:assembleDebug :app:lintDebug :app:assembleDebugAndroidTest`.

GitHub Actions ejecuta las pruebas de modelo/archivo/reloj, las comprobaciones modulares, las pruebas C++ y la integración Android. El modelo MI-GAN se descarga y verifica durante la compilación; se incluye para uso sin conexión.

Audio, interpolación IA, MP4/GIF animado e historial global de la línea de tiempo siguen pendientes. Este es un piloto funcional, no una declaración de aplicación terminada al 100 %.
