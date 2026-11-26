# Superficies y contraste de Material 3

La revisión del 25 de noviembre de 2026 adopta la alternativa Material 3 solicitada por el usuario. Sustituye el acabado de cristal anterior: se retiran Haze, la captura de fondos, los reflejos, el ruido y los degradados decorativos. Las superficies no se presentan como una reproducción del material de iOS.

`AppSurfaces.kt` centraliza los contenedores tonales y las hojas inferiores. El tema oscuro utiliza un fondo uniforme `#000000`, texto principal blanco y texto secundario gris claro. El tema claro conserva sus superficies claras y texto oscuro. Los estados seleccionados mantienen el acento azul acero.

Las tarjetas emplean `surfaceContainerLow`, las barras inferiores `surfaceContainer` y los estados seleccionados `tertiaryContainer`. Los límites usan los roles de contorno del tema. Los colores se resuelven como superficies Material 3 y no como transparencias que varíen el contraste del texto según el contenido situado detrás.

Las cabeceras conservan el fondo opaco del tema para impedir superposiciones. En las pestañas del cuaderno, el `TopAppBar` usa `exitUntilCollapsed`: se oculta al desplazarse hacia abajo y reaparece cuando el contenido alcanza su inicio. Invertir la dirección a mitad de la lista no lo muestra. Las pantallas con Volver y el editor mantienen sus cabeceras fijas.

El contenido del cuaderno aplica el padding medido por `Scaffold` y consume esos insets antes de componer listas, cuadrículas y recordatorios. Ese espacio se adapta a la altura visible de la cabecera y mantiene el viewport fuera de las barras. La franja de estado continúa protegida por el color del tema. La fecha de cada grupo conserva su semántica de encabezado y utiliza 16 sp y peso seminegrita.

La navegación se concentra ahora en `NotebookFab`: un botón flotante Material 3 despliega sus acciones en dos arcos radiales alrededor del propio botón, sin abrir una hoja. No hay barra inferior en la libreta. Las listas reservan 96 dp al final para mantener las últimas tarjetas fuera del botón; el editor conserva sus herramientas inferiores.

Todas las hojas inferiores de la app pasan por `AppBottomSheet`. El propio `ModalBottomSheet` pinta su contenedor, incluida la separación de navegación del sistema. Así no queda una franja transparente que deje ver la barra de la app por debajo. Se conserva el manejo de insets de Material 3 y se desactiva el scrim adicional de contraste de navegación en la ventana de la hoja desde API 29.

Referencias oficiales: [roles de color](https://m3.material.io/styles/color/the-color-system), [barras y desplazamiento](https://developer.android.com/develop/ui/compose/components/app-bars), [insets de Material 3](https://developer.android.com/develop/ui/compose/system/material-insets) y [protección de barras del sistema](https://developer.android.com/develop/ui/compose/system/system-bars).
