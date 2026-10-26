# Catálogo de efectos: correspondencias verificables

Referencia de inventario: `Pinta.Effects/Effects` de [Pinta-2.0](https://github.com/arcanokuro-art/Pinta-2.0/tree/main/Pinta.Effects/Effects), consultada el 9 de octubre de 2026. Cada fila identifica una implementación propia de Velyntora Core accesible desde **Efectos**. La cobertura de familias no significa igualdad de algoritmos, píxeles, rangos ni todos los controles de Pinta.

| Referencia | Categoría / control Core | Motor e identificador |
|---|---|---|
| AddNoiseEffect | Básicos / Ruido | PixelEffects 5 |
| AlignObjectEffect | Objetos / Alinear | ObjectEffects 0 |
| BulgeEffect | Distorsiones / Abombar-pellizcar | DistortionEffects 1 |
| CellsEffect | Generadores / Celdas | RenderEffects 2 |
| CloudsEffect | Generadores / Nubes | RenderEffects 0 |
| DentsEffect | Más filtros / Abolladuras | UtilityEffects 2 |
| DitheringEffect | Más filtros / Tramado ordenado | UtilityEffects 7 |
| EdgeDetectEffect | Básicos / Detectar bordes | PixelEffects 2 |
| EmbossEffect | Básicos / Repujado | PixelEffects 3 |
| FeatherEffect | Objetos / Suavizar borde alfa | ObjectEffects 1 |
| FragmentEffect | Más filtros / Fragmentar | UtilityEffects 0 |
| FrostedGlassEffect | Distorsiones / Escarcha | DistortionEffects 4 |
| GaussianBlurEffect | Desenfoques / Gaussiano | BlurEffects 0 |
| GlowEffect | Arte y fotografía / Resplandor | ArtisticEffects 3 |
| InkSketchEffect | Arte y fotografía / Boceto a tinta | ArtisticEffects 2 |
| JuliaFractalEffect | Generadores / Julia | RenderEffects 4 |
| MandelbrotFractalEffect | Generadores / Mandelbrot | RenderEffects 3 |
| MedianEffect | Arte y fotografía / Mediana-percentil | ArtisticEffects 5 |
| MotionBlurEffect | Desenfoques / Movimiento | BlurEffects 1 |
| OilPaintingEffect | Arte y fotografía / Pintura al óleo | ArtisticEffects 0 |
| OutlineEdgeEffect | Más filtros / Contorno de bordes | UtilityEffects 5 |
| OutlineObjectEffect | Objetos / Contorno de objeto | ObjectEffects 2 |
| PencilSketchEffect | Arte y fotografía / Boceto a lápiz | ArtisticEffects 1 |
| PixelateEffect | Básicos / Pixelar | PixelEffects 4 |
| PolarInversionEffect | Más filtros / Inversión polar | UtilityEffects 3 |
| RadialBlurEffect | Desenfoques / Radial | BlurEffects 2 |
| RedEyeRemoveEffect | Arte y fotografía / Reducir ojos rojos | ArtisticEffects 6 |
| ReduceNoiseEffect | Más filtros / Reducir ruido | UtilityEffects 4 |
| ReliefEffect | Más filtros / Relieve direccional | UtilityEffects 6 |
| SharpenEffect | Básicos / Enfocar | PixelEffects 1 |
| SoftenPortraitEffect | Arte y fotografía / Retrato suave | ArtisticEffects 4 |
| TileEffect | Distorsiones / Cristales | DistortionEffects 3 |
| TwistEffect | Distorsiones / Remolino | DistortionEffects 0 |
| UnfocusEffect | Más filtros / Desenfoque de lente | UtilityEffects 1 |
| VignetteEffect | Básicos / Viñeta | PixelEffects 6 |
| VoronoiDiagramEffect | Generadores / Voronoi | RenderEffects 1 |
| ZoomBlurEffect | Desenfoques / Zoom | BlurEffects 3 |

## Límites explícitos

- Interfaz de parámetros con aplicación final; aún no hay previsualización interactiva antes de aplicar.
- Óleo: radio máximo 8; percentiles: 4; gaussiano: 32; objetos: 64. Los rangos se limitan para Android.
- Tramado ordenado Bayer con niveles RGB; no reproduce todos los métodos/paletas de Pinta.
- Desenfoque de lente mediante disco muestreado; no constituye integración óptica exacta.
- Objetos detectados por alfa sobre fondo transparente. Contorno con color de dibujo y opción de alfa gradual; no incluye degradado de dos colores o relleno de fondo de Pinta.
- Generadores con primer color de dibujo y segundo blanco/negro/transparente; no hay edición independiente de dos colores arbitrarios en este diálogo.
- Los efectos actúan sobre la capa activa y respetan la selección. Los generadores reemplazan contenido; Deshacer restaura los píxeles anteriores.

Pruebas: motor C++ por familia, propiedades de transparencia/determinismo/identidad/límites, referencia independiente para percentiles y llamadas reales Java→JNI con historial y selecciones. La revisión visual en el teléfono sigue siendo una fase independiente.

Validación del catálogo: commit `c20cc4296a1f20dc7d5f09bd86dec1fca82baf7b`, Core tests 37987595585 y Android APK 37987595653, ambos aprobados.
