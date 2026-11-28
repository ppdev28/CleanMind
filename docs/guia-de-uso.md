# Guía de uso

Clean Mind permite guardar ideas y apuntes personales en Android. No necesitas una cuenta para utilizarla.

## Pantallas

| Zona | Qué muestra |
| --- | --- |
| Inicio | Cuadrícula de notas agrupadas por fecha de creación, con opción de agenda vertical. |
| Notas | Notas normales, sin incluir recordatorios. |
| Recordar | Acceso a la colección de recordatorios. Su pantalla se titula «Recordatorios». |
| Buscar | Resultados por título o contenido. |
| Editor | Título, subtítulo opcional, cuerpo de la nota, estado de guardado y acciones. |
| Ajustes | Selección de apariencia e información sobre almacenamiento y recordatorios. |

La barra inferior fija muestra **Inicio, Notas, Nueva nota, Recordar y Buscar**. Nueva nota abre una hoja con **Nueva nota** y **Nuevo recordatorio**, disponible también desde Inicio; Buscar enfoca el campo de consulta. La barra se oculta mientras se escribe una búsqueda y vuelve al cerrar el teclado. Durante una selección múltiple, sus acciones quedan desactivadas hasta cancelar o completar la selección. La cabecera se oculta al bajar y reaparece al llegar al inicio; el cambio de vista está junto al engranaje de Ajustes. La pluma del propio icono de la app acompaña al nombre Clean Mind.

## Crear una nota

1. Pulsa **Nueva nota** en el centro de la barra inferior.
2. Elige **Nueva nota** en la hoja de creación.
3. Escribe un título opcional; si lo dejas vacío, la app utiliza una primera línea legible al guardar.
4. Añade un subtítulo opcional y el texto de la nota.
5. Comprueba el estado de guardado en la cabecera o pulsa la flecha de volver, que guarda y cierra el editor.

También puedes crear la primera nota desde el botón del estado vacío.

El editor guarda después de aproximadamente 600 ms sin nuevos cambios. Si vuelves con cambios válidos pendientes, intenta guardarlos antes de cerrar. La barra de formato muestra directamente Deshacer, Rehacer, negrita, cursiva, subrayado y listas; **Más formato y espaciado** abre el panel completo del editor normal.

## Entender el guardado

| Mensaje | Significado |
| --- | --- |
| Borrador | La versión actual aún no ha confirmado el guardado; una nota nueva vacía permanece como borrador. |
| Guardando… | Hay una escritura en curso. |
| Guardado | La última versión del texto está guardada en el dispositivo. |
| No se pudo guardar | El intento ha fallado; el editor conserva el borrador durante la sesión. |
| No se pudo abrir | Ha fallado la carga de la nota; vuelve al cuaderno y reintenta. |

Si intentas salir tras un error de guardado, puedes seguir escribiendo o descartar los cambios pendientes. Descartar no elimina una versión que ya se hubiera guardado automáticamente.

No uses el borrador temporal como sustituto del guardado: antes de cerrar o forzar la detención de la app, comprueba la indicación de persistencia. No se garantiza recuperar un borrador tras cualquier cierre forzado o borrado de datos.

## Editar y consultar

En Inicio las tarjetas se agrupan por día de creación, con un separador horizontal y la fecha completa. No hay círculos ni etiquetas de día independientes. Muestran título, subtítulo si se ha escrito y conteo de imágenes; no muestran el cuerpo. La fecha de cada grupo no cambia al editar una nota. Las notas antiguas utilizan su fecha ya almacenada como referencia porque no hay un historial de creación. Las notas antiguas no obtienen un subtítulo a partir del contenido: puedes escribirlo en el editor.

Mantén pulsada una tarjeta para seleccionarla inmediatamente. Toca otras tarjetas para añadirlas o retirarlas de la selección. Con una sola seleccionada, **Más acciones de selección** ofrece **Compartir nota**, **Mover a carpeta** y fijar/desfijar. La pulsación normal fuera de la selección abre la nota.

Toca una tarjeta para abrir su contenido. Los cambios se guardan en la misma nota, manteniendo su identificador. Las colecciones conservan la fecha de última escritura para su orden habitual. La agenda de Inicio utiliza la fecha de creación independiente.

Inicio se abre en cuadrícula. **Ver en agenda** cambia a una columna; **Ver en cuadrícula** recupera las tarjetas adaptables. Ambas conservan las fechas de creación y los separadores. En Notas y Buscar, **Cambiar vista** alterna lista y cuadrícula.

La elección de vista pertenece al estado de cada pestaña. No hay un ajuste global de vista persistido en las preferencias.

## Crear un recordatorio

1. Desde Inicio, pulsa **Nueva nota → Nuevo recordatorio** en la barra inferior. También puedes abrir **Recordar** y pulsar el **+** de su cabecera.
2. Escribe **¿Qué quieres recordar?**.
3. Activa **Avisarme** y elige la fecha y la hora, o desactívalo para guardar un pendiente sin notificación.
4. Añade un detalle opcional y pulsa **Guardar recordatorio**.
5. Consúltalo desde **Recordar**.

Los recordatorios independientes usan un formulario de tarea, sin herramientas de formato, fotografías ni archivo de notas. Se guardan al pulsar el botón; si vuelves con cambios pendientes, la app pide confirmar si quieres descartarlos. La lista muestra una casilla para completar, el título y la hora. El reloj permite posponer y la pulsación larga inicia la selección para compartir o eliminar. No aparecen como tarjetas en la agenda de Inicio.

**Repetición y opciones de aviso** permite configurar el mensaje, repetición (una vez o cada 1–365 minutos, horas, días, semanas o meses), sonido, vibración, prioridad y minutos de posposición. Puedes elegir el tono desde **Personalizar tono en Android**; Android controla los canales y puede silenciarlos.

También puedes añadir una notificación a una nota normal desde **Recordar** en su editor. En ese caso se conserva la nota y la fecha se configura desde una hoja inferior.

Concede el permiso de notificaciones en Android 13 o posterior. Para puntualidad, permite **avisos exactos** en Android 12 o posterior; sin ese acceso se programa un aviso aproximado que el sistema puede retrasar. Una fecha pasada sin repetición no programa otro aviso. Las repeticiones omiten ocurrencias pasadas. Al borrar el recordatorio o desactivar su notificación se cancela la alarma pendiente. Se recuperan las futuras tras reiniciar y actualizar la app. Una detención forzada desde Android puede impedir avisos hasta volver a abrir la app.

Pulsa la notificación para abrir su nota o **Posponer** (duración configurable de 1 a 180 minutos) para volver a recibirla. La repetición mantiene su calendario original. Puedes **Completar** un aviso sin eliminar la nota; si es periódico se programa la siguiente ocurrencia.

## Organizar en carpetas

En **Notas** las carpetas aparecen en cuadrícula. El icono de carpeta con **+** abre **Gestionar carpetas**, donde puedes crearlas o renombrarlas. Selecciona una para consultar sus notas; el botón **Nueva nota** de la barra inferior utiliza esa carpeta. Mantén pulsada una nota y abre **Más acciones de selección → Mover a carpeta**. **Sin carpeta** reúne las notas sin asignación. Eliminar una carpeta conserva sus notas. Notas no muestra una barra de búsqueda; utiliza la pestaña **Buscar**. Estas carpetas son independientes del directorio de copias TXT de Ajustes. Consulta [Carpetas](carpetas.md).

## Buscar

Abre **Buscar** y escribe una parte del título o del contenido. La búsqueda:

- Incluye notas y recordatorios.
- No distingue mayúsculas y minúsculas.
- Ignora los espacios al principio y al final de la consulta.
- Compara fragmentos literales; no implementa búsqueda aproximada ni normalización de tildes.
- Muestra una explicación inicial cuando la consulta está vacía.

El botón de borrar vacía la consulta. Si no hay resultados, prueba con un fragmento más corto o con otra palabra.

## Eliminar

Mantén pulsada una tarjeta o un recordatorio para seleccionarlo y toca los demás elementos que quieras añadir. La barra muestra el total y ofrece **Seleccionar visibles**, **Eliminar seleccionados** y cancelar. El filtro actual limita qué añade Seleccionar visibles. Confirma **Eliminar selección** en el diálogo; cancelar no borra nada. Un error mantiene la selección para reintentar y los controles se bloquean mientras se completa la operación. Borrar recordatorios cancela sus notificaciones programadas.

La eliminación es permanente en la base de datos de la app: no hay papelera ni función de deshacer. Si falla, se muestra un mensaje y la nota sigue disponible para volver a intentarlo.

## Compartir

En el editor, pulsa **Compartir** en la cabecera. También puedes mantener pulsada una nota en una colección y abrir **Más acciones de selección → Compartir nota**. Android permite elegir la aplicación receptora.

La hoja **Comparte tu nota** muestra una vista previa y permite elegir **Texto** o **PDF**. La acción principal cambia a **Compartir texto** o **Compartir PDF**. Texto envía directamente título, subtítulo y cuerpo, sin adjuntos, para pegarlo fácilmente en otras aplicaciones. PDF envía el documento con formatos básicos, páginas e imágenes, concediendo acceso temporal al archivo.

**Guardar archivo…** abre el selector de Android para elegir nombre y destino del TXT (UTF-8, sin marcas ni imágenes) o PDF. **Guardar en mi carpeta** aparece si hay un directorio configurado. **Copiar texto** copia título, subtítulo y cuerpo al portapapeles, independientemente del formato seleccionado. Los controles se desactivan mientras se prepara el archivo; se muestran errores y confirmaciones de guardado, sin dar por enviado un archivo al abrir el selector de aplicaciones.

En **Ajustes → Carpeta de las notas → Elegir carpeta**, autoriza un directorio. Cada guardado posterior actualiza `nota-ID.txt` en esa carpeta. Al elegir carpeta se copian también las notas existentes; **Copiar todas las notas ahora** permite repetir la copia. Las copias no se eliminan al borrar una nota en la app. La base de datos y los adjuntos originales siguen en almacenamiento privado. Esto no es una copia completa restaurable. El menú de exportación también ofrece **Guardar en mi carpeta** para TXT y PDF. Los fallos de copia se indican en Ajustes; el original local permanece guardado.

## Cambiar el aspecto

En Ajustes puedes elegir:

- **Como el dispositivo:** sigue la apariencia del sistema.
- **Claro:** fuerza el tema claro.
- **Oscuro:** fuerza el tema oscuro.

En **Temas** puedes elegir Original, Gris, Azul acero, Bosque o Lavanda. **Crear tema** permite guardar un nombre y un color de acento (seis dígitos HEX), editarlo o eliminarlo. Los colores de las acciones se adaptan al modo claro u oscuro para conservar contraste con el fondo. Gris usa controles monocromos y un fondo grafito en oscuro; los errores conservan su color semántico. Eliminar un tema activo recupera Original. Los diálogos de fecha y hora también siguen la apariencia elegida.

La selección se aplica al guardarse y se conserva en DataStore. No hay un botón adicional para confirmar los ajustes. Si aparece un error de escritura, vuelve a seleccionar el tema; **Reintentar** vuelve a consultar las preferencias.

## Datos y límites

Las notas se guardan localmente y se pueden consultar sin conexión. La aplicación no incluye autenticación, sincronización propia, cifrado específico de las notas, etiquetas ni exportación/importación de colecciones.

El manifiesto permite copias de seguridad del sistema. Por tanto, «sin sincronización propia» no equivale a garantizar que Android nunca respalde los datos. Consulta [Datos y migraciones](datos-y-migraciones.md) para conocer la configuración implementada.

El historial de pruebas físicas está en [Pruebas](pruebas.md). Consulta la revisión de restauración del editor, FAB desplegable y cuadrícula en [Pruebas](pruebas.md).

[Índice de documentación](README.md).

## Formato y fotografías

La fila de formato permanece visible sobre el teclado. La fila **Dictar, Lista, Imagen y Recordar** conserva sus cuatro acciones mientras escribes, con iconos compactos sobre el teclado. Con texto grande se distribuye en dos filas para mantener las etiquetas completas. Abrir el panel completo retira el teclado para mostrar sus controles sin perder la selección. Muestra negrita, cursiva, subrayado y listas; deslízala horizontalmente para acceder a las demás herramientas. **Más formato y espaciado** abre un único panel con tamaños H1–H6 y Normal, estilos, listas, alineación, sangría e interlineado y espacios. Los ajustes de espaciado afectan a toda la nota. Se mantiene el editor visual normal, con selección de texto y sin escribir sintaxis Markdown.

Cada casilla de la lista tiene una **×** a la derecha para eliminarla, esté vacía, escrita o marcada. El aviso **Casilla eliminada · Deshacer** permite recuperarla con su contenido, estado y posición. La eliminación no cambia el cuerpo ni el resto de la lista.

**Imagen** ofrece elegir una fotografía, hacerla con una cámara instalada o pegar una imagen que el portapapeles exponga mediante una URI accesible.

Toca una imagen para verla completa y **Cerrar imagen** para volver a la nota. Las imágenes se copian como JPEG privados de la app, con reducción de resolución y corrección de orientación EXIF. Aparecen después del texto y las casillas y pueden quitarse o moverse hacia arriba desde su menú **⋮**. Quitar un adjunto de una nota no borra la fotografía original de la galería. Los archivos privados que queden sin referencia todavía no tienen limpieza automática.

No hay OCR, escaneo de documentos, tablas, dibujo, justificación ni posicionamiento de imágenes entre palabras. Las listas son texto editable: Intro continúa las viñetas, casillas o el siguiente número. Intro sobre un elemento vacío termina la lista. Pegar varios párrafos conserva el texto pegado.

La referencia visual de esta rama es el archivo Figma. Consulta [UI de Figma](figma-ui.md) para las pantallas, recursos, funciones y límites de comparación.

El estado de guardado se consulta en la cabecera; los errores también aparecen en un aviso. Las vistas previas de `EditNotePreviews.kt` permiten revisar claro, oscuro y texto grande desde Android Studio sin instalar la app.


Las notas que se hubieran guardado en Markdown se abren en el editor normal con texto y formato básico, sin mostrar código Markdown. Los destinos de enlaces se conservan visibles en el texto. Abrirlas no escribe cambios; una edición posterior guarda el documento normal, conservando adjuntos, casillas y metadatos.

## Fijado y listas

La pulsación larga abre Seleccionar, Mover a carpeta, Compartir, Fijar a mano y Eliminar. Las notas fijadas permanecen en su día de la agenda y también se encuentran con el filtro **Fijadas**. Se retiran la concentración y la opción de archivar, incluido el filtro Archivadas. Las notas archivadas por versiones anteriores aparecen en las colecciones normales sin reescribir sus datos.

**Lista** añade una casilla editable con su propia **×** para quitarla. Las casillas se gestionan desde esta acción y no desde el menú de formato. Se conservan viñetas y numeración en formato, incluida su continuación con Intro.

Apariencia y carpeta de las notas vuelven a estar visibles directamente en Ajustes, sin desplegar una sección adicional.

## Dictado, archivos y enlaces

El micrófono **Dictar** de la fila inferior abre el reconocimiento del sistema. El texto reconocido sustituye la selección del cuerpo o se inserta en el cursor, con espacios cuando hacen falta y disponible en Deshacer. Cancelar no modifica la nota. Requiere una aplicación compatible de reconocimiento instalada; su disponibilidad y uso de red dependen del servicio del dispositivo.

**Imagen → Adjuntar archivo**, o **Más opciones → Imágenes y archivos**, abre el selector de documentos de Android. Se copian los archivos a almacenamiento privado, con un máximo de 25 MB por archivo. La nota muestra el nombre y tamaño; toca la fila para abrirlo con una app compatible o la **×** para quitarlo. Los nombres se incluyen en búsqueda y exportaciones TXT/PDF; esos formatos no empaquetan los archivos binarios. Quitar un adjunto no modifica su original.

Los enlaces HTTP/HTTPS y `www.` generan automáticamente hasta cinco tarjetas con título, descripción y dominio, después de una pausa al escribir. Las tarjetas abren el navegador al tocarlas. Solo se solicitan metadatos de esas direcciones, sin ejecutar el HTML ni descargar sus imágenes. Los resultados se guardan temporalmente en caché; si no hay conexión o falla la página, se muestra el dominio y el enlace sigue disponible. Escribir y guardar notas sigue funcionando sin red.
