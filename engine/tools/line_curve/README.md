# Línea/Curva

Arrastrar crea una recta con dos extremos. Tocar su trayectoria añade un punto en
el segmento correspondiente; arrastrar un punto modifica una curva cardinal que
pasa por todos sus nodos. No se usan los cuatro tiradores de un Bézier aislado.
Los extremos tienen tensión 0 y cada punto añadido 1/3. Mantener pulsado un nodo
abre el ajuste de tensión entre 0 y 1. Cada modificación admite Deshacer/Rehacer.

Arrastrar fuera de las trayectorias añade otra línea al grupo editable. Se puede
volver a seleccionar un nodo de una línea anterior. Enter o Confirmar aplica el
grupo completo; Deshacer (flecha atrás o Ctrl-Z) recupera su geometría editable y
la selección original. Escape cancela el grupo. Cambiar de herramienta confirma.

Referencia estudiada: Pinta-2.0, commit
2cfea9cf1eae94e45e9d0ef98d76d9ccf4a98481,
LineCurveSeriesEngine.GeneratePoints y BaseEditEngine. Se reproduce el cálculo
de tangentes cardinales, incluida la ponderación de nodos intermedios por índice.
Las pulsaciones se proyectan sobre segmentos muestreados cada 0.025.

En Android la pulsación prolongada sustituye el ajuste con botón secundario.
El estilo actual (color, anchura, opacidad) se aplica a todo el grupo pendiente.

## Verificación de este cambio

- Pruebas Java de geometría: aprobadas.
- Pruebas Java de historial, grupos y tensión: aprobadas.
- Compilación Java de DrawingView y todos los controladores contra Android API 35:
  aprobada. No equivale a compilar el APK completo.
- Nueva prueba de dispositivo: inserción, paso por nodo, grupo de dos líneas,
  confirmación y recuperación tras Deshacer. Su ejecución está pendiente.
- Compilación Gradle del APK y ejecución en teléfono/emulador: pendientes.
