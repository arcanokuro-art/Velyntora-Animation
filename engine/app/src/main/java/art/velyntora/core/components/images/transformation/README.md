# Image placement: transformation

Owns this part of the experimental image-layer editor. Existing drawing tools are independent.

Las imágenes y el texto comparten guías de arrastre. Cada eje encaja el borde o
centro del objeto con el borde o centro del lienzo más cercano dentro de 8 dp.
El arrastre conserva su posición libre antes de aplicar el encaje, por lo que
seguir moviendo el puntero libera la guía sin bloquearlo. Para objetos girados
se usan los límites de su marco. Solo el contenido se recorta en la vista;
los tiradores siguen visibles fuera del lienzo mientras se edita.

Los tiradores de tamaño también encajan con los bordes y centros del lienzo.
La esquina o el centro del lado opuesto permanece fijo. Al mantener proporciones,
el ajuste usa una escala uniforme y solo muestra las guías que coinciden.
