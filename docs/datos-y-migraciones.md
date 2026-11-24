# Datos y migraciones

## Fuentes de verdad

Room conserva las notas guardadas. Preferences DataStore conserva la selección del tema. Los estados de pantalla son representaciones o borradores de esos datos, no almacenes alternativos permanentes.

## Modelo de nota

[Note.kt](../app/src/main/java/com/example/cleannotes/model/Note.kt) es un modelo Kotlin sin dependencias de Android:

| Campo | Tipo | Significado |
| --- | --- | --- |
| `id` | `Int?` | `null` antes de insertar; identificador de la fila después de guardar. |
| `title` | `String` | Título necesario para guardar. Un valor compuesto solo por espacios no es válido. |
| `content` | `String` | Texto de la nota. Puede estar vacío. |
| `timestamp` | `Long` | Instante de la última escritura realizada por el editor, en milisegundos. |
| `isReminder` | `Boolean` | Tipo explícito de la nota; por defecto `false`. |

No hay columnas independientes de creación, vencimiento, etiqueta o estado completado. El formato y las referencias a imágenes se guardan dentro del contenido versionado. El editor de recordatorios incluye cuerpo y configuración de notificación dentro del documento.

## Persistencia Room

- Base de datos: `notes_db`.
- Tabla: `note`.
- Versión actual: **3**.
- Esquema exportado: [3.json](../app/schemas/com.example.cleannotes.data.source.NoteDatabase/3.json).
- Orden: `timestamp DESC, id DESC`.
- Columna de tipo: `isReminder INTEGER NOT NULL DEFAULT 0`.

`NoteEntity` contiene las anotaciones de Room y los mapeos. `NoteDao` realiza las operaciones SQL. `NoteRepositoryImpl` valida el título y expone modelos `Note` a presentación.

### Contrato del repositorio

| Operación | Resultado |
| --- | --- |
| `observeNotes()` | `Flow<List<Note>>` ordenado por el DAO. |
| `getNote(id)` | Nota o `null` cuando no existe. |
| `saveNote(note)` | ID de la fila insertada o actualizada. |
| `deleteNote(note)` | Elimina la entidad correspondiente. |

Las escrituras de nota usan `@Insert(onConflict = REPLACE)`. Para actualizar se conserva el ID. Los guardados del editor están serializados con un `Mutex`. El repositorio no oculta las excepciones de almacenamiento: los ViewModels las traducen a estado de error.

## Migración de la versión 2 a la 3

La versión anterior infería el tipo de recordatorio si `content` era exactamente `Remember`. La versión 3 separa ese significado del texto.

La implementación de [NoteDatabase.kt](../app/src/main/java/com/example/cleannotes/data/source/NoteDatabase.kt) ejecuta:

```sql
ALTER TABLE note ADD COLUMN isReminder INTEGER NOT NULL DEFAULT 0;
UPDATE note SET isReminder = 1, content = '' WHERE content = 'Remember';
```

| Antes | Después |
| --- | --- |
| Fila con contenido `Remember` | Mismo ID, título y timestamp; `isReminder = true`, cuerpo vacío. |
| Otra fila | Mismo ID, título, contenido y timestamp; `isReminder = false`. |

La conversión mantiene el significado que daba la app anterior al marcador. No puede distinguir un recordatorio antiguo de una nota antigua cuyo contenido se hubiera guardado literalmente como `Remember`, porque antes no existía otro indicador.

Tras la migración, escribir `Remember` en una nota normal no cambia su tipo.

### Compatibilidad

| Base de datos de partida | Tratamiento |
| --- | --- |
| Sin base de datos | Room crea el esquema 3. |
| Esquema 2 | Se aplica `MIGRATION_2_3`. |
| Esquema 3 | Se abre sin transformación de versión. |
| Otra versión sin ruta registrada | No hay una migración implementada que garantice su apertura. |

La migración está registrada en `StorageModule`. No hay fallback de migración destructiva. En caso de una versión no soportada, hay que implementar y comprobar la transición antes de distribuir una actualización; no se debe borrar la base de datos como solución automática.

El repositorio exporta actualmente el esquema 3. La prueba de migración recrea la tabla anterior a partir de su estructura conocida; no se incluye un JSON histórico de la versión 2.

## Preferencias

[ThemeMode.kt](../app/src/main/java/com/example/cleannotes/model/ThemeMode.kt) define `SYSTEM`, `LIGHT` y `DARK`.

| Elemento | Valor |
| --- | --- |
| Nombre de DataStore | `user_preferences` |
| Clave | `theme` |
| Formato del valor | Nombre del enum como cadena. |
| Valor cuando falta la clave o no se reconoce | `SYSTEM` |
| Origen migrado | SharedPreferences `appearance`. |

`DataStoreUserPreferencesRepository` ofrece un flujo de tema y una operación suspendida de escritura. La instancia de DataStore se define una sola vez mediante la extensión de contexto y se proporciona con Hilt.

`SharedPreferencesMigration` traslada la preferencia del archivo anterior. Un fallo de lectura o escritura se comunica al estado de ajustes. Un error de escritura no debe interpretarse como confirmación de una nueva selección persistida.

## Borradores y restauración

`SavedStateHandle` conserva datos pequeños del editor:

- `draftTitle` y `draftContent`.
- `draftReminder`.
- `hasChanges`.
- `savedNoteId`, cuando ya se conoce la fila.

El ViewModel del cuaderno conserva `query` y `tab`. Navigation 3 conserva las claves de sus destinos y Compose conserva determinado estado visual de pestañas.

La restauración de estado depende del ciclo de vida de Android. No equivale a un historial de versiones ni a un archivo de recuperación permanente. La confirmación de persistencia de una nota corresponde a un guardado correcto en Room.

## Almacenamiento y copias del sistema

No hay un backend de la aplicación ni una función propia de sincronización. El manifiesto principal no declara permiso `INTERNET`. Compartir texto delega la entrega en otra aplicación elegida por la persona.

La configuración actual tiene `android:allowBackup="true"` y referencia archivos de reglas de backup y extracción. Esos archivos no implementan una política personalizada completa de inclusión/exclusión. Esta configuración permite la participación del sistema en copias o transferencias según su funcionamiento y ajustes; no es correcto prometer que los datos nunca puedan salir del dispositivo.

La app no añade cifrado de las notas a nivel de aplicación ni un mecanismo propio para exportar, importar o restaurar todo el cuaderno. No se ha validado el comportamiento de backup y restauración en dispositivos reales.

## Modificar el esquema

1. Define el cambio de modelo y entidad.
2. Incrementa la versión de Room.
3. Implementa y registra la migración desde las versiones que deban mantenerse.
4. Compila para exportar el nuevo JSON en `app/schemas/`.
5. Conserva los esquemas ya versionados.
6. Comprueba migración, lectura y escrituras posteriores con datos representativos.
7. Actualiza este documento y las pruebas.

[Índice de documentación](README.md).

## Contenido enriquecido

`NoteDocument` conserva texto, rangos de formato, nombres de imágenes, ajustes globales de espaciado, `subtitle` opcional y `reminder` opcional. `backgroundColor` se conserva para leer documentos anteriores pero no se aplica: toda nota sigue el tema. Se serializa como JSON precedido por un marcador de formato versión 1 dentro de `Note.content`; no cambia el esquema Room 3. El texto antiguo sin marcador se lee literalmente. Las tarjetas, búsqueda y compartir utilizan la proyección de texto legible.

Los adjuntos viven en `filesDir/note_images`. El documento conserva nombres relativos, no rutas absolutas ni permisos temporales externos. Un FileProvider no exportado concede acceso temporal a la cámara y a los TXT/PDF de `cacheDir/exports` al compartir. Las imágenes se normalizan a JPEG; la transparencia y los metadatos originales no se conservan. Se limita la resolución durante la importación para reducir uso de memoria.

No hay aún recolección automática de archivos huérfanos al eliminar una nota o quitar una imagen. No deben borrarse inmediatamente mientras puedan estar referenciados por un borrador o el historial de deshacer.


La carpeta adicional se guarda como URI SAF en `note_export`, con permiso persistente. Las configuraciones activas se indexan en `scheduled_reminders` para recuperar alarmas sin abrir Room durante un broadcast. Ninguna de estas adiciones cambia el esquema Room 3. El manifiesto declara POST_NOTIFICATIONS, SCHEDULE_EXACT_ALARM y RECEIVE_BOOT_COMPLETED; no solicita acceso global al almacenamiento.

`subtitle` se guarda en el documento con valor predeterminado vacío: no cambia Room ni extrae texto del cuerpo de las notas antiguas. La búsqueda y las exportaciones TXT/PDF lo incluyen. Deshacer/rehacer del cuerpo conserva los metadatos editados por separado.

La rama Figma añade campos opcionales `pinned`, `archived`, `reminderCompleted` y `checklist` al documento. Los documentos anteriores se leen con valores predeterminados sin cambiar Room 3. Las listas de casillas se buscan y exportan como texto; los ID de cada elemento se conservan al editar.

`createdAt` es opcional en documentos antiguos. Al guardar, el repositorio conserva primero el valor persistido, después usa el timestamp anterior como referencia para notas legadas, y finalmente el timestamp de inserción para notas nuevas. La agenda usa `createdAt ?: timestamp`; la columna `timestamp` continúa representando la última escritura.
