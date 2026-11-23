# Mantenimiento y ampliación

## Orientación

Conserva la separación entre estado, representación y almacenamiento. Antes de añadir una abstracción o dependencia, identifica el problema que resuelve en esta aplicación.

Los cambios de comportamiento deben quedar reflejados en las pruebas pertinentes y en la documentación. Los archivos generados por Gradle y KSP no se editan manualmente.

## Añadir una funcionalidad

| Cambio | Lugares a revisar |
| --- | --- |
| Un dato nuevo en una nota | `model/Note`, `NoteEntity`, mapeos, Room, migración, repositorio, UI y pruebas de almacenamiento. |
| Una regla de edición | `EditNoteUiState`, `EditNoteViewModel`, eventos, pantalla y pruebas del editor. |
| Una nueva preferencia | Contrato y repositorio de preferencias, modelo, `SettingsViewModel`, UI y prueba de persistencia. |
| Un destino independiente | `Destination`, enlaces de `NotesApp`, pantalla y prueba de serialización. |
| Una vista del mismo cuaderno | Estado de `NotesViewModel`, pestañas, `NotebookScreen` y barra inferior. |
| Colores o tipografía | `presentation/ui/theme/` y revisión de contraste y tamaños. |
| Texto visible | `app/src/main/res/values/strings.xml` y guía de uso si afecta al recorrido. |

## Pantallas y estado

Las pantallas reciben `UiState` y callbacks; no deben construir bases de datos ni consultar preferencias directamente. Coloca la obtención de ViewModels y la conexión de navegación en el nivel de rutas.

Mantén en Compose el estado puramente visual y pequeño, como un menú abierto. Conserva en un ViewModel las decisiones que dependen de datos o deben probarse de forma independiente. Usa `SavedStateHandle` para los datos de restauración adecuados y Room o DataStore para persistencia permanente.

No pases contenido completo de notas por navegación ni guardes callbacks de Activity o pantalla en ViewModels. Conserva la representación de la finalización del editor como estado observable.

## Cambios de almacenamiento

Sigue el procedimiento de [Datos y migraciones](datos-y-migraciones.md). Conserva identificadores al editar y mantén las migraciones ya soportadas.

Si incorporas sincronización, define antes las reglas de identidad, conflictos, borrado y funcionamiento sin conexión. Si incorporas exportación, decide qué datos incluye y cómo se valida una importación. Estos flujos pueden justificar casos de uso de dominio cuando su lógica sea compleja o compartida.

## Actualizar dependencias

1. Modifica el catálogo de versiones.
2. Revisa la compatibilidad entre Kotlin, AGP, Gradle, Compose, KSP y Hilt para el cambio concreto.
3. Sincroniza y ejecuta las comprobaciones locales de [Pruebas](pruebas.md).
4. Revisa las migraciones y los esquemas si cambia Room o el modelo persistido.
5. Actualiza [Desarrollo](desarrollo.md) si cambian SDK, JVM o tareas.

Los avisos de nuevas versiones no implican por sí solos que la app tenga un fallo. Evalúa la actualización de acuerdo con los cambios necesarios y su compatibilidad.

## Mantener la documentación

| Si cambia… | Actualiza… |
| --- | --- |
| Función disponible o límite de producto | [README principal](../README.md) y [Guía de uso](guia-de-uso.md). |
| Preparación o distribución | [Desarrollo](desarrollo.md). |
| Capas, navegación o ámbito de estado | [Arquitectura](arquitectura.md). |
| Modelo, esquema o preferencias | [Datos y migraciones](datos-y-migraciones.md). |
| Casos de prueba o informes | [Pruebas](pruebas.md). |

Los enlaces del repositorio deben ser relativos. No incluyas rutas personales del SDK, credenciales ni instrucciones que dependan del equipo de una persona. No declares verificada una función en dispositivo si solo se ha comprobado con pruebas JVM o lectura de código.

Las capturas actuales de `screenshots/` son históricas. Cuando se autorice una nueva sesión de revisión visual, podrán reemplazarse por imágenes del diseño actual y actualizarse sus referencias.

## Preparar una distribución

Antes de distribuir una actualización, verifica que la versión de app sea la adecuada, exista una configuración de firma release, las migraciones cubran las instalaciones soportadas y se hayan completado las comprobaciones en Android necesarias para el cambio.

El workflow manual actual genera artefactos; no publica en una tienda. No hay un archivo `LICENSE` en el repositorio en el estado revisado; la documentación no asigna una licencia nueva.

[Índice de documentación](README.md).
