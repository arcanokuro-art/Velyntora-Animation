# Línea/Curva

Arrastrar crea una recta; tocar su trayectoria añade un punto suave. Arrastrar
los puntos modifica una curva que pasa por todos sus nodos. El suavizado de los
nodos intermedios es uniforme, independiente de su posición en el trazo, y más
redondeado. Los puntos añadidos usan tensión 0.5; los extremos usan 1/3.

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
