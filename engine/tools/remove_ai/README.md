# Remove AI — Android local

Herramienta independiente ID 28. El icono `assets/remove_ai/remove_ai.webp` es el recurso exacto entregado por el usuario.

- `RemoveAiTool`: gestos y vista previa, sin rasterizar en el documento.
- `RemoveAiModule`: máscara por lienzo, botones Eliminar/Limpiar, cancelación, revisión y ciclo de vida.
- `RemoveAiBackend`: sesión ONNX reutilizada y cerrada al destruir la vista; CPU en un hilo de trabajo. No envía imágenes ni descarga modelos desde la app. Desactiva arena de CPU y patrones preasignados para evitar que la RAM crezca entre ejecuciones; serializa la inferencia entre vistas.
- `RemoveAiIcon`: carga del recurso original.
- Puente transaccional `app/src/main/cpp/remove_ai/RemoveAiBridge.inc`: lectura de capa activa y composición limitada a máscara, alfa original, revisión/capa/dimensiones verificadas, un único paso Deshacer/Rehacer.

MI-GAN Places2 512 oficial: https://github.com/Picsart-AI-Research/MI-GAN
Modelo pipeline v2, revisión `1538c135034b8cfe7a8472f34d09c8a5a45b17a7` de `andraniksargsyan/migan`, SHA256 `6f1f3530a1a2324b19752018ce756088b07973cda8d7d890034ace5c8a48c40b`. La compilación obtiene y verifica el modelo y lo incluye en el APK; no se guarda el binario generado en git. Licencia MIT adjunta y también incluida en los assets del APK.

Entrada imagen RGB uint8 `[1,3,H,W]`, máscara uint8 `[1,1,H,W]`: 255 conserva, 0 elimina. El pipeline oficial realiza recorte, escalado a 512, normalización, inferencia y reescalado. La app conserva exactamente el exterior y alfa. Se rechaza máscara vacía o que cubra todo el fondo. Si hay selección activa, la máscara se intersecta con ella.

Uso: seleccionar Remove AI, ajustar anchura, pintar objeto/sticker y pulsar Eliminar. Cancelar descarta el resultado aunque el cálculo en curso termine. Cambiar herramienta, capa o documento invalida la máscara. Una reconstrucción es estimada: no recupera los píxeles originales ocultos.

Verificación: `RemoveAiDeviceTests` ejecuta el modelo real sobre imagen rectangular, verifica alfa/exterior, reutilización, máscaras inválidas, transacción obsoleta y Deshacer/Rehacer; también valida máscara no destructiva e icono.

La única modificación dentro de una herramienta existente es el registro compartido `tools/shared/Tools.java` para añadir el nuevo ID; los 78 archivos restantes permanecen idénticos.

Los botones resuelven la instancia activa de Remove en cada pulsación. La reorganización del espacio de trabajo puede separar y volver a insertar la vista sin recrear la barra; `toolbarButtonsUseCurrentModuleAfterWorkspaceRelayout` verifica Limpiar máscara y Eliminar mediante los botones reales después de dos reorganizaciones, inferencia y Deshacer.
