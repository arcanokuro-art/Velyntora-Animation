# Dependencias y recursos de terceros

- **AndroidSVG 1.4**, `com.caverock:androidsvg-aar:1.4`: rasterizador SVG estático. Copyright 2013–2018 Cave Rock Software Ltd.; licencia Apache 2.0. Fuente: https://github.com/BigBadaboom/androidsvg . Texto de licencia incluido en `app/src/main/assets/licenses/ANDROIDSVG-LICENSE.txt` y empaquetado en el APK.
- **Recursos Pinta**: importados desde el commit fijo descrito en `reference/README.md`; vectores Android adaptados dentro de `app/src/main/res/drawable`. Licencias originales conservadas en `reference/licenses`; licencias MIT y Paint.NET empaquetadas en `app/src/main/assets/licenses/`.
- **Colores X11**: valores RGB y nombres del catálogo `rgb.txt` de X.Org, versión identificada en la primera línea del archivo `app/src/main/resources/x11-rgb.txt`. Se usan para interpretar nombres de color de XPM.
- **AndroidX Test/JUnit**: dependencias exclusivas de `androidTest`; no forman parte del APK de la aplicación.

Los lectores BMP, PCX, XBM/XPM, TGA y contenedores estáticos de Core son implementaciones del proyecto. Consultar un catálogo de formatos de GDK no introduce código GDK en el APK.
