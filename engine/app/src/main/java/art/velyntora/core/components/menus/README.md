# components/menus

Responsabilidad propia de components/menus.

Fuentes: `WorkspaceMenus.java`.

Los paquetes Java existentes se conservan por compatibilidad con Android, JNI y las herramientas. La carpeta expresa la propiedad del componente. Los componentes de interfaz reciben la fachada de composición; consultan y actualizan su estado mediante métodos, sin acceso directo a campos. Los codecs y modelos puros no dependen de la Activity.

Validación: comprobación de límites modular, compilación Android y suite de integración del emulador. Los modelos y codecs también se verifican con las pruebas JVM existentes. No cambiar `tools/` para trabajar en este módulo.

## Menú ≡ de Animation

Archivo, Editar y selección, Lienzo e imagen, Color, Efectos, Ver y Ayuda.
Archivo conserva la importación de video, su progreso y la inserción de imágenes como capas.
La barra conserva Nuevo, Abrir, Guardar y exportar, Deshacer, Rehacer y los paneles.
Los ajustes de la herramienta activa se abren desde su barra; los colores, desde la paleta.
Color agrupa los ajustes comunes y separa los canales y tratamientos avanzados.
Efectos clasifica los filtros sin cambiar sus identificadores ni el motor.
