# Velyntora Core

Aplicación Android de dibujo con interfaz Java y motor C++, inspirada en Pinta. Proyecto independiente; sin funciones de animación. Este repositorio es el único destino de desarrollo.

El estado verificable del plan de 20 hitos está en [PROGRESS.md](docs/PROGRESS.md). Las diferencias de compatibilidad se detallan en [FORMAT_SUPPORT.md](docs/FORMAT_SUPPORT.md) y [EFFECTS_PARITY.md](docs/EFFECTS_PARITY.md).

## Organización por herramienta

Cada herramienta tiene su controlador Android y su icono vectorial en `tools/<herramienta>/`. El código C++ específico, cuando existe, está en `tools/<herramienta>/native/`. Los componentes reutilizados están en `tools/shared/`. El inventario completo y las reglas de mantenimiento están en [TOOLS_ARCHITECTURE.md](docs/TOOLS_ARCHITECTURE.md); la validación de esta reorganización se registra en [REORGANIZATION.md](docs/REORGANIZATION.md).

## Compilar y probar

Java 17, Gradle 8.12, Android SDK 35 y CMake 3.22.1:

```sh
gradle :app:assembleDebug --no-daemon
```

Pruebas C++ del motor:

```sh
cmake -S tests -B build/tests -DCMAKE_BUILD_TYPE=Debug
cmake --build build/tests --parallel
ctest --test-dir build/tests --output-on-failure
```

Los workflows de GitHub también ejecutan las suites Java, integración JVM/JNI y pruebas de dispositivo Android. Con un emulador o dispositivo conectado:

```sh
gradle :app:connectedDebugAndroidTest --no-daemon
```

El workflow **Android APK** adjunta `velyntora-core-debug-apk`. Es un APK de desarrollo, instalable para la revisión posterior en el teléfono; no es una publicación en una tienda.

## Dibujar y guardar

Dos muestras de color permiten editar e intercambiar primario/secundario. Degradados lineal, radial, reflejado, diamante y cónico; configuración desde Herramientas o pulsación larga del icono.

Herramientas y capas aparecen en columnas en ventanas anchas y como paneles conmutables en teléfono. Dos dedos permiten zoom, desplazamiento y rotación. Archivo permite abrir con detección de formato y exportar imágenes. **Guardar** conserva un proyecto editable `.vlycore`; OpenRaster es la alternativa de intercambio con capas. Los avisos de cambios pendientes permiten guardar, cancelar o descartar; al pasar a segundo plano se escribe recuperación atómica.

Con teclado: Ctrl+N/O/S para nuevo/abrir/guardar proyecto; Ctrl+Z, Ctrl+Shift+Z o Ctrl+Y para historial; Ctrl+A/D y Ctrl+C/X/V para selección y portapapeles. Enter confirma Línea/Curva y Escape cancela su borrador.

Límites de documento y variantes de formatos admitidas: [FORMAT_SUPPORT.md](docs/FORMAT_SUPPORT.md). Dependencias y licencias: [THIRD_PARTY.md](docs/THIRD_PARTY.md).
