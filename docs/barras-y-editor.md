# Barras, Home y editor

La revisión simplifica el cuaderno y utiliza la [navegación flexible de Material 3 Expressive](https://developer.android.com/develop/ui/compose/components/navigation-bar), disponible en la versión de Compose del proyecto.

## Navegación y Home

- Barra inferior acoplada al borde, con Inicio, Notas, Nueva nota, Recordar y Buscar. Los cuatro destinos muestran su selección; Nueva nota es una acción y abre el editor directamente.
- Buscar enfoca el campo al entrar. La barra se oculta con el teclado de búsqueda y reaparece al cerrarlo.
- Nuevo recordatorio queda en el **+** de la cabecera de Recordar, también cuando hay avisos en la colección.
- La Home conserva fechas y agrupación por creación, sin barra de búsqueda. El cambio entre cuadrícula y agenda está junto a Ajustes. En Notas y Buscar, el mismo lugar cambia lista/cuadrícula.
- Una pluma estilográfica Material Symbols acompaña a Clean Mind. Se conserva el peso 500 de los iconos.
- La cabecera mantiene su comportamiento de ocultarse al descender y reaparecer al llegar al inicio. Scaffold reserva el espacio de ambas barras.

## Editor

La fila inferior ofrece **Dictar, Lista, Imagen y Recordar**. Con teclado usa iconos compactos y con texto grande puede distribuirse en dos filas para evitar recortes.

Las casillas se crean con Lista, mantienen su edición, borrado y Deshacer. Se retiran los accesos de casilla en la fila de formato y en su panel; las viñetas, números, estilos y espaciado permanecen disponibles.

Se eliminan concentración y archivo del editor y el menú de las notas. Las notas archivadas anteriormente se muestran en las colecciones habituales sin reescribir el documento. El campo antiguo se conserva por compatibilidad.

## Contraste

La paleta usa fondos carbón y blanco roto, texto gris y contornos más suaves. El acento sigue las preferencias de apariencia. Original oscuro utiliza `#181818`, superficies `#202020` y texto `#DCDCDC`; claro utiliza `#F0F0F0`, superficies `#F6F6F6` y texto `#3C3C3C`. El tema Gris continúa siendo monocromo. Se comprueba la legibilidad de texto y acciones en todas las paletas incorporadas, además de los acentos personalizados.

## Verificación

Compilaciones debug/release, lint de ambas variantes y **98 pruebas locales** correctas, sin fallos ni omisiones. La suite conserva las regresiones del editor, casillas, metadatos, almacenamiento y selección múltiple. Incluye navegación de cinco acciones a 320 dp con texto grande, ausencia de solapamientos, búsqueda con foco, colocación de cambio de vista junto a Ajustes y acceso a notas archivadas antiguas.


La comprobación física se realizó en el Xiaomi Redmi Note 8 Pro conectado, Android API 36. Se verificaron los cinco accesos, creación contextual de aviso, cambio de vista en Home/Notas, ausencia de búsqueda en Home, dictado inferior con teclado, creación y persistencia de una casilla desde Lista, ausencia de casillas en formato y de archivo en el menú, búsqueda con foco y filtrado, navegación durante scroll y ambas apariencias. Las dos pruebas instrumentadas también pasaron. El modo previo Como el dispositivo se restauró. Se eliminó la nota temporal y su copia TXT y se compararon todos los campos de las 9 notas originales y las 2 carpetas. Las capturas del repositorio son sintéticas; las del dispositivo se mantienen fuera del repositorio.
