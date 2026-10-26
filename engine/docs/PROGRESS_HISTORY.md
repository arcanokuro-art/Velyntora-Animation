# Registro histórico anterior al cierre

Este archivo conserva los estados y notas previos. Las cifras y pendientes que aparecen aquí son históricos; el estado actual está en [PROGRESS.md](PROGRESS.md).

# Velyntora Core — medición de avance

Objetivo: reproducir la experiencia y las funciones de Pinta 3.1.2 en Android. El porcentaje global se calcula sobre una lista de **20 entregables** de igual peso (5 puntos cada uno). Cada entregable cuenta únicamente cuando está integrado y su compilación/pruebas pasan; una implementación parcial vale 0 en esta métrica. Este listado es una referencia de planificación, no un inventario exhaustivo de comandos individuales de Pinta.

- [x] 01. Proyecto Android con compilación automatizada.
- [x] 02. Motor de lienzo C++ y pruebas básicas.
- [x] 03. Pincel y herramientas geométricas básicas.
- [x] 04. Abrir imágenes y exportar PNG.
- [x] 05. Capas básicas conectadas al lienzo.
- [x] 06. Deshacer/rehacer multicapa básico.
- [x] 07. Panel de capas con miniaturas y opacidad (funcionalidad básica programada).
- [x] 08. Iconos originales de Pinta integrados (siete herramientas básicas; otros recursos visuales pendientes).
- [ ] 09. Diseño adaptable equivalente a Pinta.
- [x] 10. Selecciones rectangulares y elípticas (crear, borrar, copiar, cortar, pegar y mover contenido; sin selección libre).
- [x] 11. Selección libre y varita mágica (máscaras para copiar, borrar y mover; tolerancia configurable y regiones conectadas).
- [x] 12. Mover, recortar y transformar selecciones.
- [x] 13. Herramientas de texto y formas adicionales.
- [x] 14. Pinceles, borrador alfa y configuración avanzada.
- [x] 15. Zoom, panorámica y rotación del lienzo.
- [ ] 16. Gestión completa de tamaños y formatos.
- [x] 17. Ajustes de color de Pinta.
- [x] 18. Efectos y filtros de Pinta (cobertura de las 37 familias; implementaciones y controles Core).
- [x] 19. Guardar y abrir proyectos editables con capas.
- [ ] 20. Pulido de interfaz, accesibilidad y corrección de defectos detectados durante desarrollo.

**Avance verificado por entregables: 17/20 = 85.00%**. El entregable 15 incluye zoom de 1–7000 %, panorámica y rotación con dos dedos, controles del menú Ver y coordenadas inversas para las herramientas. Validado con pruebas Java y compilación Android del commit `e90a779f6c8dc8f6c5cea4a1bf33d7ae634e9507` (Actions 37975450908 y 37975450586). La cifra mide estos hitos y no representa paridad exhaustiva con cada comando de Pinta. Las funcionalidades marcadas pueden tener limitaciones y defectos pendientes.

## Regla del 100.00 %

El 100.00 % se alcanza al terminar **la programación** de los 20 entregables, no al realizar la prueba manual del APK en el teléfono del usuario. La instalación, ejecución y revisión visual por parte del usuario son una **fase posterior e independiente** que no se incluye en el denominador. Las compilaciones y pruebas automatizadas de desarrollo siguen siendo controles de calidad del código.

## Proyectos editables (entregable 19, verificado)

Formato `.vlycore` versión 1: dimensiones, orden y nombres de capas, visibilidad, opacidad, capa activa y píxeles ARGB completos. Lectura/escritura por bloques de 4 KiB en un hilo de trabajo; no se crea una copia completa del archivo. Se valida el formato, dimensiones, cantidad de capas, propiedades y archivo completo antes de sustituir el documento. Un archivo inválido conserva el dibujo y su historial. Límites: 32 capas y 24 millones de píxeles acumulados; tras el bloque de tamaños, abre proyectos con dimensiones variables dentro del límite Android. Undo/Redo comienza vacío al abrir y funciona con las ediciones posteriores. Validado con pruebas C++ y JNI, y compilación Android del commit `c4c80f337bd76350642b2aaedc66ece5eb822620` (Actions 37975859273 y 37975859142).

## Tamaños y formatos (entregable 16, parcial)

Nuevo documento con ancho/alto, tamaño de imagen con remuestreo por píxel cercano, tamaño del lienzo desde la esquina superior izquierda y recorte multicapa a los límites de la selección. Las dimensiones forman parte de Undo/Redo y de los proyectos. Importación sin deformar la relación de aspecto y exportaciones PNG, JPEG sobre blanco y WebP. Límite Android: 4 millones de píxeles por documento, 8192 px por lado y 24 millones acumulados en capas; otros formatos de Pinta siguen pendientes. Este bloque no se cuenta como entregable 16 completo.

## Pincel y borrador (entregable 14, verificado)

Puntas circular/cuadrada; radio de 1–128 px; opacidad y dureza de 0–100 %; presión del stylus sobre el radio. Mezcla alfa source-over y borrador que reduce alfa sin pintar el fondo. El segmento se rasteriza una vez por píxel para no multiplicar artificialmente la opacidad por el solapamiento de dabs. Levantar el dedo sin desplazamiento no aplica tinta otra vez. Pruebas de mezcla, borrador parcial, bordes suaves y punta cuadrada más APK verificados en `ff72e6ad3ae9f4529e95d97e788a03a769aa2689` (Actions 37976717112 y 37976717255). Quedan detalles transversales de selección y cancelación de gestos dentro del pulido del entregable 20.

## Texto y formas (entregable 13, verificado)

Texto multilínea colocado al tocar el lienzo, con tamaño de 4–256 px, negrita/cursiva y familias sans-serif/serif/monospace. Se rasteriza en la capa activa; se puede deshacer, seleccionar y mover, pero no reabrir como objeto tipográfico editable. Rectángulo redondeado y triángulo con variantes de contorno/relleno y alfa. Paleta de herramientas en tres columnas con desplazamiento; capas ocultables para dar espacio al lienzo en teléfonos. Recursos originales de Pinta adaptados como vectores Android para texto, selección libre, varita, movimiento y rectángulo redondeado; icono propio de triángulo.

Validación de texto/formas: compilación Android y pruebas C++/Java/JNI del commit `c42c98c56e1106df744385e0715bec0daef442c2` (Actions 37977246850 y 37977246851). La prueba JNI comprueba transparencia, mezcla de los píxeles insertados y Undo/Redo; el aspecto y la interacción en pantalla quedan para revisión en Android.

## Efectos básicos (familias del entregable 18)

Siete efectos parametrizados sobre la capa activa: desenfoque de caja (premultiplicación de alfa para evitar halos), enfoque, detección de bordes, relieve, pixelado, ruido y viñeta. Se calculan en un hilo de trabajo con un único punto de Undo/Redo. Este fue el primer grupo implementado. Los demás grupos y sus límites actuales se detallan en `EFFECTS_PARITY.md`.

## Correcciones transversales

El pincel/borrador respeta selecciones rectangulares, elípticas, libres y de varita mediante una máscara. El primer toque de un gesto de dos dedos no modifica el documento ni añade un punto al historial; un toque de pintura se aplica al levantar el dedo. Los gestos iniciados después de un trazo ya dibujado conservan ese tramo. El límite acumulado de 24 millones de píxeles se comprueba también al añadir capas.

## Selecciones: mover, recortar y transformar (entregable 12, verificado)

Movimiento por arrastre y desplazamiento numérico, recorte real del documento en todas las capas, escalado horizontal/vertical de 10–400 %, rotación de −180 a +180° y reflejos. Se aplica la máscara de cualquier tipo de selección, conserva los píxeles excluidos y mezcla el alfa sobre el destino. El resultado mantiene una máscara de selección transformada y usa un solo punto de Undo/Redo. Remuestreo por píxel cercano; la parte que salga del documento se recorta, recuperable mediante Deshacer.

Validación del entregable 12: pruebas C++ del remuestreo y máscaras, pruebas de llamadas JNI desde Java con reflejo/Undo/Redo y compilación APK del commit `453f8f1f76ced79c6ebeaddbe9aeb901f732ad6b` (Actions 37978432820 y 37978432930). Se corrigió una publicación incompleta y se verificó el árbol final completo frente a la revisión anterior: ningún archivo eliminado.

**Resumen actual: 17 de 20 entregables verificados = 85.00 %.** Abiertos: 09 (paridad/adaptación completa de interfaz), 16 (todos los formatos), 20 (pulido/accesibilidad/defectos transversales). Los diálogos de parámetros usan desplazamiento para que todos los controles sean accesibles en pantallas pequeñas y con el teclado visible.

## Ajustes de color avanzados (entregable 17, verificado)

Curvas suaves editables con hasta 32 puntos y canal RGB/R/G/B/luminosidad; niveles independientes con entrada, salida y gamma por canal; niveles automáticos con recorte de histogramas; posterización RGB independiente y tono/saturación/luminosidad combinados. Procesamiento fuera del hilo de interfaz, alfa conservado, píxeles totalmente transparentes intactos y un solo paso de Undo/Redo. Los valores inválidos y operaciones sin cambios no alteran el historial. Se incluyen pruebas C++, interpolación Java e integración JNI. Brillo/contraste combinados y blanco y negro, invertir y sepia completan las nueve familias de ajustes base en un menú propio. Validado con C++/Java/JNI y APK del commit `0955b4fec74da61437ef092bbaa3f0b9ce89039d` (Actions 37980592850 y 37980592486). El procesamiento afecta a la capa activa; no incluye previsualización interactiva ni igualdad numérica exacta con Pinta.

## Exportación adicional (entregable 16, parcial)

BMP de 24 bits con fondo blanco y TGA de 32 bits con alfa, escritos fila a fila en un hilo de trabajo. Se comprueban cabeceras, orden de filas, relleno, transparencia y composición mediante pruebas Java. Siguen pendientes formatos adicionales. Importación TGA de 24/32 bits, sin compresión o con RLE, con ambos orígenes y validación de paquetes truncados o fuera de límites antes de sustituir el documento. No se suma el entregable 16.

## Desenfoques adicionales (entregable 18, parcial)

Gaussiano separable con radio 0–32 px, movimiento con distancia y dirección, radial con centro configurable y arco, y zoom con centro e intensidad. Muestreo bilineal y mezcla premultiplicada por alfa para no contaminar bordes con RGB oculto de píxeles transparentes. Cada aplicación se ejecuta fuera de la interfaz y ocupa un solo paso del historial. Pruebas de simetría, constantes, identidad, direcciones, transparencia, parámetros inválidos y llamadas JNI con Undo/Redo. Este grupo está integrado en el catálogo base verificado del entregable 18. Se corrigió también la curva de luminosidad identidad para colores próximos al negro.

## Espacio adaptable (entregable 09, parcial)

A partir de 720 dp, herramientas a la izquierda, lienzo central y capas a la derecha. En teléfonos o ventanas más estrechas, los paneles son superpuestos y se abren desde botones; nunca restan ancho al lienzo, sólo uno queda abierto, y elegir herramienta cierra su panel. Los cambios de orientación/tamaño conservan la misma vista de dibujo, sus ajustes y selección. Política de ancho probada de 240 a 1280 dp. Los colores tienen descripción accesible y superficie táctil de 48 dp; Deshacer/Rehacer tienen etiquetas completas. La paridad visual completa continúa pendiente y no suma el entregable 09.

Validación de formatos BMP/TGA: pruebas Java y compilación Android del commit `35b9d6025eef758f0cbdef494de5ef522b7f8969` (Actions 37981022159 y 37981022107). Validación de los cuatro desenfoques y corrección de curvas: C++/Java/JNI y APK del commit `524e47a3c047f3f4d6336904428c51c515ee5b9d` (Actions 37981283105 y 37981283130).

Pulido adicional: selector de color RGBA completo, controles de curva con entrada/salida numérica además del gráfico, etiquetas habladas en sliders y acciones de capas. Estos cambios no cierran la revisión transversal de accesibilidad del entregable 20.

## OpenRaster (entregable 16, parcial)

Abrir/guardar `.ora` con capas PNG normales, nombres UTF-8, orden, visibilidad, opacidad, desplazamiento de capas y capa activa. Archivo ZIP con `mimetype` primero y sin compresión, `stack.xml` y `mergedimage.png`. La conversión se realiza en un hilo de trabajo, con archivos temporales eliminados al finalizar. La importación se valida antes de reemplazar atómicamente el documento nativo. Grupos y modos de mezcla distintos de normal se rechazan; no se importan con una apariencia incorrecta. Límites de tamaño, número de capas, XML y entradas ZIP; no se extraen rutas ZIP al sistema de archivos. La gestión completa de formatos sigue pendiente: no se suma el entregable 16.

## Distorsiones y selecciones (entregable 18, parcial)

Remolino, abombar/pellizcar, ondas radiales, cristales y escarcha con intensidad, radio/periodo, dirección y centro según el efecto. Interpolación bilineal premultiplicada por alfa; escarcha reproducible. Curvas, ajustes avanzados, los siete efectos iniciales, desenfoques y distorsiones respetan la máscara de selección capturada antes del hilo de trabajo y conservan el contorno. Los niveles automáticos usan sólo el histograma seleccionado. Las operaciones sin cambio no añaden historial; cada aplicación efectiva ocupa un solo paso de Undo/Redo. Los antiguos extras de color conservan su comportamiento sobre toda la capa. Estos avances forman parte del catálogo base verificado del entregable 18.

Validación OpenRaster: pruebas con PNG reales y compilación APK del commit `6a8e771e4e27a91fc458064ce7d327fc89f07178` (Actions 37984360200 y 37984360235). Referencia de formato: https://www.openraster.org/baseline/file-layout-spec.html y https://www.openraster.org/baseline/layer-stack-spec.html . Las pruebas cubren orden, UTF-8, visibilidad, opacidad, capa activa, composición, miniatura y metadatos inválidos; la interacción visual en Android queda para la fase manual.

## TIFF (entregable 16, parcial)

Exportación TIFF RGB de 8 bits por canal con alfa no asociado, sin compresión, en orden de filas superior a inferior y escrita por bloques de fila en el hilo de exportación. Las pruebas verifican IFD, etiquetas, offsets, alfa e ida/vuelta con el decodificador TIFF de Java. Importación TIFF de tiras RGB/gris/paleta de 8 bits, orden II/MM, orientaciones 1–8 y alfa asociado/no asociado. Admite sin compresión, LZW, PackBits y Deflate, además de predictor horizontal. Lectura en hilo de trabajo, archivo máximo de 64 MiB y 4 millones de píxeles; rechaza offsets, tamaños y códigos inválidos antes de modificar el documento. No admite BigTIFF, mosaicos, CMYK, 16 bits ni perfiles ICC; el gris se interpreta directamente como intensidad de 8 bits. Las pruebas usan 20 archivos de un codificador independiente y verifican ambos órdenes de bytes, las ocho orientaciones, alfa y entradas corruptas. Otros formatos siguen pendientes; no se suma el entregable 16. La lectura de `stack.xml` OpenRaster valida UTF-8 y admite BOM.

## Arte y fotografía (entregable 18, parcial)

Pintura al óleo con radio y niveles de intensidad, boceto a lápiz, boceto a tinta con umbral de contorno, resplandor, retrato suave, mediana/percentil y reducción de ojos rojos. Conservan el alfa original y los píxeles totalmente transparentes; el óleo ignora los colores ocultos y pondera por alfa. Procesamiento fuera del hilo de interfaz, máscara de selección y un único punto de historial. Implementaciones propias: no se afirma igualdad numérica con Pinta. Pruebas del motor y llamadas JNI con Undo/Redo y máscara vacía. Este grupo forma parte del catálogo base verificado; avance global 17/20 = 85.00 %.

Validación de los siete efectos de arte/foto: pruebas C++ y JNI con Undo/Redo y APK del commit `37763930b6da60d1f571c45fd15389f57d23a37c` (Actions 37986391469 y 37986391407). Optimización de óleo y mediana mediante ventanas deslizantes; comparación exacta contra la implementación previa sobre dimensiones y radios variados, y prueba de percentiles contra una referencia independiente por ordenación. Medición local orientativa de 512×512, compilación `-O2`: óleo 347→95 ms; mediana 164→57 ms. Estas cifras no corresponden a un dispositivo Android y no forman parte del porcentaje global.

## Generadores (entregable 18, parcial)

Nubes por ruido suave multiescala, Voronoi, celdas, Mandelbrot y Julia. Escala/zoom, octavas/iteraciones y semilla según el generador; primer color de dibujo y segundo blanco/negro/transparente. Interpolación de colores premultiplicada por alfa. Generan contenido nuevo en la capa activa o selección; los píxeles excluidos se conservan y el cambio se deshace en un solo paso. Pruebas de determinismo, semillas, paletas uniformes, transparencia, límites y JNI. Este grupo forma parte del catálogo base verificado; avance 85.00 %.

## Resto del catálogo de filtros y objetos (entregable 18, verificado)

Fragmentar, desenfoque de lente mediante disco muestreado, abolladuras por ruido suave, inversión polar, reducción de ruido con tolerancia de color, contorno de bordes, relieve direccional y tramado ordenado. Objetos: alinear en nueve posiciones dentro del lienzo/selección, suavizar alfa hacia dentro y dibujar un contorno detrás del objeto. La detección de objetos se basa en alfa, por lo que requiere fondo transparente. Distancias euclidianas calculadas en dos pasadas; no se realiza una búsqueda de todos los puntos de borde para cada píxel. Los efectos respetan selecciones y tienen Undo/Redo. Implementaciones propias con parámetros acotados para Android; no se promete reproducción píxel a píxel de Pinta ni sus opciones de previsualización. La integración y el APK de estas familias están verificados.

## Cierre del entregable 18

Cobertura funcional de las 37 familias del catálogo de referencia, con controles, selección e historial, agrupadas en un solo menú Efectos. Correspondencias y diferencias explícitas en [EFFECTS_PARITY.md](EFFECTS_PARITY.md). No se afirma igualdad píxel a píxel ni todos los rangos/métodos/opciones de Pinta. Compilación Android y pruebas C++/Java/JNI aprobadas para `c20cc4296a1f20dc7d5f09bd86dec1fca82baf7b` (Actions 37987595585 y 37987595653). Se suma el hito: 17/20 = **85.00 %**. Quedan 09, 16 y 20.


## GIF y exportación en segundo plano (entregables 16 y 20, parciales)

Exportación GIF89a de una sola imagen: paleta exacta hasta 255 colores visibles, reducción a un cubo RGB de 216 colores al excederla y transparencia binaria (alfa menor de 128 transparente). No exporta animaciones ni conserva semitransparencias. Compresión LZW con diccionario de 9–12 bits, reinicios y bloques de 255 bytes; memoria de trabajo proporcional al ancho de fila. Pruebas con el decodificador independiente de Java: paletas, reducción de colores, umbral alfa, límites del diccionario, reinicios e imagen de 4 millones de píxeles. El mensaje de exportación avisa de la reducción de colores/transparencia.

PNG/JPEG/WebP usan ahora el mismo hilo de exportación que BMP/TGA/TIFF/GIF; compresión, escritura y composición blanca de JPEG ya no se ejecutan en el hilo de interfaz. Se bloquea la edición durante la operación y se liberan las imágenes al terminar. Estos cambios no cierran la gestión completa de formatos ni el pulido transversal; el avance sigue en **17/20 = 85.00 %**.


## ICO (entregable 16, parcial)

Abrir iconos Windows con imágenes PNG embebidas o DIB sin compresión de 1/4/8/24/32 bits. Selecciona la mayor representación compatible y usa profundidad de color como desempate; valida entradas, offsets, paletas, dimensiones y máscaras AND. Admite el alfa de 32 bits y la convención antigua de alfa completamente cero más máscara. Importación fuera del hilo de interfaz y sin reemplazar el documento ante un error.

Guardar ICO de una imagen DIB de 32 bits con semitransparencias y máscara AND. El dibujo se reduce proporcionalmente a un máximo de 256 px por lado, sin modificar el documento. Límites de lectura: 16 MiB y 64 representaciones. No admite CUR, DIB comprimidos ni BI_BITFIELDS. Pruebas Java y lectura independiente con Pillow del archivo exportado. Este avance no suma aún el entregable 16; progreso **85.00 %**.


## Netpbm/PPM (entregable 16, parcial)

Abrir PPM ASCII P3 y binario P6 con comentarios, valores máximos de 1–65535 y canales binarios de 8/16 bits en orden big endian. Convierte a RGB de 8 bits; valida dimensiones, muestras fuera de rango, cabeceras, truncamiento y muestras adicionales. Los bytes de color que coinciden con espacios, saltos de línea o `#` se conservan en P6. Guardar P6 RGB de 8 bits, por filas, componiendo semitransparencias sobre blanco. Operaciones fuera del hilo de interfaz. Pruebas Java e inspección del archivo exportado con Pillow. No admite PBM/PGM/PAM ni secuencias de imágenes; 64 MiB máximo de archivo y los límites habituales del documento.


## Tamaños avanzados e importación sin bloqueo (entregables 16 y 20, parciales)

Tamaño de imagen: elegir píxel cercano o bilineal, con interpolación premultiplicada por alfa para evitar halos de colores ocultos. Tamaño de lienzo: nueve anclajes (esquinas, bordes y centro), tanto al ampliar como al recortar; relleno nuevo transparente. Conserva nombres, opacidad, visibilidad y capa activa. Las dimensiones y los píxeles se publican atómicamente y forman un solo paso de Undo/Redo. Remuestreo y asignación del documento en hilo de trabajo; actualización de selección y vista en el hilo de interfaz. Pruebas C++ de los nueve anclajes, recorte/ampliación, alfa y colores, y pruebas JNI reales de Undo/Redo y rechazo de parámetros inválidos.

Abrir imagen ahora realiza lectura y decodificación Android en segundo plano. La política de reducción usa los límites reales (8192 por lado y 4 millones de píxeles), con cálculo por redondeo hacia arriba y protección de desbordamientos; ya no reduce innecesariamente panoramas que caben en el presupuesto. Pruebas Java de panoramas, dimensiones enormes y muestras mínimas suficientes. El avance global sigue en **85.00 %**; esto no completa todas las variantes de formatos ni el pulido transversal.


## Proporciones, porcentajes y tamaños rápidos (entregable 16, parcial)

Tamaño de imagen permite mantener proporciones, activado inicialmente. Editar ancho recalcula alto y viceversa; el redondeo se hace al píxel más próximo. Al activar el bloqueo toma la proporción de las dimensiones actuales del diálogo. Escala uniforme por porcentaje respecto del documento original, admite decimales con punto o coma; al escribir medidas individuales el campo de porcentaje queda vacío para evitar mostrar una escala que ya no representa ambos ejes. La validación final vuelve a calcular las medidas desde el último campo editado: entradas parciales o fuera de rango no aplican dimensiones antiguas por accidente.

Nuevo documento añade cinco tamaños rápidos: 512×512, 1024×1024, 1920×1080, 1080×1920 y 2400×1000. Mantiene medidas personalizadas en píxeles. Todos respetan los límites actuales. Pruebas Java de proporciones, redondeo, escala, presupuesto de píxeles, valores no finitos y desbordamientos. No añade metadatos de impresión/DPI ni cierra la cobertura completa de formatos; avance **85.00 %**.


## Estado activo y ventanas bajas (entregables 09 y 20, parciales)

Las 20 herramientas ya tenían iconos conectados. Ahora la activa muestra fondo y borde propios, estado accesible de selección y descripción «Activa» en Android 11 o superior. Las formas rellenas usan una marca gráfica de tamaño fijo, que no se recorta al aumentar la fuente. Etiquetas de radio y opacidad, acciones de al menos 48 dp y etiqueta de varita sin afirmar color exacto cuando se configura tolerancia.

Color activo visible sobre tablero de transparencia, actualizado desde la paleta, selector RGBA o cuentagotas; descripción accesible de los cuatro canales. En ventanas inferiores a 480 dp de altura se ocultan la fila de accesos rápidos y la paleta para dejar más espacio al lienzo; sus operaciones siguen disponibles en Archivo/Editar, Ver (paneles) y Herramientas (color). El panel de capas crece con fuentes grandes, hasta 320 dp, y el lienzo conserva al menos 240 dp para activar columnas. Cambios de orientación, tamaño y escala de fuente conservan la vista de dibujo. Pruebas Java de anchos, alturas y escalas de fuente; sin afirmar revisión visual completa en dispositivos. Estos cambios no suman los hitos 09/20; avance **85.00 %**.


## Herramientas de la lista del usuario (implementación parcial)

Lápiz dedicado de un píxel: trazo alineado a centros de píxel, borde duro, sin presión ni suavizado; respeta alfa del color, selección e historial del pincel. Desplazamiento con un dedo y zoom como herramientas del panel: toque acerca, toque largo aleja y arrastre vertical cambia escala alrededor del punto inicial. Navegar no edita píxeles ni añade historial; conserva los gestos existentes de dos dedos. Iconos derivados de los recursos SVG originales de Pinta ya almacenados en el repositorio. Pruebas Java de navegación anclada. Siguen pendientes degradado, forma libre de dibujo, tampón de clonar, recoloración, curvas editables, mover contornos de lazo/varita y restricción circular. Avance global **85.00 %**.

## Degradado lineal y movimiento de máscaras (parcial)

Degradado del color activo a transparente por arrastre, aplicado sólo a la selección si existe y con una operación de historial. Extremos con alfa gradual y sin RGB visible al llegar a transparencia total. Incluye modos lineal y radial, seleccionables manteniendo pulsada la herramienta. No incluye segundo color ni edición posterior de tiradores. El movimiento del contorno de lazo y varita conserva y traslada su máscara en píxeles enteros; cancelar restaura la posición. Estos cambios no cierran los entregables abiertos: avance 17/20 = 85.00 %. Las pruebas de degradado forman parte de CI.

## Formas pendientes (parcial)

Forma libre: arrastre para construir un contorno cerrado rasterizado en la capa activa, respetando la selección y con un solo paso de historial. Vista previa durante el arrastre; cancelar no escribe píxeles. Círculo: variante de la elipse que impone ancho y alto iguales. Quedan Línea/Curva editable, Tampón de clonar, Recoloración y variantes avanzadas del degradado. Las funciones nuevas siguen pendientes de compilación/verificación del cambio completo; avance global 85.00 %.


## Verificación local del bloque de herramientas

Se corrigieron declaraciones duplicadas de Forma libre causadas por cambios simultáneos y se confirmó una única variable `action` en la navegación. Compiladas y ejecutadas 15 suites Java y 15 suites C++ del árbol local. Las pruebas de degradado cubren también el modo radial y rechazan extremos numéricos que desbordan la longitud. Esto no acredita la compilación Android ni la interacción visual: SDK/Gradle no disponibles en el entorno y publicación bloqueada por revisión automática. Cambios conservados en la rama local `codex/pending-drawing-tools`; no se contabiliza un hito nuevo. Avance 17/20 = 85.00 %.


## Tampón de clonar y Recoloración (validación en curso)

Tampón de clonar sobre la capa activa: primer toque fija origen; seleccionar otra vez la herramienta permite cambiarlo. Cada trazo toma una copia estable de la capa y mantiene el desplazamiento origen/destino dentro del trazo. Fuera de la imagen no se pinta. Recoloración sustituye RGB del color inicial dentro de la tolerancia compartida con varita, conserva alfa y omite píxeles transparentes. Ambas usan radio, dureza, opacidad, presión y máscara de selección; un paso de historial por trazo. Iconos provisionales compartidos con pincel/cubeta. No equivalen aún a todas las opciones de Pinta. Pruebas C++ de solapamiento, selección, alfa, tolerancia y entradas inválidas; pruebas JNI de Undo/Redo añadidas a CI. Avance 17/20 = 85.00 %.


## Línea/Curva editable (integración pendiente de CI)

Una herramienta conserva un borrador cúbico con dos extremos y dos tiradores interiores. Arrastrar un tirador modifica la curva sin escribir píxeles. Enter o mantener pulsada la herramienta confirma; Escape cancela. Cambiar herramienta, capa o iniciar una operación que bloquea la vista confirma el borrador. Al deshacer una confirmación se recupera el borrador si su geometría sigue en la caché de 15 curvas; Rehacer elimina el borrador y recupera los píxeles. Marcas de historial JNI evitan confundir una curva con otras ediciones. La rasterización respeta la selección, radio, opacidad y alfa. No se afirma paridad completa con todos los modos y transiciones de Pinta; edición del borrador no tiene historial por tirador y la selección se descarta al deshacer. Pruebas Java de geometría y JNI de identidad de historial. Avance 17/20 = 85.00 %.


## Historial de tiradores y selección de curvas (validación pendiente)

Cada arrastre completado de un tirador tiene Deshacer/Rehacer del borrador, separado del historial de píxeles y limitado a 100 movimientos. Arrastres sin cambios no añaden historial; una nueva edición elimina la rama de Rehacer. Deshacer al inicio del borrador lo retira y Rehacer lo recupera; Escape descarta su historial. Ctrl+Shift+Z ejecuta Rehacer. Al recuperar una curva confirmada se restauran la selección original (tipo, contorno y región), color, opacidad y radio. La caché mantiene 15 geometrías confirmadas; su reapertura inicia un historial de tiradores vacío. No se contabiliza otro hito: avance 17/20 = 85.00 %.


## Netpbm ampliado (hito 16 parcial)

Abrir PBM P1/P4 (blanco/negro) y PGM P2/P5 (escala de grises), además de PPM P3/P6. PBM binario conserva el relleno independiente de cada fila; PGM admite muestras de 8/16 bits con máximo hasta 65535. Rechaza muestras fuera de rango, datos truncados y adicionales. Exportación sigue siendo PPM RGB. No incluye PAM ni secuencias; avance permanece 17/20 = 85.00 %.


## Limpieza al cambiar documento y estado visual de herramientas

Crear o abrir otro documento elimina borradores de curvas/formas, caché de curvas, origen de clonación y estados de arrastre/navegación. El motor libera la copia de origen de clonación al reiniciar el historial. La herramienta activa del panel y su descripción accesible se sincronizan también después de Deshacer/Rehacer que cambia a Línea/Curva. Clonación y Recoloración usan ahora vectores adaptados de sus recursos originales de Pinta almacenados en el repositorio. Prueba JNI de reinicio de marcas y ausencia de historial tras cargar documento. No se da por concluido el pulido completo; avance 17/20 = 85.00 %.
