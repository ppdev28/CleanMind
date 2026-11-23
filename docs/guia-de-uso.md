# Guía de uso

Clean Notes permite guardar ideas y apuntes personales en Android. No necesitas una cuenta para utilizarla.

## Pantallas

| Zona | Qué muestra |
| --- | --- |
| Inicio | Fecha, contadores de notas y recordatorios y actividad reciente. |
| Notas | Notas normales, sin incluir recordatorios. |
| Recordar | Acceso a la colección de recordatorios. Su pantalla se titula «Recordatorios». |
| Buscar | Resultados por título o contenido. |
| Editor | Título, subtítulo opcional, cuerpo de la nota, estado de guardado y acciones. |
| Ajustes | Selección de apariencia e información sobre almacenamiento y recordatorios. |

La barra inferior da acceso a Inicio, Notas y Recordatorios. La búsqueda está en Inicio y Notas. **+ Nota** abre el editor y **¿Qué quieres guardar?** abre la captura rápida. El control de ajustes abre **A tu ritmo**.

## Crear una nota

1. Pulsa **+ Nota**.
2. Se abre el editor.
3. Escribe un título opcional; si lo dejas vacío, la app utiliza una primera línea legible al guardar.
4. Añade un subtítulo opcional y el texto de la nota.
5. Comprueba el estado de guardado en **⋮** o pulsa la flecha de volver, que guarda y cierra el editor.

También puedes crear la primera nota desde el botón del estado vacío.

El editor guarda después de aproximadamente 600 ms sin nuevos cambios. Si vuelves con cambios válidos pendientes, intenta guardarlos antes de cerrar. El botón **A** de la barra inferior abre el formato del texto seleccionado o del párrafo actual.

## Entender el guardado

| Mensaje | Significado |
| --- | --- |
| Cambios sin guardar | El editor todavía no ha confirmado la persistencia del borrador. En una nota nueva vacía la barra no muestra ningún estado. |
| Guardando… | Hay una escritura en curso. |
| Guardado en el dispositivo | La última versión del texto está guardada. |
| Borrador | La versión actual aún no ha confirmado el guardado. |
| No se ha podido guardar | El intento ha fallado; el editor conserva el borrador durante la sesión. |

Si intentas salir tras un error de guardado, puedes seguir escribiendo o descartar los cambios pendientes. Descartar no elimina una versión que ya se hubiera guardado automáticamente.

No uses el borrador temporal como sustituto del guardado: antes de cerrar o forzar la detención de la app, comprueba la indicación de persistencia. No se garantiza recuperar un borrador tras cualquier cierre forzado o borrado de datos.

## Editar y consultar

En Inicio las tarjetas muestran únicamente título, subtítulo si se ha escrito, conteo de imágenes y fecha/hora del último guardado. El cuerpo no aparece ni en lista ni en cuadrícula. Las notas antiguas no obtienen un subtítulo a partir del contenido: puedes escribirlo en el editor.

Mantén pulsada una tarjeta para abrir las opciones **Compartir nota** (texto o PDF) y **Eliminar**. Eliminar sigue solicitando confirmación. La pulsación normal abre la nota. Estas opciones también están disponibles desde **⋯** en las colecciones.

Toca una tarjeta para abrir su contenido. Los cambios se guardan en la misma nota, manteniendo su identificador. La fecha utilizada para ordenar la colección se actualiza al guardar cambios: la actividad refleja la última escritura, no una fecha de creación independiente.

En Notas puedes alternar entre:

- **Lista:** elementos agrupados por día.
- **Cuadrícula:** tarjetas distribuidas según el ancho disponible.

La elección de vista pertenece al estado de cada pestaña. No hay un ajuste global de vista persistido en las preferencias.

## Crear un recordatorio

1. Desde una nota, pulsa **Recordar**, o utiliza **+ Recordatorio** en la pestaña Recordatorios.
2. Escribe en el título lo que quieres recordar.
3. Espera a que se guarde o pulsa la flecha de volver.
4. Consúltalo desde **Recordar**.

El editor permite añadir contenido y activar **Notificación del sistema**. Configura fecha, hora, mensaje, repetición (una vez o cada 1–365 minutos, horas, días, semanas o meses), sonido, vibración y prioridad. El título es opcional y se genera un nombre legible si se deja vacío. También puedes configurar los minutos de posposición. Puedes elegir el tono desde **Personalizar tono en Android**; Android controla los canales y puede silenciarlos.

Concede el permiso de notificaciones en Android 13 o posterior. Para puntualidad, permite **avisos exactos** en Android 12 o posterior; sin ese acceso se programa un aviso aproximado que el sistema puede retrasar. Una fecha pasada sin repetición no programa otro aviso. Las repeticiones omiten ocurrencias pasadas. Al borrar el recordatorio o desactivar su notificación se cancela la alarma pendiente. Se recuperan las futuras tras reiniciar y actualizar la app. Una detención forzada desde Android puede impedir avisos hasta volver a abrir la app.

Pulsa la notificación para abrir su nota o **Posponer** (duración configurable de 1 a 180 minutos) para volver a recibirla. La repetición mantiene su calendario original. Puedes **Completar** un aviso sin eliminar la nota; si es periódico se programa la siguiente ocurrencia.

## Buscar

Abre **Buscar** y escribe una parte del título o del contenido. La búsqueda:

- Incluye notas y recordatorios.
- No distingue mayúsculas y minúsculas.
- Ignora los espacios al principio y al final de la consulta.
- Compara fragmentos literales; no implementa búsqueda aproximada ni normalización de tildes.
- Muestra una explicación inicial cuando la consulta está vacía.

El botón de borrar vacía la consulta. Si no hay resultados, prueba con un fragmento más corto o con otra palabra.

## Eliminar

Abre el menú de tres puntos de una tarjeta y elige eliminar. También puedes mantener pulsada la tarjeta. Confirma la acción en el diálogo.

La eliminación es permanente en la base de datos de la app: no hay papelera ni función de deshacer. Si falla, se muestra un mensaje y la nota sigue disponible para volver a intentarlo.

## Compartir

En el editor, con un título escrito, pulsa el icono de compartir. Android permite elegir la aplicación receptora.

La hoja **Comparte tu nota** muestra una vista previa y permite elegir **Texto** o **PDF**. La acción principal cambia a **Compartir texto** o **Compartir PDF**. Texto envía directamente título, subtítulo y cuerpo, sin adjuntos, para pegarlo fácilmente en otras aplicaciones. PDF envía el documento con formatos básicos, páginas e imágenes, concediendo acceso temporal al archivo.

**Guardar archivo…** abre el selector de Android para elegir nombre y destino del TXT (UTF-8, sin marcas ni imágenes) o PDF. **Guardar en mi carpeta** aparece si hay un directorio configurado. **Copiar texto** copia título, subtítulo y cuerpo al portapapeles, independientemente del formato seleccionado. Los controles se desactivan mientras se prepara el archivo; se muestran errores y confirmaciones de guardado, sin dar por enviado un archivo al abrir el selector de aplicaciones.

En **Ajustes → Carpeta de las notas → Elegir carpeta**, autoriza un directorio. Cada guardado posterior actualiza `nota-ID.txt` en esa carpeta. Al elegir carpeta se copian también las notas existentes; **Copiar todas las notas ahora** permite repetir la copia. Las copias no se eliminan al borrar una nota en la app. La base de datos y los adjuntos originales siguen en almacenamiento privado. Esto no es una copia completa restaurable. El menú de exportación también ofrece **Guardar en mi carpeta** para TXT y PDF. Los fallos de copia se indican en Ajustes; el original local permanece guardado.

## Cambiar el aspecto

En Ajustes puedes elegir:

- **Como el dispositivo:** sigue la apariencia del sistema.
- **Claro:** fuerza el tema claro.
- **Oscuro:** fuerza el tema oscuro.

La rama de Figma utiliza la paleta neutra del diseño: superficies oscuras y acciones claras, con una adaptación equivalente para el tema claro. Los diálogos de fecha y hora también siguen la apariencia elegida.

La selección se aplica al guardarse y se conserva en DataStore. No hay un botón adicional para confirmar los ajustes. Si aparece un error de escritura, vuelve a seleccionar el tema; **Reintentar** vuelve a consultar las preferencias.

## Datos y límites

Las notas se guardan localmente y se pueden consultar sin conexión. La aplicación no incluye autenticación, sincronización propia, cifrado específico de las notas, etiquetas ni exportación/importación de colecciones.

El manifiesto permite copias de seguridad del sistema. Por tanto, «sin sincronización propia» no equivale a garantizar que Android nunca respalde los datos. Consulta [Datos y migraciones](datos-y-migraciones.md) para conocer la configuración implementada.

La interacción con el teclado, los gestos y el diseño en dispositivos reales sigue pendiente de validación. Esta guía describe los controles implementados en el código.

[Índice de documentación](README.md).

## Formato y fotografías

Pulsa **A** en la barra inferior para mostrar la fila **H₁, H₂, Aa, negrita, cursiva, subrayado y borrar formato**. La **×** cierra esa fila. Las opciones de tachado, alineación, sangría y espaciado están en **⋮ → Formato y espaciado**. Selecciona una palabra o un fragmento antes de cambiarlo. Sin selección, el formato se aplica al párrafo actual cuando contiene texto.

- Negrita, cursiva, subrayado y tachado. Los colores siguen el tema de la app.
- Tamaños de título, subtítulo y texto normal.
- Alineación izquierda, centro o derecha y sangría.
- Listas con viñetas o numeradas sobre las líneas seleccionadas.
- **Casilla**: añade una casilla al párrafo del cursor; vuelve a pulsar para marcarla o desmarcarla.
- Interlineado de 1 a 2, espacio adicional entre líneas y separación entre párrafos. Estos ajustes de espaciado afectan a toda la nota.
- **Sin formato** elimina las marcas del fragmento seleccionado; no restablece los ajustes globales de espaciado.
- **Deshacer/Rehacer** funciona durante la sesión del editor; el historial no se guarda al cerrar.

En **+**, **Añadir imagen** abre el selector y **Hacer foto** permite tomar una fotografía con una aplicación de cámara instalada. **Pegar imagen** y el menú nativo de pegado aceptan imágenes que el portapapeles exponga mediante una URI accesible. No descargan enlaces web como imágenes.

Las imágenes se copian como JPEG privados de la app, con reducción de resolución y corrección de orientación EXIF. Aparecen después del texto y las casillas y pueden quitarse o moverse hacia arriba desde su menú **⋮**. Quitar un adjunto de una nota no borra la fotografía original de la galería. Los archivos privados que queden sin referencia todavía no tienen limpieza automática.

No hay OCR, escaneo de documentos, tablas, dibujo, justificación ni posicionamiento de imágenes entre palabras. Las listas son texto editable: Intro continúa las viñetas, casillas o el siguiente número. Intro sobre un elemento vacío termina la lista. Pegar varios párrafos conserva el texto pegado.

La referencia visual de esta rama es el archivo Figma. Consulta [UI de Figma](figma-ui.md) para las pantallas, recursos, funciones y límites de comparación.

El estado de guardado se consulta en **⋮**; los errores también aparecen en un aviso. Las vistas previas de `EditNotePreviews.kt` permiten revisar claro, oscuro y texto grande desde Android Studio sin instalar la app.


## Fijado, archivo y concentración

La pulsación larga abre Compartir, Fijar a mano, Archivar y Eliminar. Las notas fijadas aparecen en **A mano**. El filtro **Archivadas** permite abrir y restaurar notas archivadas. El aviso de archivo ofrece **Deshacer** después de confirmarse el cambio.

En el editor, **Concentrarme** oculta las herramientas. La acción de cerrar concentración vuelve al editor sin perder el borrador. **Lista** añade una casilla editable; se conserva la continuación de viñetas/números de las listas de texto.
