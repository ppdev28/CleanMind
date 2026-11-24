# Pruebas y verificación

## Comprobación local

Desde la raíz:

```bash
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
```

Este comando compila el APK, ejecuta análisis estático y lanza las pruebas JVM. No instala ni abre la app en un teléfono o emulador. En Windows utiliza `gradlew.bat`.

La primera ejecución puede descargar dependencias y los componentes de Android que utiliza Robolectric para sus pruebas en el ordenador.

## Ejecutar una parte

```bash
# Todas las pruebas locales de debug.
./gradlew :app:testDebugUnitTest

# Solo el editor.
./gradlew :app:testDebugUnitTest --tests 'com.example.cleannotes.presentation.EditNoteViewModelTest'

# Solo almacenamiento y preferencias.
./gradlew :app:testDebugUnitTest --tests 'com.example.cleannotes.data.*'

# Solo análisis estático.
./gradlew :app:lintDebug
```

Los filtros mostrados corresponden a las clases presentes en el repositorio.

## Cobertura implementada

| Clase | Casos | Comportamiento comprobado |
| --- | --- | --- |
| `EditNoteViewModelTest` | 12 | Autoguardados sobre la misma fila, salida inmediata, escritura lenta, fallos y reintento, guardado explícito de recordatorios, salida repetida, reversión del texto, título vacío, borrador restaurado, texto literal `Remember` y fallo de carga; creación de una nota en la carpeta seleccionada. |
| `NotesViewModelTest` | 9 | Filtros y búsqueda, reintento de carga, error de eliminación, detención de observación sin suscriptores y restauración de consulta/pestaña/carpeta, creación de carpetas y fallo/reintento de movimientos. |
| `SettingsViewModelTest` | 1 | Conservación del tema persistido cuando falla una escritura y recuperación al reintentar. |
| `NoteStorageTest` | 4 | Migración 2→4 con SQLite y operaciones de repositorio con IDs estables y orden de consulta. |
| `FolderStorageTest` | 3 | Migración 3→4 conservando todos los campos; creación/renombrado/borrado, movimientos y guardados obsoletos, duplicados con acentos, carpetas desaparecidas y separación de recordatorios. |
| `SampleNoteCollectionTest` | 3 | Seis estrofas breves, importación repetida y reintento parcial sin duplicar, conservación de notas/metadatos y reutilización de carpetas existentes. |
| `UserPreferencesRepositoryTest` | 1 | Migración del tema anterior a DataStore y conservación de una nueva selección al reabrir. |
| `DestinationTest` | 1 | Serialización y deserialización de destinos con argumentos tipados. |
| `DateUtilsTest` | 2 | Agrupación de fechas de años distintos y detección de hoy/ayer. |
| `RichEditorTest` | 12 | Compatibilidad con texto antiguo, serialización, reapertura con negrita, deshacer/rehacer, limpieza parcial, listas, casillas, espaciado, color con selección conservada y estado de la barra y continuación/salida de listas con Intro, subtítulos y conservación de metadatos al deshacer y disponibilidad del campo nativo para accesibilidad. |
| `ExampleUnitTest` | 1 | Prueba aritmética de ejemplo del proyecto; no valida funcionalidad de Clean Mind. |
| `ReminderExportTest` | 5 | Repetición y serialización, programación/recuperación/cancelación de alarmas, TXT UTF-8 y paginación visual de exportación. |
| `DesignUiTest` | 14 | Capturas locales, privacidad de previews, compartir con pulsación larga, búsqueda/cuadrícula, concentración, completado, ajustes, selección de fecha/hora y formulario independiente con confirmación al descartar y filtrado/movimiento de notas entre carpetas y exclusión de Volver en destinos ocultos, carpetas en cuadrícula y ausencia de búsqueda en Notas y botones del editor sin palabras partidas ni truncadas a 320 dp con fuente 1.35. |

Total actual: **68 pruebas locales**, de las cuales una es el ejemplo aritmético original. Esta cifra describe la suite actual y debe revisarse al añadir o retirar casos; no es un porcentaje de cobertura de código.

Las pruebas de ViewModel usan `kotlinx-coroutines-test` para controlar la ejecución y el tiempo virtual. `FakeNoteRepository` permite simular lentitud, errores y cambios observables.

Las pruebas de integración de almacenamiento usan Robolectric con API 28 y una `Application` de prueba. No arrancan un dispositivo, no comprueban toda la interfaz y no sustituyen una prueba de migración en las versiones Android de destino.

## Informes

| Resultado | Ubicación |
| --- | --- |
| Informe HTML de pruebas | `app/build/reports/tests/testDebugUnitTest/index.html` |
| Resultados XML | `app/build/test-results/testDebugUnitTest/` |
| Informe HTML de lint | `app/build/reports/lint-results-debug.html` |
| APK de debug | `app/build/outputs/apk/debug/app-debug.apk` |

Las carpetas `build/` contienen salidas generadas. No forman parte de la documentación permanente ni deben usarse como fuente de código.

## Estado conocido

La última ejecución local de verificación de código terminó con compilación debug correcta, lint sin errores y 68 pruebas locales superadas, sin fallos ni casos omitidos. Lint conserva avisos sobre versiones de dependencias y recursos.

La comprobación de formato de Android Studio terminó con 101 archivos revisados y conformes al estilo compartido.

## Validación en Xiaomi · 24 de noviembre de 2026

Se actualizó la instalación debug existente en un Redmi Note 8 Pro, Android API 36, 1080×2340 y escala de texto 1.0. Se guardó una copia local privada antes de la prueba y se compararon los registros originales después: permanecen idénticos. Las notas, recordatorios, archivos TXT y PDF creados exclusivamente para esta prueba se eliminaron al terminar.

Resultados observados:

- Arranque con el nombre Clean Mind y actualización sin borrar datos.
- Hoja del botón central con Nueva nota y Nuevo recordatorio.
- Creación y autoguardado de una nota con título, subtítulo y cuerpo; Home muestra título/subtítulo bajo el separador con fecha, sin exponer el cuerpo.
- Formulario de tarea, fecha y hora con selectores nativos, guardado explícito y lista compacta de recordatorios.
- Notificación del sistema recibida con la app en segundo plano; se permitió el acceso de alarmas exactas en Ajustes de Android y se conserva habilitado.
- Completado de la tarea y retirada de la lista de pendientes.
- Pulsación larga desde Home, hoja de compartir y apertura/cierre del selector de Android para texto y PDF, sin enviar contenido a aplicaciones externas.
- PDF generado por Android, válido, de una página, con título, subtítulo y cuerpo comprobados mediante extracción de texto.
- Apariencia oscura y cambio a claro desde Ajustes; restauración de la preferencia previa de seguir el sistema.
- Ningún fallo del paquete en el registro de crashes conservado por Android durante la revisión.

Esta prueba comprueba una notificación real; no certifica la entrega bajo todas las políticas de batería de MIUI ni tras una detención forzada.

## Ampliación: carpetas, splash y movimiento

En el mismo Xiaomi se comprobó la actualización real del esquema 3 al 4. La comparación de los campos anteriores confirmó que los registros originales permanecen idénticos, con pertenencia inicial nula.

Se creó una carpeta y una nota dentro de ella, se renombró, se movió la nota a Sin carpeta y de vuelta y se eliminó la carpeta conservando el contenido. Se comprobó entrar/salir de concentración y se eliminaron al terminar la nota, la carpeta y su TXT de prueba. El arranque en frío se grabó localmente para revisar la pluma monocroma, sin publicar contenido del dispositivo. Se repitieron navegación, vuelta desde Ajustes y cierre del formulario de recordatorio vacío con la escala de animación normal y desactivada. La configuración original se restauró y el teléfono quedó en Inicio. El registro de crashes no contiene fallos del paquete durante la revisión.

## Validación pendiente

- Entrega a aplicaciones receptoras reales desde el selector de compartir.
- Otras escalas de texto, tamaños de ventana, multiventana y fabricantes adicionales; la rotación, recreación y escala 1.35 se revisaron en el realme.
- Sesión completa de accesibilidad con lector de pantalla; se verificó el campo nativo del editor en el árbol de accesibilidad y se añadió su regresión.
- Permisos denegados, reinicio, restricciones de batería y repeticiones durante periodos largos en hardware real.
- Migración desde una instalación real de la base de datos versión 2, backup y restauración del sistema.

`app/src/androidTest/` contiene dos pruebas instrumentadas: el identificador del paquete y el pegado de una URI de imagen desde el portapapeles real a `RichNoteEditor`. La segunda crea una imagen temporal propia, restaura el portapapeles en `finally` y no modifica notas. No constituyen una suite completa de navegación.

## Añadir pruebas útiles

Añade casos cuando cambie una regla de comportamiento o exista un riesgo de regresión: concurrencia de guardado, transformación de datos, compatibilidad o tratamiento de errores. Para pantallas que solo cambien texto o estilo, evita pruebas que se limiten a duplicar la implementación.

Cuando modifiques la entidad Room, comprueba además que una base migrada permite editar, insertar y borrar; validar solo que se añade una columna no cubre el uso posterior.

[Índice de documentación](README.md).

La composición del PDF se comprueba con Canvas nativo sobre bitmaps, revisando páginas de una nota larga. Robolectric no implementa aquí la creación nativa de `PdfDocument`; la escritura del contenedor final se comprobó además en el Xiaomi. El selector SAF se comprobó posteriormente en el realme, tanto para archivos individuales como para una carpeta de copias TXT.

El rediseño, sus capturas y las referencias que no se pudieron contrastar se documentan en [UI de Figma](figma-ui.md).

Las pruebas de almacenamiento comprueban que la fecha de creación no cambia al editar y que las notas antiguas utilizan la fecha persistida. Las pruebas de UI verifican la agrupación por creación aunque una nota se haya editado hoy, las dos acciones del botón central y los ajustes visibles sin desplegables.

## Revisión de cristal y cuadrícula

Compilación debug y release, lint debug y las 66 pruebas locales han finalizado correctamente. Se verificó además el contenido de ambos APK: `science-notes.json` se incluye únicamente en debug. Las pruebas de UI renderizan el acabado translúcido sin GPU; no certifican el desenfoque acelerado de Haze.

Se obtuvo una copia privada del realme (Android API 33) antes de efectuar cambios. Al perder la conexión ADB se omitieron la instalación y las pruebas de teléfono, como indicó el usuario. Esta situación se resolvió después al recuperar la conexión USB; la validación real se describe a continuación.

[Material de cristal](glass-ui.md) · [Colección de ciencia](ciencia-breve.md).

## Validación en realme · 24 de noviembre de 2026

Dispositivo RMX3370, Android API 33, 1080×2400, conectado por USB. Se conservó una copia privada previa y se actualizó la instalación existente sin borrar datos. Se utilizaron notas, carpetas, imágenes y exportaciones identificadas como QA para las operaciones de edición y eliminación.

Comprobaciones realizadas:

- Importación de las seis notas científicas con sus textos completos en Ciencia y conservación exacta de los cuatro registros originales tras la actualización.
- Cristal acelerado en hardware con apariencia clara y oscura; restauración de la preferencia Oscuro. Cuadrícula de carpetas, ausencia de búsqueda en Notas y búsqueda global por una palabra del cuerpo.
- Creación dentro de una carpeta, título/subtítulo/cuerpo, autoguardado, continuación y salida de listas numeradas con Intro, negrita, deshacer/rehacer, concentración y reapertura. Guardado de interlineado, separación de párrafos y espacio extra entre líneas.
- Se corrigió la exclusión del cuerpo nativo del árbol de accesibilidad: Android 13 expone ahora los tres campos editables. Esto no sustituye una sesión completa con TalkBack.
- Selección de una imagen sintética mediante el selector del sistema, captura real con la cámara y cancelación sin cambios; adjuntos guardados. Dos pruebas instrumentadas superadas, incluida la entrega de una URI de imagen desde el menú Pegar del portapapeles nativo al callback de inserción. La prueba instrumentada verifica esa URI; la importación y persistencia de imágenes se comprobaron mediante galería y cámara.
- Apertura y cancelación del selector de compartir de Android para texto y PDF, sin envío a otras aplicaciones. Guardado de ambos formatos mediante SAF. TXT comparado con el texto esperado.
- PDF nativo de dos páginas: extracción de título, subtítulo y cuerpo, dos imágenes y revisión visual de ambas páginas renderizadas con Poppler.
- Selección de una carpeta mediante SAF, copia de las 11 entradas existentes y actualización del mismo TXT mediante autoguardado; restauración de la configuración inicial sin carpeta adicional.
- Creación y renombrado de carpeta, movimientos de ida y vuelta, eliminación de carpeta conservando la nota en Sin carpeta y conservación de su ID y fecha de creación. Fijar/desfijar, archivar/desarchivar, filtros y cuatro cambios entre lista y cuadrícula sin fallos.
- Recordatorio con fecha/hora nativas, repetición por minutos y mensaje propio. Notificación recibida con la app en segundo plano; acción de posponer diez minutos observada en Android, completado que avanza la siguiente repetición y eliminación que retira su programación. Se habilitaron las notificaciones de Clean Mind; el permiso de alarmas exactas ya estaba activado y se conservó así. El registro de ciclo de vida confirma que MainActivity estaba detenida cuando se observó la notificación y no se reanudó hasta después. La entrega sigue sujeta a las políticas de batería y alarmas de realme.
- Rotación horizontal y regreso a vertical; cuadrícula de carpetas, editor y navegación con escala de fuente 1.35. Se corrigieron los botones del editor para colocar icono y texto en vertical con fuente grande, evitando palabras partidas. La regresión reproduce el fallo previo a 320 dp y comprueba los tres nombres completos después.
- Navegación desde Ajustes y cierre de recordatorio vacío con las tres escalas de animación a cero, activadas desde Accesibilidad del sistema. Se restauraron las escalas originales a 1, la fuente a 0.9 y la rotación bloqueada vertical.

- Arranque desde proceso detenido, tras guardar los borradores y comprobar que no había alarmas de usuario pendientes en esta instalación. Los datos persistieron y las muestras no se duplicaron.
- Limpieza de la nota, carpeta, aviso, adjuntos, imagen de galería, copias y exportaciones temporales, además del APK de pruebas. La comparación final conserva idénticos los cuatro registros originales y mantiene exactamente las seis notas científicas. No aparecen crashes ni ANR del paquete en los registros conservados durante la validación. El teléfono queda en Inicio, con su tema Oscuro y sus preferencias de pantalla restauradas.

La compilación debug y release, lint debug y release, las 68 pruebas locales y la repetición final de las dos instrumentadas han pasado. El APK release continúa sin contener el archivo científico de desarrollo.
