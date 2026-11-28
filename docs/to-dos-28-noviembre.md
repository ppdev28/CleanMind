# To Dos · 28 de noviembre de 2026

Se leyó la nota «To Dos» de la instalación realme y se implementaron sus puntos en este orden. El punto 1, Biometría, queda excluido expresamente. La nota original no se marca ni modifica.

| Punto | Resultado | Commit |
| --- | --- | --- |
| 2. Dictado por voz | Micrófono en el editor, reconocimiento de Android, inserción en cursor/selección y Deshacer. | `9f7901d` |
| 3. Custom themes | Paletas y temas propios con nombre/acento, edición, borrado y persistencia; adaptación claro/oscuro. | `4166cf7` |
| 4. Files, photos and automatic linking previews | Adjuntos privados, apertura con aplicación compatible, fotografías a pantalla completa y metadatos de enlaces con caché y fallback. | `9604b55` |
| 5. Gris theme | Paleta monocroma, grafito en oscuro y adaptación clara. | `9007c4f` |
| 6. Barra de formato al abrir teclado | Insets de teclado/navegación, barra compacta visible y conservación de selección/composición IME. | `e4472fc` |
| 7. Borrado en selección múltiple | Seleccionar desde pulsación larga, contador, seleccionar visibles, cancelar y confirmar; transacción y cancelación de avisos. | `9d62791` |
| 8. Hover cuadrado de las cards | La indicación de pulsación se dibuja dentro del recorte redondeado de Surface; se mantiene la animación breve. | `1e99c79` |

La [guía de uso](guia-de-uso.md) explica los accesos. [Datos y migraciones](datos-y-migraciones.md) documenta los campos nuevos sin cambiar el esquema Room 4, DataStore y las copias privadas. [Arquitectura](arquitectura.md) describe la integración MVVM + UDF.

## Verificación

`assembleDebug`, `assembleRelease`, `testDebugUnitTest`, `lintDebug` y `lintRelease` correctos. **96 pruebas locales** sin fallos ni omisiones. Incluyen dictado, contraste, temas persistentes, archivos, enlaces, composición IME, borrado atómico y selección en ambas colecciones.

Las vistas previas de enlaces dependen de conexión y metadatos publicados por cada página; la escritura local funciona sin red. TXT/PDF incluyen los nombres de archivos, no sus binarios. El dictado depende de un servicio compatible instalado en Android. No hay todavía limpieza automática de adjuntos privados sin referencia.


En el realme se verificaron los temas, la barra sobre el teclado, formato persistido, archivo privado, fotografía completa, metadatos HTTPS y borrado múltiple de notas y recordatorios con cancelación de avisos. Las dos pruebas instrumentadas también finalizaron correctamente. Se retiraron los datos de prueba y la comparación conserva idénticas las 12 notas originales y la carpeta. [Resultados y límites de la revisión](pruebas.md#to-dos--28-de-noviembre-de-2026).
