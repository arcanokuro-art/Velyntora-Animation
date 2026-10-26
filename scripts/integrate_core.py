"""Import the pinned Core snapshot and wire it as an Android library. Run once."""
from pathlib import Path
import hashlib, json, shutil, subprocess, sys
root=Path(__file__).resolve().parents[1]
source=Path(sys.argv[1]).resolve()
expected='5843440d28eeeb931bef6808dd5c8782854efd44'
assert subprocess.check_output(['git','-C',str(source),'rev-parse','HEAD'],text=True).strip()==expected
engine=root/'engine'
assert not engine.exists(), 'Engine already imported; do not overwrite subsequent work'
shutil.copytree(source,engine,ignore=shutil.ignore_patterns('.git'))
# Retain every tracked source file, including licenses, resources, tooling and tests.
paths=subprocess.check_output(['git','-C',str(source),'ls-files','-z']).split(b'\0')
original={p.decode():hashlib.sha256((source/p.decode()).read_bytes()).hexdigest() for p in paths if p}
(root/'docs').mkdir(exist_ok=True)
(root/'docs/core-source.json').write_text(json.dumps({'repository':'arcanokuro-art/Velyntora-Core','commit':expected,'files':original},indent=2)+'\n')
def edit(path,old,new):
 p=root/path;s=p.read_text();assert old in s,path;p.write_text(s.replace(old,new))
edit('build.gradle',"id 'com.android.application' version '8.7.3' apply false","id 'com.android.application' version '8.9.2' apply false\n    id 'com.android.library' version '8.9.2' apply false")
with (root/'settings.gradle').open('a') as f:f.write("\ninclude ':drawing-engine'\nproject(':drawing-engine').projectDir = file('engine/app')\n")
edit('engine/app/build.gradle',"id 'com.android.application'","id 'com.android.library'")
edit('engine/app/build.gradle',"  applicationId 'art.velyntora.core'\n",'')
(root/'engine/app/src/main/AndroidManifest.xml').write_text('''<manifest xmlns:android="http://schemas.android.com/apk/res/android">
  <application>
    <activity android:name="art.velyntora.core.MainActivity" android:label="Velyntora Drawing"
      android:theme="@android:style/Theme.Material.NoActionBar"
      android:configChanges="orientation|screenSize|smallestScreenSize|fontScale"
      android:exported="false" />
  </application>
</manifest>
''')
with (root/'app/build.gradle').open('a') as f:f.write("\ndependencies { implementation project(':drawing-engine'); androidTestImplementation 'androidx.test:runner:1.6.2'; androidTestImplementation 'androidx.test.ext:junit:1.2.1' }\n")
edit('app/build.gradle',"versionCode 1","versionCode 2\n        testInstrumentationRunner 'androidx.test.runner.AndroidJUnitRunner'")
edit('app/build.gradle',"'0.1.0-pilot'","'0.2.0-pilot'")
entry=root/'app/src/main/java/app/velyntora/editor';entry.mkdir(parents=True)
(entry/'EditorNavigator.java').write_text('''package app.velyntora.editor;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
/** Start screen -> editor contract. The Android library owns document operations. */
public final class EditorNavigator {
    private EditorNavigator(){}
    public static void create(Activity host){host.startActivity(new Intent(host,art.velyntora.core.MainActivity.class).setAction("app.velyntora.drawing.CREATE"));}
    public static void open(Activity host,Uri image){host.startActivity(new Intent(host,art.velyntora.core.MainActivity.class).setAction("app.velyntora.drawing.OPEN").setData(image).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));}
}
''')
main='app/src/main/java/app/velyntora/MainActivity.java'
edit(main,'public void createIllustration(){info("Versión piloto","La pantalla de inicio está en desarrollo. El motor de dibujo se conectará en la siguiente etapa.");}', 'public void createIllustration(){app.velyntora.editor.EditorNavigator.create(this);}')
edit(main,'info("Imagen seleccionada","La imagen se ha seleccionado. Su edición estará disponible cuando se integre el motor de dibujo.");','app.velyntora.editor.EditorNavigator.open(this,data.getData());')
edit(main,'Prototipo de la pantalla de inicio. Crea ilustraciones y edita imágenes.','Versión piloto con el motor de Velyntora Core integrado. Crea ilustraciones y edita imágenes.')
coremain='engine/app/src/main/java/art/velyntora/core/MainActivity.java'
edit(coremain,'    workspaceComposition.onCreate(state);','    workspaceComposition.onCreate(state);\n    EditorEntry.enter(this, state);\n    EditorEntry.immersive(getWindow());')
edit(coremain,'  void configureColor() {','  @Override public void onWindowFocusChanged(boolean focus) {\n    super.onWindowFocusChanged(focus);\n    if (focus) EditorEntry.immersive(getWindow());\n  }\n\n  void configureColor() {')
edit('engine/app/src/main/java/art/velyntora/core/app/workspace/WorkspaceComposition.java','if (freshProcess) host.recoverDocument();','if (freshProcess && !EditorEntry.hasRequest(host.getIntent())) host.recoverDocument();')
bridge=root/'engine/app/src/main/java/art/velyntora/core/app/entry';bridge.mkdir(parents=True)
(bridge/'EditorEntry.java').write_text('''package art.velyntora.core;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.*;
/** Drawing start-screen entry; preserves Core's document protection and import pipeline. */
final class EditorEntry {
    private EditorEntry(){}
    static boolean hasRequest(Intent intent){return intent!=null && ("app.velyntora.drawing.CREATE".equals(intent.getAction()) || "app.velyntora.drawing.OPEN".equals(intent.getAction()));}
    static void enter(MainActivity host,Bundle state){
        Intent intent=host.getIntent();
        if(state!=null || !hasRequest(intent))return;
        if("app.velyntora.drawing.CREATE".equals(intent.getAction()))host.protectDocument(()->host.configureDimensions(0));
        else if(intent.getData()!=null)host.protectDocument(()->host.openDetected(intent.getData()));
    }
    static void immersive(Window window){
        if(Build.VERSION.SDK_INT>=30){
            window.setDecorFitsSystemWindows(false);
            WindowInsetsController controller=window.getInsetsController();
            if(controller!=null){controller.hide(WindowInsets.Type.systemBars());controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);}
        }else window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }
}
''')
(bridge/'README.md').write_text('Entrada desde Drawing: nuevo lienzo y apertura pasan por protección de documento y las operaciones originales. Recuperación automática omitida únicamente al recibir una petición explícita. Pantalla completa se restaura al recuperar foco.\n')
(root/'README.md').write_text('''# Velyntora Drawing — piloto integrado

Nombre completo: Velyntora Drawing. Nombre bajo el icono: V-Drawing.

- `app/`: inicio, imagen, logo transparente, aviso piloto, crédito Powered By Zaskar Startup y navegación al editor.
- `engine/`: instantánea completa de Velyntora-Core; motor C++, herramientas, pinceles, formatos, persistencia, IA Remove, recursos y pruebas.
- `:drawing-engine`: biblioteca Android en `engine/app`; mantiene el namespace `art.velyntora.core` y las firmas JNI.
- `docs/core-source.json`: revisión de origen y SHA-256 de todos sus archivos.

Crear ilustración abre el formulario de dimensiones original del editor. Abrir imagen pasa su URI al importador original. Atrás usa la protección de cambios de Core y vuelve al inicio. Se mantiene pantalla completa en ambos entornos.

Los proyectos recientes y las preferencias del inicio siguen pendientes; los controles informan de ello. Las opciones del editor Core permanecen disponibles.

Compilar con JDK 17, Gradle 8.12, SDK 35 y CMake 3.22.1: `gradle :app:assembleDebug :app:lintDebug`.
El modelo MI-GAN se descarga durante la compilación y se verifica con SHA-256; la aplicación lo incluye para uso sin conexión.

Pruebas nativas: `cmake -S engine/tests -B build/core-tests && cmake --build build/core-tests --parallel 2 && ctest --test-dir build/core-tests --output-on-failure`.
Validación modular: `python engine/scripts/check_modular_boundaries.py`.
''')
print('Integrated',len(original),'tracked Core files from',expected)
