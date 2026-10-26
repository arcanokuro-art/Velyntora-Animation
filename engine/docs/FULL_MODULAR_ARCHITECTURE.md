# Arquitectura modular integral de Velyntora-Core

La estructura propuesta se ha aplicado al código existente. El inventario de propietarios, límites de compatibilidad y comprobaciones está en [MODULAR_MIGRATION.md](MODULAR_MIGRATION.md).

Cada elemento tiene su carpeta de implementación y documentación. Los componentes Android están bajo `app/src/main/java/art/velyntora/core/`; los codecs se agrupan por formato; los puentes nativos están bajo `app/src/main/cpp/`. `core/` conserva sus módulos de documento y efectos y el motor compartido. `tools/` queda intacto.

La ventana y el lienzo son fachadas compatibles con Android y las herramientas; no concentran los algoritmos extraídos. Los componentes solicitan acciones mediante la API de composición y no acceden directamente a sus campos. No deben duplicar estado de documento, historial o motor. El modelo de colores pertenece a su componente.

Cada nuevo cambio debe ejecutar las verificaciones de límites y las pruebas relevantes. Un cambio compartido requiere pruebas de integración. El código de Línea/Curva no se modifica.

La animación sigue fuera del alcance de esta migración. Este repositorio organiza el programa existente de dibujo y podrá servir como referencia para Velyntora sin modificarlo aquí.
