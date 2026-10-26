# Adaptador de presets Krita

VelyntoraBrushCatalog carga el catálogo y las miniaturas. VelyntoraBrushRuntime prepara la punta una vez por selección y reinicia la sesión nativa por gesto. TipBrushEngine interpola sellos, conserva el resto de espaciado entre eventos, respeta la máscara de selección y usa StrokeTiles para mantener la opacidad del gesto sin duplicar capas completas. Mezcla muestrea únicamente la capa activa con alfa premultiplicado. Nunca añade pigmento a una capa transparente si el preset no tiene aporte de color.

JNI se encuentra en app/src/main/cpp/brush/VelyntoraBrushBridge.inc. No modifica los renderizadores ClassicBrush ni AntialiasedBrush. Las limitaciones de adaptación están documentadas en el README de la colección.
