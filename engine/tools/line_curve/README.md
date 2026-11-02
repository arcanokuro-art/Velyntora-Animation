# Línea/Curva

Arrastrar crea una recta; tocar su trayectoria añade un punto suave. Arrastrar
los puntos modifica una curva que pasa por todos sus nodos. Las curvas conservan el
cálculo original de tangentes de Pinta, incluida la ponderación por índice.
Los puntos añadidos usan tensión 1/3; los extremos usan tensión 0.

Tocar un punto interior existente abre su menú: **Convertir en esquina** crea
una unión angular, **Convertir en curva** recupera el suavizado y **Borrar punto**
elimina el nodo. Las esquinas aparecen como cuadrados y los puntos suaves como
círculos. Una pulsación prolongada conserva el ajuste de tensión entre 0 y 1;
la tensión cero corresponde a esquina. Cada cambio admite Deshacer/Rehacer.

Arrastrar fuera del trazo añade otra línea al grupo editable. Se puede volver a
editar una línea anterior. Enter o ✓ confirma el grupo; Escape o ✕ lo cancela.
Deshacer después de confirmar recupera la geometría editable, incluidos los tipos
de punto. El color, anchura y opacidad se aplican al grupo pendiente.

La previsualización, confirmación y detección del trazo comparten las mismas
tangentes. Las pulsaciones se proyectan sobre segmentos muestreados cada 0.025.
