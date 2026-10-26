# components/history

Responsabilidad propia de components/history.

Fuentes: `HistoryActions.java`.

Los paquetes Java existentes se conservan por compatibilidad con Android, JNI y las herramientas. La carpeta expresa la propiedad del componente. Los componentes de interfaz reciben la fachada de composición; consultan y actualizan su estado mediante métodos, sin acceso directo a campos. Los codecs y modelos puros no dependen de la Activity.

Validación: comprobación de límites modular, compilación Android y suite de integración del emulador. Los modelos y codecs también se verifican con las pruebas JVM existentes. No cambiar `tools/` para trabajar en este módulo.

Native undo/redo run through LayerWorkQueue so disk reads, layer restoration and display preparation do not block the UI. Editable curve draft undo retains its existing behavior. Native HistoryArchive has no fixed step limit and archives immutable blocks in app-cache session storage.

HistoryActions checks native stack availability before starting undo/redo. Reaching the beginning/end is a normal no-op. Real undo/redo errors have their own messages; layer progress is delayed 250 ms to avoid flashing dialogs during quick operations.
