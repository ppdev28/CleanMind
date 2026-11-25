# Superficies y contraste de Material 3

La revisión del 25 de noviembre de 2026 adopta la alternativa Material 3 solicitada por el usuario. Sustituye el acabado de cristal anterior: se retiran Haze, la captura de fondos, los reflejos, el ruido y los degradados decorativos. Las superficies no se presentan como una reproducción del material de iOS.

`AppSurfaces.kt` centraliza los contenedores tonales y las hojas inferiores. El tema oscuro utiliza un fondo uniforme `#000000`, texto principal blanco y texto secundario gris claro. El tema claro conserva sus superficies claras y texto oscuro. Los estados seleccionados mantienen el acento azul acero.

Las tarjetas emplean `surfaceContainerLow`, las barras inferiores `surfaceContainer` y los estados seleccionados `tertiaryContainer`. Los límites usan los roles de contorno del tema. Los colores se resuelven como superficies Material 3 y no como transparencias que varíen el contraste del texto según el contenido situado detrás.

La cabecera del cuaderno es un `TopAppBar` transparente con desplazamiento `enterAlways`. La lista dibuja a lo largo de la ventana y aplica la separación inicial como `contentPadding`, de modo que su viewport no termina bruscamente bajo la barra. La franja de estado mantiene el color uniforme del tema para proteger la lectura del reloj. Al cambiar de pestaña se muestra de nuevo la cabecera. La fecha de cada grupo conserva su semántica de encabezado y utiliza 16 sp y peso seminegrita.

Todas las hojas inferiores de la app pasan por `AppBottomSheet`. El propio `ModalBottomSheet` pinta su contenedor, incluida la separación de navegación del sistema. Así no queda una franja transparente que deje ver la barra de la app por debajo. Se conserva el manejo de insets de Material 3 y se desactiva el scrim adicional de contraste de navegación en la ventana de la hoja desde API 29.

Referencias oficiales: [roles de color](https://m3.material.io/styles/color/the-color-system), [barras y desplazamiento](https://developer.android.com/develop/ui/compose/components/app-bars), [insets de Material 3](https://developer.android.com/develop/ui/compose/system/material-insets) y [protección de barras del sistema](https://developer.android.com/develop/ui/compose/system/system-bars).
