# Recursos de Clean Mind

- `branding/`: originales del icono de la app, antes sin versionar en la raíz.
- `figma-source/icons/`: fuentes históricas de Figma, conservadas para referencia; no se distribuyen ni se usan como iconos de la app.
- `material-symbols.json`: procedencia, huella del original y ejes de los iconos activos.

Los iconos activos son Material Symbols Outlined (Material 3), sin relleno, peso 200, grade -25 y tamaño óptico 24. Se generan como ImageVector locales en `MindIcons.kt`; no necesitan conexión ni una fuente de iconos en tiempo de ejecución. La notificación usa el mismo símbolo en XML. Licencia en `app/src/main/assets/licenses/Material-Symbols-LICENSE.txt`.

Para regenerar, descarga el TTF y los codepoints oficiales y ejecuta `scripts/update_material_icons.py` con fonttools. La especificación de ejes es fija en el generador. Después ejecuta el formato compartido.
