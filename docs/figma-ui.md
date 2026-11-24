# UI de Figma

Rama de trabajo: `feat/figma-complete-ui`.

Referencia: [Clean Notes, archivo de Figma](https://www.figma.com/design/qokv6yTT5e72bxduDmq7AV/Sin-t%C3%ADtulo).

## Implementación

- Inicio con búsqueda, captura rápida y agenda vertical: fecha, separador horizontal, día/mes en el lateral y todas las notas creadas en ese día. No se limita a diez notas.
- Barras anteriores recuperadas: icono de la app y mayor separación superior; Inicio, Notas, «+» central, Recordar y Buscar en la barra inferior. Se mantienen colores y tipografía del estilo actual.
- Filtros Todas, Fijadas, Con imágenes y Archivadas; vistas de lista y cuadrícula.
- Tarjetas de título, subtítulo y metadatos. Se mantiene la petición anterior de ocultar el cuerpo en Home: las vistas previas de la referencia reciben el subtítulo independiente, no una extracción del contenido.
- Editor con guardado visible, fijado, archivo, deshacer archivo, captura de listas/imágenes y hoja de recordatorio. Las herramientas de formato, compartir y exportar permanecen disponibles.
- Concentración sin herramientas inferiores; volver cierra este modo antes de abandonar la nota.
- Listas de casillas editables, con estado independiente por elemento, además de las listas de texto ya existentes.
- Recordatorios con opciones rápidas Más tarde y Mañana, fecha/hora personalizadas, configuración avanzada, posposición y completado sin eliminar la nota. Una recurrencia completada avanza a la siguiente ocurrencia.
- Ajustes anteriores recuperados: apariencia y carpeta de copias visibles directamente, información de la app y enlace a notificaciones de Android.

El título del editor es opcional: al persistir, se utiliza el subtítulo, la primera línea del cuerpo o el primer elemento de lista como nombre cuando no se escribe uno. Una nota completamente vacía no genera una fila. Room sigue validando un nombre legible y conserva el identificador de la nota.

## Referencias y límites de fidelidad

Se obtuvieron respuestas de contexto de alta fidelidad y capturas para los frames:

| Frame | Nodo |
| --- | --- |
| 01 · Inicio | `2:153` |
| 02 · Notas | `2:159` |
| 04 · Concentración | `2:171` |
| 05 · Recordatorio | `2:177` |
| 06 · Recordatorios | `2:181` |
| 12 · Ajustes | `2:217` |
| 13 · Vista de cuadrícula | `2:223` |
| 15 · Archivada · Deshacer | `2:233` |
| 17 · Recordatorio pospuesto | `2:245` |
| 18 · Notas fijadas | `2:251` |
| 19 · Notas con imágenes | `2:257` |

Se inventariaron los 30 frames y los componentes mediante metadatos. El conector rechazó el resto de las peticiones con «You've reached the Figma MCP tool call limit on the Starter plan». Por tanto, no se considera terminada la comprobación visual de todas las variantes. Editor, captura rápida y fecha/hora reutilizan componentes contrastados y la estructura inventariada; falta obtener sus contextos completos para certificar su fidelidad.

Los iconos de fijado, concentración e imagen del editor utilizan por ahora Material Icons: los originales de los nodos `2:31`, `2:50` y `2:34` no pudieron descargarse al alcanzar el límite. Este detalle está pendiente de sustitución y comparación; no se promete una copia exacta de estas tres piezas. La imagen de paisaje de ejemplo no se incorpora como contenido ficticio: las imágenes del editor dependen de los adjuntos reales de cada nota.

La referencia recibida define el tema oscuro. El tema claro adapta la misma jerarquía, tipografía y componentes a superficies claras. Se conservan las opciones existentes de compartir/exportar y los mensajes necesarios de permisos, aunque no tienen una variante específica en las referencias recibidas.

## Recursos locales

Los 14 SVG disponibles se conservan sin modificar en `app/src/main/assets/figma`. Sus versiones PNG a 3× están en `res/drawable-nodpi`; `DesignIcon` los muestra en su tamaño de diseño de 24 dp, con el color del tema. No se cargan URLs temporales de Figma durante la ejecución.

| Recursos | Uso |
| --- | --- |
| home, notes, bell | Navegación y acceso a recordatorios |
| search, settings, plus | Cabeceras y captura |
| grid, list | Cambio de vista y herramientas |
| back, close | Navegación del editor y hojas |
| clock, check, arrow | Recordatorios y filas de opciones |
| archive | Archivo y aviso con Deshacer |

La familia Inter se distribuye localmente en cuatro pesos, generados a partir del original de Google Fonts. Su licencia OFL está en `assets/figma/INTER-LICENSE.txt`. Los tamaños, alturas de línea y contenedores se adaptan a Compose, con márgenes de 24 dp, espaciado de 8/16 dp y controles de al menos 48 dp. Los colores y la tipografía están centralizados en el tema.

## Datos y comportamiento

`NoteDocument` añade `pinned`, `archived`, `reminderCompleted`, `checklist` y `createdAt` con valores predeterminados compatibles. No cambia el esquema Room. Las actualizaciones de metadatos se serializan y vuelven a leer la nota actual para no perder cambios concurrentes. Deshacer el cuerpo preserva estos metadatos. Las casillas se incluyen al buscar y exportar TXT/PDF.

Una notificación puede pertenecer a una nota normal o a un recordatorio explícito. Al completar un aviso único se cancela su programación; al completar uno periódico se guarda y programa la siguiente ocurrencia. Las notas archivadas se consultan desde su filtro. Archivar no elimina adjuntos ni cancela automáticamente un aviso ya configurado.

## Verificación

`DesignUiTest` utiliza Compose y Robolectric con gráficos nativos, sin teléfono ni emulador. Comprueba ocultación del cuerpo, pulsación larga, búsqueda/cuadrícula, entrada/salida de concentración, completar sin eliminar, ajustes y selección de fecha/hora. Las capturas se generan en `/tmp/cleannotes-figma-*.png`; una selección está en [figma-preview](figma-preview/).

La suite completa consta de 53 pruebas locales. La integración con selectores del sistema, cámara, alarmas reales, permisos y escritura nativa del contenedor PDF sigue pendiente de revisión en Android cuando el usuario autorice usar el dispositivo.


## Ajustes solicitados el 24 de noviembre

Home deja de utilizar las secciones «A mano» y «Recientes» de Figma y recupera la agenda anterior. Las barras y Ajustes también se restauran por petición expresa del usuario; estas diferencias visuales son intencionadas. La pulsación larga sigue dando acceso a compartir, fijar, archivar y eliminar.

La agenda agrupa por `NoteDocument.createdAt`, no por la última edición. El repositorio fija ese valor al crear una nota y lo conserva al guardarla de nuevo, incluso si el borrador omite el campo. Las notas anteriores no tienen historial de creación: se utiliza su timestamp ya almacenado como referencia y se fija en la primera actualización. No se inventa una fecha histórica ni se modifica el esquema Room.
