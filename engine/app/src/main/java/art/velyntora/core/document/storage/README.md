# document/storage

Apertura, importación, guardado y exportación de archivos.

Fuentes: `DocumentStorage.java`.

Los paquetes Java existentes se conservan por compatibilidad con Android, JNI y las herramientas. La carpeta expresa la propiedad del componente. Los componentes de interfaz reciben la fachada de composición; consultan y actualizan su estado mediante métodos, sin acceso directo a campos. Los codecs y modelos puros no dependen de la Activity.

Validación: comprobación de límites modular, compilación Android y suite de integración del emulador. Los modelos y codecs también se verifican con las pruebas JVM existentes. No cambiar `tools/` para trabajar en este módulo.
