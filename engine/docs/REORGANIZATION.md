# Reorganización por herramientas — 10 de octubre de 2026

Esta ampliación tiene un denominador independiente del plan anterior de 20 hitos. Ocho entregables de igual peso, 12.5 puntos cada uno. Sólo se contabilizan entregables completos y comprobados.

**8/8 — 100.00 % del proceso de reorganización y validación.**

- [x] Inventario de 28 herramientas e identificadores preservados.
- [x] Controladores, previsualizaciones e iconos propios en una carpeta por herramienta.
- [x] Código nativo específico junto al módulo y servicios compartidos organizados.
- [x] Gradle, CMake y workflows adaptados a las nuevas rutas.
- [x] Suites Java, 16 suites C++ y las dos integraciones JNI aprobadas localmente.
- [x] APK Android y APK de instrumentación compilados localmente.
- [x] Pruebas de dispositivo Android API 29 con fuentes 1.0 y 1.8 aprobadas.
- [x] Cambios publicados y comprobaciones finales de Actions aprobadas.

La reorganización conserva el comportamiento existente y añade regresiones para forma/undo, movimiento de selección/píxeles, confirmación de Línea/Curva y renderizado de todos los iconos. No representa paridad exhaustiva con Pinta ni sustituye la revisión manual del APK en el teléfono.

## Evidencia de cierre

Código verificado: `47dd222398d6ca1df7b21e22df22c322277ac0a2`, publicado en `main`.

| Comprobación | Resultado | Ejecución |
|---|---|---|
| 23 suites Java, 16 suites C++ e integración nativa/JVM JNI | Aprobadas localmente y en GitHub | [Core tests](https://github.com/arcanokuro-art/Velyntora-Core/actions/runs/38033470878) |
| APK Android | Compilación aprobada; artefacto `velyntora-core-debug-apk` | [Android APK](https://github.com/arcanokuro-art/Velyntora-Core/actions/runs/38033470882) |
| Android API 29, fuente 1.0 | 13 pruebas aprobadas, cero omitidas y cero fallidas | [Android device tests](https://github.com/arcanokuro-art/Velyntora-Core/actions/runs/38033470874) |
| Android API 29, fuente 1.8 | 13 pruebas aprobadas, cero omitidas y cero fallidas | [Android device tests](https://github.com/arcanokuro-art/Velyntora-Core/actions/runs/38033470874) |

La actualización final de este registro sólo modifica documentación; las fuentes ejecutadas son las del commit indicado arriba. No se publica una nueva release con esta reorganización. El APK comprobado está en el artefacto de la ejecución enlazada.
