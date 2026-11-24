# Recursos de Clean Mind

- `branding/`: originales del icono de la app, antes sin versionar en la raíz.
- `figma-source/icons/`: fuentes históricas de Figma, conservadas para referencia; no se distribuyen ni se usan como iconos de la app.
- `material-symbols.json`: procedencia, huella del original y ejes de los iconos activos.

Los iconos activos son Material Symbols Outlined (Material 3), sin relleno, peso 500, grade 0 y tamaño óptico 24. Se generan como ImageVector locales en `MindIcons.kt`; no necesitan conexión ni una fuente de iconos en tiempo de ejecución. La notificación usa el mismo símbolo en XML. Licencia en `app/src/main/assets/licenses/Material-Symbols-LICENSE.txt`.

Para regenerar, descarga el TTF y los codepoints oficiales y ejecuta `scripts/update_material_icons.py` con fonttools. La especificación de ejes es fija en el generador. Después ejecuta el formato compartido.

## Marca monocroma y arranque

`branding/clean-mind-monochrome.svg` es la versión monocroma editable. La pluma conserva la silueta original en blanco sobre gris carbón; los originales de color se guardan únicamente como referencia histórica. El launcher usa vectores adaptativos y una máscara monocroma para iconos temáticos, con un fallback vectorial desde API 24. La imagen de tienda también se ha regenerado desde el vector.

`ic_splash_mark.xml` muestra la pluma sin recuadro, en el color de texto del tema del sistema. `Theme.CleanMind.Starting` utiliza AndroidX SplashScreen 1.2.0 y vuelve a `Theme.CleanMind` al dibujar el contenido. Solo espera la lectura real de preferencias; no añade un temporizador de permanencia. La salida dura 180 ms y se omite cuando Android tiene los animadores desactivados.

Referencias oficiales: [SplashScreen](https://developer.android.com/develop/ui/views/launch/splash-screen/migrate) y [AndroidX Core Splashscreen](https://developer.android.com/jetpack/androidx/releases/core#core-splashscreen).
