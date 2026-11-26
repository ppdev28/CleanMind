# UI de Figma

Rama de trabajo: `feat/figma-complete-ui`.

Referencia: [Clean Mind, archivo de Figma](https://www.figma.com/design/qokv6yTT5e72bxduDmq7AV/Sin-t%C3%ADtulo).

## Implementación

- Inicio con búsqueda, captura rápida y agenda vertical: separador horizontal y fecha completa, sin círculos ni etiquetas laterales y todas las notas creadas en ese día. No se limita a diez notas.
- Barras anteriores recuperadas: título de Clean Mind y mayor separación superior; Inicio, Notas, «+» central, Recordar y Buscar en la barra inferior. Se mantienen colores y tipografía del estilo actual.
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

En la implementación inicial no se pudieron descargar los originales de los nodos `2:31`, `2:50` y `2:34`. La petición posterior del usuario sustituye ahora toda la iconografía de la app por Material Symbols Outlined de trazo fino; ya no se busca reproducir esos iconos de Figma. La imagen de paisaje de ejemplo no se incorpora como contenido ficticio: las imágenes del editor dependen de los adjuntos reales de cada nota.

La referencia recibida define el tema oscuro. El tema claro adapta la misma jerarquía, tipografía y componentes a superficies claras. Se conservan las opciones existentes de compartir/exportar y los mensajes necesarios de permisos, aunque no tienen una variante específica en las referencias recibidas.

## Recursos locales

Los 14 SVG obtenidos se conservan sin modificar en `design/figma-source/icons/` como referencia histórica. Se han retirado sus PNG de los recursos de ejecución. `MindIcons.kt` contiene los símbolos Material 3 activos como `ImageVector` locales, con peso 500, grade 0, tamaño óptico 24 y sin relleno. La procedencia y el generador están en [Recursos de diseño](../design/README.md). No se cargan URLs temporales de Figma durante la ejecución.

La familia Inter se distribuye localmente en cuatro pesos, generados a partir del original de Google Fonts. Su licencia OFL está en `assets/licenses/Inter-OFL.txt`. Los tamaños, alturas de línea y contenedores se adaptan a Compose, con márgenes de 24 dp, espaciado de 8/16 dp y controles de al menos 48 dp. Los colores y la tipografía están centralizados en el tema.

## Datos y comportamiento

`NoteDocument` añade `pinned`, `archived`, `reminderCompleted`, `checklist` y `createdAt` con valores predeterminados compatibles. No cambia el esquema Room. Las actualizaciones de metadatos se serializan y vuelven a leer la nota actual para no perder cambios concurrentes. Deshacer el cuerpo preserva estos metadatos. Las casillas se incluyen al buscar y exportar TXT/PDF.

Una notificación puede pertenecer a una nota normal o a un recordatorio explícito. Al completar un aviso único se cancela su programación; al completar uno periódico se guarda y programa la siguiente ocurrencia. Las notas archivadas se consultan desde su filtro. Archivar no elimina adjuntos ni cancela automáticamente un aviso ya configurado.

## Verificación

`DesignUiTest` utiliza Compose y Robolectric con gráficos nativos, sin teléfono ni emulador. Comprueba ocultación del cuerpo, pulsación larga, búsqueda/cuadrícula, entrada/salida de concentración, completar sin eliminar, ajustes y selección de fecha/hora. Las capturas se generan en `/tmp/cleanmind-ui-*.png`; una selección está en [ui-preview](ui-preview/).

La suite consta de 69 pruebas locales y dos pruebas instrumentadas. Además de los flujos comprobados anteriormente en Xiaomi, en el realme RMX3370 se verificaron cristal acelerado, carpetas, edición, imágenes de galería/cámara, portapapeles nativo, notificación en segundo plano y escritura SAF de TXT/PDF y copias automáticas. Consulta [Pruebas](pruebas.md) para el alcance y los casos pendientes.


## Ajustes solicitados el 24 de noviembre

Home deja de utilizar las secciones «A mano» y «Recientes» de Figma y recupera la agenda anterior. Las barras y Ajustes también se restauran por petición expresa del usuario; estas diferencias visuales son intencionadas. La pulsación larga sigue dando acceso a compartir, fijar, archivar y eliminar.

La agenda agrupa por `NoteDocument.createdAt`, no por la última edición. El repositorio fija ese valor al crear una nota y lo conserva al guardarla de nuevo, incluso si el borrador omite el campo. Las notas anteriores no tienen historial de creación: se utiliza su timestamp ya almacenado como referencia y se fija en la primera actualización. No se inventa una fecha histórica ni se modifica el esquema Room.

La revisión posterior simplifica los recordatorios independientes en tareas con guardado explícito, los separa de la agenda y sustituye el menú de creación por una hoja Material 3. Estas diferencias respecto de Figma también son intencionadas.

Las carpetas, el arranque monocromo y las transiciones se incorporan por las nuevas peticiones del usuario y amplían la referencia original. Consulta [Carpetas](carpetas.md) y [Movimiento y arranque](movimiento.md).

Las carpetas en cuadrícula son un cambio posterior pedido por el usuario. El 25 de noviembre, el acabado de cristal se sustituyó por la alternativa Material 3: fondo negro uniforme en oscuro, contraste alto, cabecera transparente con scroll y hojas continuas hasta el área de navegación. Notas ya no incorpora una búsqueda local; la búsqueda global continúa en su pestaña.

El 26 de noviembre se fijan las cabeceras y se hacen opacas para impedir superposiciones con el texto. La barra inferior pasa a estar acoplada al borde, con ancho completo. La distribución del contenido respeta el espacio reservado para ambas barras.
