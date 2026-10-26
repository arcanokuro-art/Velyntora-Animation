# Acciones de capas

Operaciones del documento y actualización del lienzo. Los botones se componen en panel.

`LayerWorkQueue` serializa las acciones del panel mediante el bloqueo de operación del documento. El motor y la preparación de texturas trabajan fuera de la interfaz; ésta instala el resultado terminado. Seleccionar o añadir una capa transparente conserva la composición visible sin repetir su rasterizado. Las API síncronas de `LayerOperations` permanecen para los consumidores existentes.
