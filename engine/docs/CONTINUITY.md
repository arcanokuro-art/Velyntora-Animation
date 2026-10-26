# Alcance y continuidad del desarrollo

Fuente revisada: conversación compartida https://chatgpt.com/share/6ac933b5-76ec-83e8-9de4-11ffd736be8f («Continuar programación real»).

## Decisiones del usuario

- Programar únicamente `arcanokuro-art/Velyntora-Core`.
- Crear una aplicación Android de dibujo equivalente a Pinta, como proyecto independiente antes de trasladar sus funciones a Velyntora.
- Interfaz Android en Java y motor en C++; otros sistemas quedan para el futuro.
- No introducir animación en Velyntora Core.
- Pinta-2.0 se consulta como referencia; no se modifica.
- Conservar los recursos originales de Pinta en `reference/pinta-assets`; vincular los necesarios a la interfaz Android.
- Remove se retoma por autorización del usuario el 10 de octubre: MI-GAN 512 local Android en `tools/remove_ai/`.
- Continuar implementación, revisión y correcciones sin confirmaciones entre etapas.
- Publicar porcentaje basado en entregables verificados. El 100 % significa terminar la programación; la prueba manual posterior en el teléfono no se incluye, pero sí las compilaciones y pruebas de desarrollo.

## Cómo continuar

El plan de 20 hitos queda cerrado en `PROGRESS.md` con la integración del PR #3. No aumentar ni reinterpretar el porcentaje por cantidad de commits. Las diferencias de formatos y efectos permanecen en `FORMAT_SUPPORT.md` y `EFFECTS_PARITY.md`; el 100 % del plan no equivale a paridad exhaustiva con cada comando o variante de Pinta. La revisión del APK en el teléfono es la siguiente fase independiente. Nuevas ampliaciones requieren un inventario propio y no deben alterar retroactivamente este denominador.

Comprobar las ejecuciones Android APK y Core tests del SHA publicado. Core tests incluye pruebas de píxeles, capas, proyectos, redimensionado, pinceles y efectos, coordenadas Java y pruebas del puente JNI con llamadas desde una JVM. Mantener el documento intacto al rechazar archivos o entradas inválidas.

## Organización actual — 10 de octubre de 2026

La petición de reorganizar el código por herramienta queda integrada y verificada en `main`; ver `REORGANIZATION.md`. El inventario de 28 módulos y las reglas de mantenimiento están en `TOOLS_ARCHITECTURE.md`.

- Código Android e iconos activos: `tools/<herramienta>/`.
- Código C++ específico: `tools/<herramienta>/native/`.
- Componentes compartidos: `tools/shared/`; codecs, documentos, ajustes y espacio de trabajo en sus carpetas de `app/src/main/java/art/velyntora/core/`.
- Conservar el paquete Java y los identificadores estables; `DrawingView` mantiene el estado de interacción y la fachada JNI, y `MainActivity` la interfaz.
- Interfaz aprobada de dos columnas y zoom nítido desde 100 % conservados. Remove AI es una ampliación independiente autorizada; no modifica Línea/Curva.
- Las 23 suites Java, 16 C++, las dos integraciones JNI, el APK y 13 pruebas Android repetidas con fuentes 1.0 y 1.8 están aprobados. La revisión manual en el teléfono sigue como fase independiente.
