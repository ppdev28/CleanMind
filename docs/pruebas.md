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
| `EditNoteViewModelTest` | 13 | Autoguardados sobre la misma fila, salida inmediata, escritura lenta, fallos y reintento, guardado explícito de recordatorios, salida repetida, reversión del texto, título vacío, borrador restaurado, texto literal `Remember` y fallo de carga; creación de una nota en la carpeta seleccionada. |
| `NotesViewModelTest` | 9 | Filtros y búsqueda, reintento de carga, error de eliminación, detención de observación sin suscriptores y restauración de consulta/pestaña/carpeta, creación de carpetas y fallo/reintento de movimientos. |
| `SettingsViewModelTest` | 1 | Conservación del tema persistido cuando falla una escritura y recuperación al reintentar. |
| `NoteStorageTest` | 4 | Migración 2→4 con SQLite y operaciones de repositorio con IDs estables y orden de consulta. |
| `FolderStorageTest` | 3 | Migración 3→4 conservando todos los campos; creación/renombrado/borrado, movimientos y guardados obsoletos, duplicados con acentos, carpetas desaparecidas y separación de recordatorios. |
| `SampleNoteCollectionTest` | 3 | Seis estrofas breves, importación repetida y reintento parcial sin duplicar, conservación de notas/metadatos y reutilización de carpetas existentes. |
| `UserPreferencesRepositoryTest` | 1 | Migración del tema anterior a DataStore y conservación de una nueva selección al reabrir. |
| `DestinationTest` | 1 | Serialización y deserialización de destinos con argumentos tipados. |
| `DateUtilsTest` | 2 | Agrupación de fechas de años distintos y detección de hoy/ayer. |
| `RichEditorTest` | 12 | Compatibilidad con texto antiguo, serialización, reapertura con negrita, deshacer/rehacer, limpieza parcial, listas, casillas, espaciado, color con selección conservada y estado de la barra y continuación/salida de listas con Intro, subtítulos y conservación de metadatos al deshacer y disponibilidad del campo nativo para accesibilidad. |
| `MarkdownCompatibilityTest` | 5 | Documentos normales sin cambios, lectura de documentos Markdown como texto con formato conservando metadatos y destinos de enlaces, estilos/tablas/tareas anteriores, HTML literal y texto legible para buscar/compartir. |
| `ExampleUnitTest` | 1 | Prueba aritmética de ejemplo del proyecto; no valida funcionalidad de Clean Mind. |
| `ReminderExportTest` | 5 | Repetición y serialización, programación/recuperación/cancelación de alarmas, TXT UTF-8 y paginación visual de exportación. |
| `DesignUiTest` | 20 | Capturas locales, privacidad de previews, compartir con pulsación larga, búsqueda/cuadrícula, concentración, completado, ajustes, selección de fecha/hora y formulario independiente con confirmación al descartar y filtrado/movimiento de notas entre carpetas y exclusión de Volver en destinos ocultos, carpetas en cuadrícula y ausencia de búsqueda en Notas y botones del editor sin palabras partidas ni truncadas a 320 dp con fuente 1.35; ocultación al descender, permanencia oculta al invertir el scroll a mitad de la lista y reapertura al llegar al inicio, conservando los límites del contenido fuera de las barras. |

Total actual: **80 pruebas locales**, de las cuales una es el ejemplo aritmético original. Esta cifra describe la suite actual y debe revisarse al añadir o retirar casos; no es un porcentaje de cobertura de código.

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

La validación de la revisión de «To Dos» se documenta al final de esta página. Lint conserva avisos sobre versiones de dependencias y recursos.

La comprobación de formato de Android Studio usa el estilo compartido; la revisión nueva abarca 98 archivos.

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

## Material 3 y contraste · 25 de noviembre de 2026

La revisión adopta la alternativa Material 3 pedida por el usuario. Se retiraron Haze y los efectos de cristal simulados. La compilación debug/release y lint de ambas variantes finalizaron correctamente, con las **69 pruebas locales** superadas. La nueva regresión recorre una agenda larga, comprueba que la cabecera se oculta y reaparece y conserva visible la acción Crear.

En el realme RMX3370, API 33, se revisaron el fondo negro uniforme, el contraste de texto, la fecha de la agenda, la cabecera al desplazar y la permanencia de la barra inferior. Se eliminó un recorte redundante en las superficies que interfería con su dibujo en hardware. La franja de estado protege el reloj al pasar contenido por debajo.

Se abrieron y cerraron las hojas de creación, carpetas, formato e imágenes. Su contenedor se prolonga hasta el borde inferior, cubriendo el área de navegación en lugar de dejar ver la barra de la app. Se comprobó también el tema claro y se restauró la preferencia previa Oscuro. Esta revisión física utiliza navegación por gestos; no se cambió la configuración de navegación del teléfono.

La comparación final conservó idénticos los 11 registros de notas y recordatorios y la carpeta que había al comenzar esta revisión. Las capturas de `ui-preview/` se regeneraron con datos sintéticos; las capturas del dispositivo y sus copias de datos se conservan solo como material privado local.

## Barras fijas · 26 de noviembre de 2026

La barra inferior se cambia a `NavigationBar` Material 3 de ancho completo, acoplada al borde y con insets de sistema propios. Las cabeceras del cuaderno, pantallas con Volver y editor usan un fondo opaco del tema. Se retira `enterAlways` y el contenido del cuaderno reserva el espacio de ambas barras mediante el padding de `Scaffold`, sin duplicarlo dentro de las listas.

La regresión de cabecera comprueba ahora que el título permanece visible al desplazar en ambos sentidos, que Crear sigue accesible y que los límites de los viewports de agenda y cuadrícula no invaden las barras. Esta revisión sustituye el comportamiento de ocultación descrito para el día 25. No había ningún teléfono conectado por ADB; se verifica localmente y no se afirma una nueva instalación o prueba física.

Resultado local: compilaciones debug y release, lint de ambas variantes y las 69 pruebas correctas, sin fallos ni omisiones. Se regeneraron las capturas sintéticas con las barras fijas.

## Cabecera que reaparece al inicio · 26 de noviembre de 2026

La cabecera de las pestañas del cuaderno usa ahora `exitUntilCollapsed`, conservando el fondo opaco. Se oculta al descender y no reaparece con un pequeño desplazamiento de vuelta mientras la lista siga lejos del principio. El padding de `Scaffold` se adapta a su altura sin permitir que el contenido se superponga. La barra inferior sigue acoplada al borde.

La regresión de UI comprueba una agenda larga: cabecera visible al empezar, oculta tras descender, todavía oculta al invertir brevemente el desplazamiento con posición mayor que cero y visible de nuevo con la posición de scroll a cero. También comprueba el colapso en la cuadrícula y que Crear permanece visible. Esta revisión sustituye la cabecera siempre fija del apartado anterior.

Resultado: compilaciones debug/release, lint de ambas variantes y las 69 pruebas locales correctas. Se instaló el APK debug en el realme mediante ADB inalámbrico. No se completó la comprobación manual de scroll al encontrarse el editor con una nota abierta; no se registra como prueba física superada.


## Nota «To Dos» · 26 de noviembre de 2026

Se comprobaron los seis puntos en orden: retirada de pantallas y recursos sin referencias; FAB con creación y navegación; doble toque en Buscar con campo enfocado; Markdown y vista previa; formato visible y panel único; cuadrícula de Inicio con vuelta a agenda y separadores por creación.

La regresión de scroll sigue comprobando ocultación al bajar y reapertura solo al llegar al inicio. Las pruebas nuevas comprueban el foco directo de búsqueda, la distribución real en dos columnas y su cambio a agenda, la ausencia de contenido privado en tarjetas y la alternancia de vista previa sin cambios de documento. Se mantienen las pruebas de editor nativo, almacenamiento, recordatorios y exportación.

El realme estuvo conectado al leer «To Dos» mediante una copia privada de la base. Después dejó de aparecer tanto en `adb devices` como en el descubrimiento mDNS. No se instaló ni se probó físicamente esta revisión, ni se modificó la nota original. Las imágenes actualizadas de `docs/ui-preview/` son capturas sintéticas de Robolectric, con datos de prueba.

Resultado final: **80 pruebas locales superadas**, sin fallos, errores ni omisiones. `assembleDebug`, `assembleRelease`, `lintDebug` y `lintRelease` correctos. El formateador de Android Studio revisó 98 archivos. Se corrigió el helper de capturas JVM para invalidar las capas cacheadas de Compose antes de dibujar en Canvas; esto evita documentar capturas incompletas y no cambia el código de la app.


## Correcciones: editor normal, FAB anclado y cuadrícula

El editor vuelve al formato visual anterior. Se mantiene lectura de documentos Markdown existentes y se comprueba que abrirlos no altera el contenido almacenado. La prueba de UI verifica la ausencia de controles Markdown y la disponibilidad del formato normal.

El FAB presenta sus seis acciones en la propia pantalla. La regresión comprueba que aparecen encima del botón, que este conserva su posición, que no hay un diálogo y que Volver cierra el menú. Se conservan las dos acciones de creación y la búsqueda con doble toque.

La regresión de Home usa ahora un ancho de 360 dp, el ancho lógico del realme (1080 px y 480 dpi), y verifica con coordenadas la distribución en dos columnas y el cambio real a una columna. El tamaño anterior de celda de 156 dp solo permitía una columna en ese dispositivo; pasa a 128 dp.

Resultado: **79 pruebas locales superadas**, sin fallos ni omisiones; compilaciones debug/release y lint de ambas variantes correctos. Se instaló el APK en el realme RMX3370 mediante USB y se verificaron la cuadrícula (tarjetas de 450 px) frente a agenda (936 px), el despliegue anclado al FAB, su cierre con Volver, la creación de un borrador vacío con editor normal, la apertura del formato visual y el foco de búsqueda con doble toque. El borrador se cerró sin contenido. La comparación de las 12 notas y la carpeta antes y después confirmó que todos sus campos permanecen idénticos. Las capturas del repositorio siguen siendo sintéticas; las capturas privadas del teléfono no se publican.


## Menú de formato, eliminación de casillas y FAB radial

La fila visible y el panel único se restauran sobre el editor normal. La regresión comprueba la disponibilidad directa de negrita y del panel con H6, sin controles Markdown. La prueba de casillas elimina una fila normal, la recupera con deshacer y elimina filas marcadas y vacías, conservando cuerpo, rangos y metadatos.

La prueba del FAB usa 320 dp de ancho, comprueba los radios de 108 y 216 dp, la ausencia de solapamientos entre los seis objetivos táctiles y la posición estable del botón principal. Se mantienen navegación, ambas opciones de creación, cierre con Volver y foco de búsqueda con doble toque.

Resultado: **80 pruebas locales superadas**, sin fallos ni omisiones; compilaciones debug/release y lint de ambas variantes correctos. El formateador revisó 98 archivos. En el realme RMX3370 se comprobó la distribución radial sin solapamientos, la aplicación y persistencia de negrita en el cuerpo normal, el panel nuevo con H6 y la eliminación de casillas marcadas y vacías. Deshacer recuperó sus identificadores y estado; eliminar todas conservó el cuerpo y la negrita. Se eliminó la nota exclusiva de prueba y la comparación de las 12 notas originales y la carpeta confirmó todos sus campos intactos. Las imágenes del repositorio son capturas sintéticas.

## Integración de IA (28 de noviembre de 2026)

Rama `feat/ai-integration`, creada desde `feat/figma-complete-ui`. La suite ampliada incluye **106 pruebas JVM/Robolectric**, 26 nuevas para validaciones, API HTTPS con MockWebServer, ajustes, cancelación, privacidad del payload, propuestas, aceptación explícita, protección frente a borradores antiguos y recuperación de errores al guardar. Se mantiene la cobertura existente del cuaderno y el editor.

Se han comprobado `testDebugUnitTest`, `assembleDebug`, `lintDebug` y la compilación/empaquetado de las pruebas instrumentadas. Lint no añade avisos en los archivos nuevos de IA; los avisos existentes de la base continúan.

`EncryptedAiSettingsTest` se ha ejecutado correctamente en un emulador Android API 35, con Android Keystore real: configuración cifrada, lectura desde otra instancia, IV diferente al volver a guardar, error explícito ante corrupción y borrado/recuperación. Utiliza un directorio de prueba aislado, sin alterar la configuración de uso de la app.

No se ha llamado a una API externa con credenciales reales. La prueba desde **Ajustes → Inteligencia artificial → Probar conexión** permite validar la cuenta y el modelo configurados. El contrato, los límites y los comandos están en [IA](ia.md).

## Prueba de la rama de IA en Xiaomi

Se compiló `feat/ai-integration` y se instaló su APK debug en el Xiaomi Redmi Note 8 Pro conectado, Android API 36, sin desinstalar ni borrar datos. Pasaron las **26 pruebas locales específicas de IA**, lint debug y la prueba instrumentada `EncryptedAiSettingsTest` sobre el Android Keystore real. Esta última usa un archivo temporal independiente, verifica persistencia cifrada, IV nuevo, detección de corrupción y borrado, y retira sus datos al terminar.

Se abrió Ajustes en la sección de IA y se comprobó el campo enmascarado. La comparación tras la actualización conservó todos los campos de las **10 notas y 2 carpetas originales**. No se enviaron notas ni credenciales reales a un proveedor durante estas comprobaciones. La prueba con una cuenta real requiere introducir la configuración del proveedor.

Esta rama parte de `7a4b43a`, anterior a las revisiones posteriores de navegación, temas y barra de formato; su interfaz refleja esa base.
