# Interfaz oscura basada en la referencia de Pinta

La solicitud del 9 de octubre cancela el diseño anterior de tres columnas. La interfaz usa dos columnas de herramientas, botones de iconos, opciones de pincel arriba, capas a la derecha y paleta compacta de dos filas. Los menús completos permanecen en el botón Menú principal. En ventanas pequeñas los paneles se despliegan desde los botones Herramientas y Capas.

El lienzo utiliza muestreo por vecino más cercano al ampliar (zoom >= 100 %) y filtrado al reducir. Esto evita mezclar colores de píxeles vecinos durante el zoom, sin modificar los píxeles guardados. Las curvas, los tiradores y otras guías conservan su suavizado independiente.

Esta revisión visual es una ampliación del plan de funciones ya completado. Su aceptación visual se comprueba separadamente; no equivale a una copia exacta de todas las funciones o controles de Pinta.
