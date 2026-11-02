# Línea de tiempo y estabilidad

La tira se virtualiza mediante una ventana explícita de scroll y anchura visible, con una celda de margen por extremo. No usa Canvas.getClipBounds para decidir qué archivos cargar: Android puede grabar un display list del hijo completo aunque el padre solo muestre una pequeña región.

Las miniaturas visibles conservan una referencia al bitmap mientras permanecen en la ventana. Los repintados del editor no borran esa imagen. La caché de miniaturas (8 MiB) es independiente de la caché de reproducción/cebolla (24 MiB); el máximo de solicitudes pendientes es 64. El desplazamiento durante reproducción solo cambia al salir el indicador de la ventana, sin reiniciar una animación de scroll en cada fotograma.

La reproducción usa el reloj monotónico y cuatro fotogramas de precarga como máximo. Arranca el reloj después de cargar la primera imagen y conserva el último bitmap válido mientras espera una lectura. No modifica ni carga documentos nativos durante reproducción. El guardado automático se difiere mientras se reproduce.

TimelineStabilityTest reproduce la grabación de un hijo completo con 3233 fotogramas, limita las solicitudes a la ventana y verifica que las miniaturas sobrevivan a repintados repetidos. PlaybackStabilityTest reproduce un proyecto de 3233 entradas a 30 FPS con PNG de 1280×720, comprueba avance, intervalos entre imágenes y ausencia de callbacks tras detenerse. Estas pruebas no sustituyen la validación en el dispositivo del usuario ni garantizan 30 FPS en cualquier resolución.
