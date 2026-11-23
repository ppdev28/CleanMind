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
| `EditNoteViewModelTest` | 11 | Autoguardados sobre la misma fila, salida inmediata, escritura lenta, fallos y reintento, tipo de recordatorio, salida repetida, reversión del texto, título vacío, borrador restaurado, texto literal `Remember` y fallo de carga. |
| `NotesViewModelTest` | 7 | Filtros y búsqueda, reintento de carga, error de eliminación, detención de observación sin suscriptores y restauración de consulta/pestaña. |
| `SettingsViewModelTest` | 1 | Conservación del tema persistido cuando falla una escritura y recuperación al reintentar. |
| `NoteStorageTest` | 2 | Migración 2→3 con SQLite y operaciones de repositorio con IDs estables y orden de consulta. |
| `UserPreferencesRepositoryTest` | 1 | Migración del tema anterior a DataStore y conservación de una nueva selección al reabrir. |
| `DestinationTest` | 1 | Serialización y deserialización de destinos con argumentos tipados. |
| `DateUtilsTest` | 2 | Agrupación de fechas de años distintos y detección de hoy/ayer. |
| `RichEditorTest` | 11 | Compatibilidad con texto antiguo, serialización, reapertura con negrita, deshacer/rehacer, limpieza parcial, listas, casillas, espaciado, color con selección conservada y estado de la barra y continuación/salida de listas con Intro, subtítulos y conservación de metadatos al deshacer. |
| `ExampleUnitTest` | 1 | Prueba aritmética de ejemplo del proyecto; no valida funcionalidad de Clean Notes. |

| `ReminderExportTest` | 5 | Repetición y serialización, programación/recuperación/cancelación de alarmas, TXT UTF-8 y paginación visual de exportación. |

| `DesignUiTest` | 7 | Capturas locales, privacidad de previews, compartir con pulsación larga, búsqueda/cuadrícula, concentración, completado, ajustes y selección de fecha/hora. |

Total actual: **49 pruebas locales**, de las cuales una es el ejemplo aritmético original. Esta cifra describe la suite actual y debe revisarse al añadir o retirar casos; no es un porcentaje de cobertura de código.

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

La última ejecución local de verificación de código terminó con compilación debug correcta, lint sin errores y 49 pruebas locales superadas, sin fallos ni casos omitidos. Lint conserva avisos sobre versiones de dependencias y recursos.

Los cambios de esta tarea son de documentación. No se ha repetido la suite por modificar texto; se comprueban rutas, enlaces locales y consistencia con la implementación.

## Validación pendiente en Android

Las siguientes comprobaciones son una lista para una futura sesión autorizada, no resultados obtenidos:

- Creación, reapertura y eliminación desde lista y cuadrícula.
- Autoguardado y salida del editor con teclado abierto, gestos y botón de volver.
- Comportamiento de la hoja de compartir con aplicaciones receptoras reales.
- Estado de pestañas, consulta y borradores tras rotación y recreación del proceso.
- Tema claro, oscuro y del sistema al reabrir la aplicación.
- Accesibilidad con lector de pantalla, texto ampliado y diferentes tamaños de ventana.
- Migración con una instalación real que contenga una base de datos de versión 2.
- Backup y restauración del sistema.

El archivo en `app/src/androidTest/` contiene la prueba instrumentada de ejemplo del identificador de paquete. No constituye una suite funcional de la app. Las tareas de pruebas instrumentadas y las acciones de instalación quedan fuera del flujo local descrito arriba.

## Añadir pruebas útiles

Añade casos cuando cambie una regla de comportamiento o exista un riesgo de regresión: concurrencia de guardado, transformación de datos, compatibilidad o tratamiento de errores. Para pantallas que solo cambien texto o estilo, evita pruebas que se limiten a duplicar la implementación.

Cuando modifiques la entidad Room, comprueba además que una base migrada permite editar, insertar y borrar; validar solo que se añade una columna no cubre el uso posterior.

[Índice de documentación](README.md).

La composición del PDF se comprueba con Canvas nativo sobre bitmaps, revisando páginas de una nota larga. Robolectric no implementa aquí la creación nativa de `PdfDocument`: la escritura final del contenedor PDF, el selector SAF, los permisos reales, las notificaciones con la app cerrada y la hoja de compartir se revisarán en Android cuando se autorice usar el dispositivo. No se ha instalado ni ejecutado la app en teléfono.

El rediseño, sus capturas y las referencias que no se pudieron contrastar se documentan en [UI de Figma](figma-ui.md).
