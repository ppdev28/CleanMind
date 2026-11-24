# Movimiento y arranque

`AppMotion.kt` centraliza un ritmo breve: 160 ms para desapariciones y 240 ms para navegación y cambios de contenido. Los desplazamientos son cortos y sin rebotes.

- Navegación entre cuaderno, editor y ajustes: fundido y desplazamiento horizontal, con dirección inversa al volver y soporte de navegación predictiva. La dirección se adapta a RTL.
- Carpetas: cuadrícula adaptable con selección visible y reordenación animada. Lista/cuadrícula de notas: fundido entre dos contenedores con scroll independiente para mantener estable la colocación de elementos.
- Pestañas y carga inicial: transición entre contenidos; cada pestaña conserva su estado de scroll mediante un contenedor guardable.
- Lista/cuadrícula y recordatorios: aparición, retirada y reordenación con claves estables y `animateItem`.
- Agenda y estados vacíos: ajuste animado de tamaño.
- Tarjetas: respuesta de pulsación sutil, manteniendo ripple y pulsación larga.
- Concentración y herramientas de formato: expansión/contracción sin reemplazar el editor de texto ni su historial.
- Formulario de recordatorio: fecha y hora aparecen dentro del formulario al activar Avisarme.
- Hojas inferiores, chips y controles: mantienen las transiciones nativas de Material 3. Los diálogos de fecha/hora pertenecen a Android.

Compose utiliza la escala de duración de Android; no hay temporizadores manuales ni bucles que mantengan las pantallas animándose. La salida de la splash también comprueba si el sistema permite animadores. El cambio de paleta conserva la aplicación inmediata del tema para mantener contraste entre texto y fondo.

La splash nativa muestra la pluma monocroma sobre la superficie clara u oscura del sistema y espera únicamente a que estén leídas las preferencias. La transición de salida dura 180 ms; no existe una segunda pantalla de arranque ni un tiempo mínimo artificial. Recursos y marca se describen en [design](../design/README.md).

Referencias de implementación: [animaciones de Compose](https://developer.android.com/develop/ui/compose/animation/quick-guide), [transiciones de Navigation 3](https://developer.android.com/guide/navigation/navigation-3/animate-destinations) y [SplashScreen](https://developer.android.com/develop/ui/views/launch/splash-screen/migrate).

Navigation 3 puede conservar una escena anterior para transiciones o vuelta predictiva. Los manejadores de Volver de cuaderno y editores reciben un indicador de destino activo: una escena oculta no cambia de pestaña ni cierra otra nota. Existe una prueba de regresión de este comportamiento.
