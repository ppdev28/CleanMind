# Carpetas de notas

En **Notas → Carpetas → Gestionar** puedes crear, renombrar y eliminar carpetas. Los nombres admiten de 1 a 40 caracteres; se normalizan espacios y se rechazan duplicados sin distinguir mayúsculas. Las carpetas son de un solo nivel y pertenecen al cuaderno local de la app.

Las carpetas aparecen en una cuadrícula adaptable, con nombre y conteo de notas. Selecciona una para ver sus notas. **Todas las notas** reúne el cuaderno y **Sin carpeta** muestra las notas sin asignación. Los filtros de archivo, fijado e imágenes se combinan con la carpeta seleccionada. La pantalla Notas no tiene barra de búsqueda; la pestaña Buscar sigue consultando todo el cuaderno.

Mantén pulsada una nota en Inicio o Notas y pulsa **Mover a carpeta**. Puedes elegir otra carpeta o **Sin carpeta**. Crear una nota desde el botón central mientras estás en una carpeta la guarda directamente allí. Los recordatorios independientes continúan separados de las carpetas.

Eliminar una carpeta solicita confirmación y conserva todas sus notas en **Sin carpeta**. Mover una nota no cambia su contenido, su ID ni sus fechas. Una edición abierta anteriormente no revierte un movimiento posterior.

## Persistencia y compatibilidad

Room pasa de la versión 3 a la 4: añade `folder`, un índice único para nombres normalizados y `note.folderId` con clave externa y `ON DELETE SET NULL`. La migración conserva todos los campos anteriores; las notas existentes empiezan sin carpeta. Se mantiene también la cadena de migraciones desde la versión 2.

Las operaciones de pertenencia y guardado se resuelven en la base de datos para evitar que un borrador obsoleto deshaga movimientos o apunte a una carpeta eliminada. La carpeta seleccionada se restaura con `SavedStateHandle`.

Estas carpetas son organización interna. La carpeta de copias TXT elegida en Ajustes sigue siendo un destino del sistema de archivos; no se crean allí subdirectorios ni se trasladan fotografías. TXT/PDF conservan el contenido de cada nota y no son una copia restaurable de la estructura de carpetas.

Las pruebas cubren migraciones 2→4 y 3→4, ciclo de vida de carpetas, duplicados con acentos, movimientos, guardados obsoletos, creación dentro de una carpeta, errores y filtrado/movimiento desde Compose.
