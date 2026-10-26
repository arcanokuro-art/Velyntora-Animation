# Velyntora Brush

Colección unificada de 36 pinceles: clásico y suavizado en `clasico/`, más 34 presets distribuidos en nueve categorías. Los presets importados provienen de las colecciones Krita 3 y Krita 4. No se incorporan recursos de HiPaint. Los dos renderizadores históricos de Velyntora permanecen separados.

Cada categoría contiene una carpeta por pincel: `source.kpp` original íntegro (PNG con parámetros XML), `preset.json` de adaptación, `tip.bin`, `texture.bin`, miniatura y documentación. `catalog.json` registra identidad estable y clasificación; `runtime/` carga los datos y dibuja sin depender de Qt. Anchura y opacidad siguen en los controles de la herramienta; seleccionar un preset no cambia su anchura arbitrariamente.

El adaptador implementa estampado de máscaras con interpolación, espaciado continuo independiente del muestreo táctil, presión de tamaño/opacidad/flujo, ángulo de punta, densidad, textura, aerógrafo temporizado y arrastre de pigmento de la capa activa. Las máscaras de pinceles animados conservan hasta 32 fotogramas de 128×128 y se recorren de forma determinista. Memoria de trabajo acotada por la punta y los bloques tocados; no crea otra imagen del tamaño del lienzo.

Es una **adaptación a Velyntora**, no el motor completo de Krita ni una promesa de equivalencia visual. Las curvas de presión se interpolan linealmente; la textura se normaliza a 128×128; no se implementan todavía todos los sensores de inclinación, modos de textura, física húmeda, selección aleatoria/direccional de GIH ni composición avanzada del preset. Los archivos KPP originales se conservan para ampliaciones futuras. La categoría Crayones y pastel usa los presets Pastel originales de Krita 3; no se inventan presets de cera que esa colección no tiene.

Selector: categorías como figuras; clásico y suavizado accesibles directamente para conservar la interacción previa. Un gesto genera una sola entrada de historial. El temporizador del aerógrafo se detiene al soltar, cancelar, cambiar de herramienta o cerrar la edición.

## Procedencia y licencia

Fuente de solo lectura: https://github.com/arcanokuro-art/Velyntora-Z/tree/main/krita/data/bundles

- Krita_3_Default_Resources.bundle, SHA-256 registrado en `provenance.json`. Autores según meta.xml: Timothée Giet, Ramon Miranda, David Revoy y otros. Licencia declarada CC0.
- Krita_4_Default_Resources.bundle. Autores según meta.xml: Deevad, con derivaciones de Ramon Miranda, Razvanc, Radian, Wolthera, Storm, Scottyp y otros. Licencia declarada CC0.

`source.kpp` tiene SHA-256 propio en cada preset. `scripts/import_velyntora_brushes.py` reproduce la extracción a partir de ambos bundles locales verificados; no escribe en el repositorio de referencia. Código nuevo del adaptador bajo la licencia del repositorio.
