# Barras, Home y editor

La revisión simplifica el cuaderno y utiliza la [navegación flexible de Material 3 Expressive](https://developer.android.com/develop/ui/compose/components/navigation-bar), disponible en la versión de Compose del proyecto.

## Navegación y Home

- Barra inferior acoplada al borde, con Inicio, Notas, Nueva nota, Recordar y Buscar. Los cuatro destinos muestran su selección; Nueva nota es una acción y abre la hoja **Crear**, con Nueva nota y Nuevo recordatorio desde cualquier pestaña, incluida Inicio.
- Buscar enfoca el campo al entrar. La barra se oculta con el teclado de búsqueda y reaparece al cerrarlo.
- Nuevo recordatorio está en la hoja Crear y en el **+** de la cabecera de Recordar, también cuando hay avisos en la colección.
- La Home conserva fechas y agrupación por creación, sin barra de búsqueda ni título «Tu agenda». El cambio entre cuadrícula y agenda está junto a Ajustes. En Notas y Buscar, el mismo lugar cambia lista/cuadrícula.
- La pluma de `ic_launcher_foreground`, el mismo dibujo del icono de la app, acompaña a Clean Mind. El resto conserva Material Symbols con peso 500.
- La cabecera mantiene su comportamiento de ocultarse al descender y reaparecer al llegar al inicio. Scaffold reserva el espacio de ambas barras.

La pulsación larga selecciona directamente una nota o recordatorio, sin hoja intermedia. Un toque añade o retira otros elementos. Con una sola nota seleccionada, Más acciones de selección permite compartir, mover y fijar; eliminar usa el mismo diálogo y operación transaccional para uno o varios elementos.

## Editor

La fila inferior ofrece **Dictar, Lista, Imagen y Recordar**. Con teclado usa iconos compactos y con texto grande puede distribuirse en dos filas para evitar recortes.

Se retira el menú global de tres puntos: Compartir está en la cabecera, Deshacer/Rehacer en la barra de formato y las inserciones en la fila inferior. El panel completo de formato y las opciones propias de cada imagen permanecen disponibles.

Las casillas se crean con Lista, mantienen su edición, borrado y Deshacer. Se retiran los accesos de casilla en la fila de formato y en su panel; las viñetas, números, estilos y espaciado permanecen disponibles.

Se eliminan concentración y archivo del editor y el menú de las notas. Las notas archivadas anteriormente se muestran en las colecciones habituales sin reescribir el documento. El campo antiguo se conserva por compatibilidad.

## Contraste

La paleta usa fondos carbón y blanco roto, texto gris y contornos más suaves. El acento sigue las preferencias de apariencia. Original oscuro utiliza `#181818`, superficies `#202020` y texto `#DCDCDC`; claro utiliza `#F0F0F0`, superficies `#F6F6F6` y texto `#3C3C3C`. El tema Gris continúa siendo monocromo. Se comprueba la legibilidad de texto y acciones en todas las paletas incorporadas, además de los acentos personalizados.

## Verificación

Compilaciones debug/release, lint de ambas variantes y **100 pruebas locales** correctas, sin fallos ni omisiones. La suite conserva las regresiones del editor, casillas, metadatos, almacenamiento y selección múltiple. Incluye navegación de cinco acciones a 320 dp con texto grande, ausencia de solapamientos, búsqueda con foco, colocación de cambio de vista junto a Ajustes y acceso a notas archivadas antiguas.


En la revisión anterior, la comprobación física se realizó en el Xiaomi Redmi Note 8 Pro conectado, Android API 36. Se verificaron los cinco accesos, creación contextual de aviso, cambio de vista en Home/Notas, ausencia de búsqueda en Home, dictado inferior con teclado, creación y persistencia de una casilla desde Lista, ausencia de casillas en formato y de archivo en el menú, búsqueda con foco y filtrado, navegación durante scroll y ambas apariencias. Las dos pruebas instrumentadas también pasaron. El modo previo Como el dispositivo se restauró. Se eliminó la nota temporal y su copia TXT y se compararon todos los campos de las 9 notas originales y las 2 carpetas. Las capturas del repositorio son sintéticas; las del dispositivo se mantienen fuera del repositorio.

Las capturas actuales de [Inicio](ui-preview/home-dark.png), [selección](ui-preview/selection-home-dark.png), [creación](ui-preview/creation-sheet-dark.png) y [editor](ui-preview/editor-dark.png) se generan con datos sintéticos de Robolectric.

La revisión actual añade validación de la variante release optimizada en el Xiaomi y repite las dos pruebas instrumentadas. Tras retirar los elementos temporales, las 10 notas originales, las 2 carpetas y las preferencias permanecen idénticas. El detalle está en [Pruebas](pruebas.md).
