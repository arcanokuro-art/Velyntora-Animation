# Módulos de Animation

- `frames/FrameSequence`: identidades y orden; no conoce Android ni almacena píxeles.
- `project/AnimationProject`: metadatos y archivos ZIP; preserva documentos nativos por fotograma. Solo admite nombres de entrada controlados y limita los bytes descomprimidos.
- `playback/PlaybackClock`: calcula el fotograma a partir de tiempo monotónico y FPS, sin acumular retraso de temporizadores.
- `timeline/TimelinePanel`: controles Android; delega las operaciones al adaptador.
- `art/velyntora/core/AnimationActivity`: conecta los módulos con el editor ya existente y su ciclo de vida.
- `art/velyntora/core/OnionSkin`: dibuja las vistas de los fotogramas adyacentes con la transformación del lienzo; no altera los píxeles ni se exporta.

El motor se conserva desde Velyntora Drawing `d0a4b2b47566506d1789927d26725d38a7931edd`. Sus únicos puntos de extensión para Animation son una actividad extensible, la entrada de navegación y una superposición de lienzo opcional. Los pinceles y las implementaciones JNI protegidas permanecen intactos.

La ilustración inicial procede de Velyntora `2924d7744ddabed5f276ea5973bf070efc2135dd`.

## Formato piloto

`.vlyanim` es un ZIP con `project.meta`, `<uuid>.vlycore` y `<uuid>.png`. El archivo de metadatos usa DataOutputStream: cadena UTF `VLYANIM1`, enteros FPS, selección y número de fotogramas, seguidos por las identidades UTF en orden. El documento nativo conserva capas; PNG es una vista compuesta para reproducción/exportación. El formato todavía puede evolucionar y no contiene audio.

La exportación PNG genera `frame-00001.png`, etc., y `timing.txt` con los FPS. El proyecto se guarda automáticamente en una sesión local al cambiar de fotograma o pausar la actividad. “Guardar proyecto” permite llevar una copia fuera de la aplicación. Desinstalar o borrar datos de Android elimina las sesiones locales.
