# Cristal y superficies

La UI usa un material compartido en `GlassMaterial.kt`: desenfoque del fondo, tinte neutro, reflejo suave, borde de luz y sombra contenida. La paleta conserva el acento azul acero. Texto e iconos se dibujan encima del material, sin desenfoque ni transparencia de lectura.

`GlassScene` captura una decoración estática de fondo. Las tarjetas y opciones solo toman esa capa ambiental; las barras flotantes del cuaderno pueden captar además el contenido que pasa detrás. El desenfoque no introduce animaciones permanentes ni captura fotografías o ventanas externas a la app.

Se utiliza Haze 1.6.10, compatible con la versión de Kotlin/Compose del proyecto. Las versiones nuevas de Haze requieren una plataforma de compilación posterior; este cambio visual no actualiza la arquitectura ni el SDK. Referencias: [uso y estilos de Haze 1.6.10](https://github.com/chrisbanes/haze/blob/1.6.10/docs/usage.md), [versión de la biblioteca](https://github.com/chrisbanes/haze/tree/1.6.10).

El desenfoque se activa desde API 32 cuando la vista tiene aceleración gráfica. En versiones anteriores, vistas de previsualización y renderizado sin GPU, se usa una superficie translúcida con los mismos bordes y contraste. Las capturas JVM de `DesignUiTest` seleccionan explícitamente este acabado porque su Canvas de bitmap no implementa el pipeline gráfico de Android; el desenfoque real requiere comprobación en hardware. En esta revisión se ha validado el renderizado de compatibilidad; la conexión ADB del realme se perdió y no se continuaron las pruebas en el teléfono.

Las hojas inferiores comparten `GlassBottomSheet`. El material está dentro del contenido que desplaza Material 3, para que su recorte no interfiera con arrastre, gestos o zonas pulsables. Las superficies transparentes heredan explícitamente el color de texto del tema.
