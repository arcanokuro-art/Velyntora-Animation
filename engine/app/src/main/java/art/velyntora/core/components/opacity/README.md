# Opacidad de herramientas

ToolOpacity guarda un porcentaje independiente para los 19 IDs que pintan o borran. OpacityControls muestra un control 0–100 % en la barra desplazable, con etiqueta de ancho fijo y arrastre protegido de la intercepción del contenedor. No modifica la opacidad de capa ni el alfa elegido en el selector de color: el alfa efectivo es alfa del color × opacidad de herramienta.

DrawingView mantiene una fachada compatible con brushOpacity y persiste cada porcentaje en preferencias locales. La selección, navegación, cuentagotas y Remove AI no muestran este control. Línea/Curva conserva geometría, confirmación, cancelación e historial; al reabrir un trazo restaura también su porcentaje.

Pincel, lápiz, borrador, clonar y recolorear usan máxima cobertura por gesto sobre los píxeles originales: los eventos solapados no aumentan la intensidad. Un segundo gesto sí puede acumular pintura o borrado. Al 0 % no se editan píxeles ni se añade historial. Los trazos mantienen su rasterizado y tamaño actuales.
