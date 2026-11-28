# Clean Mind

Aplicación Android para escribir, consultar y organizar notas y recordatorios personales. Guarda las notas en una base de datos local y permite escribir y consultarlas sin conexión. Las vistas previas automáticas de enlaces utilizan Internet.

La interfaz está construida con Jetpack Compose y Material 3. La arquitectura utiliza MVVM con flujo unidireccional de datos, dos capas y un único módulo Gradle.

## Funciones

- Crear, editar y eliminar notas con formato visual, listas, imágenes, archivos y deshacer/rehacer.
- Dictar texto mediante el reconocimiento de voz de Android y abrir fotografías a pantalla completa.
- Obtener vistas previas automáticas de enlaces y eliminar selecciones de notas o recordatorios.
- Guardar automáticamente mientras escribes y mostrar el estado de guardado.
- Eliminar casillas y recuperarlas con Deshacer; herramientas visibles y un panel único de formato.
- Consultar la actividad por fecha o cambiar a una cuadrícula adaptable.
- Navegar con la barra inferior Inicio, Notas, Nueva nota, Recordar y Buscar; búsqueda con foco directo, cambio de vista junto a Ajustes y hoja de creación de notas y recordatorios accesible desde Inicio.
- Organizar notas en [carpetas](docs/carpetas.md) en cuadrícula, con creación, renombrado y movimiento.
- Crear recordatorios con formulario propio de tarea, fecha y hora y guardado explícito.
- Compartir texto mediante el selector de aplicaciones de Android.
- Elegir modo claro, oscuro o del dispositivo, paletas de acento, un tema Gris o temas personalizados.
- Arranque monocromo, [superficies Material 3 de contraste suave](docs/glass-ui.md) y transiciones breves entre pantallas, pestañas y herramientas.
- Conservar el tema y restaurar determinados estados de navegación y edición.

Los recordatorios permiten notificaciones configurables, repetición y posposición. Se pueden guardar y compartir TXT/PDF y mantener copias TXT en una carpeta elegida. La app no incluye cuentas, sincronización propia, restauración de copias completas ni papelera. Consulta los detalles en la [guía de uso](docs/guia-de-uso.md).

## Documentación

| Documento | Contenido |
| --- | --- |
| [Índice de documentación](docs/README.md) | Recorridos recomendados y fuentes de verdad. |
| [Guía de uso](docs/guia-de-uso.md) | Pantallas, notas, recordatorios, búsqueda, ajustes y errores. |
| [Desarrollo](docs/desarrollo.md) | Preparación del entorno, compilación, dependencias y artefactos. |
| [Arquitectura](docs/arquitectura.md) | Decisión MVVM + UDF, responsabilidades, navegación y estado. |
| [Datos y migraciones](docs/datos-y-migraciones.md) | Modelo, Room, DataStore, compatibilidad y almacenamiento. |
| [Pruebas](docs/pruebas.md) | Comandos locales, cobertura real y validación pendiente. |
| [Mantenimiento](docs/mantenimiento.md) | Cómo ampliar la aplicación y actualizar su documentación. |

## Preparación rápida

Necesitas Android Studio con soporte para Android Gradle Plugin 9.1.0, Android SDK 36 y la JVM 21 configurada para el daemon de Gradle. La configuración completa está en [Desarrollo](docs/desarrollo.md).

```bash
git clone https://github.com/PPDev28/CleanNotes.git
cd CleanNotes
```

El nombre visible es **Clean Mind**. Se conserva el identificador `com.example.cleannotes` y la URL histórica del repositorio para actualizar la instalación existente sin perder datos.

Abre la carpeta en Android Studio y sincroniza Gradle. Desde la raíz puedes compilar y verificar sin instalar ni abrir la aplicación:

```bash
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
```

En Windows utiliza `gradlew.bat`. El APK debug se genera en `app/build/outputs/apk/debug/app-debug.apk`.

## Base técnica

| Área | Implementación |
| --- | --- |
| Lenguaje | Kotlin |
| Interfaz | Jetpack Compose y Material 3 |
| Estado | ViewModel, StateFlow y SavedStateHandle |
| Navegación | Navigation 3 y destinos serializables |
| Inyección | Hilt |
| Notas | Room sobre SQLite |
| Preferencias | Preferences DataStore |
| Pruebas locales | JUnit, kotlinx-coroutines-test y Robolectric |

Las versiones configuradas están en [libs.versions.toml](gradle/libs.versions.toml). El SDK y las opciones de compilación están en [app/build.gradle.kts](app/build.gradle.kts).

## Estado de verificación

La verificación local incluye compilación debug, lint y pruebas JVM/Robolectric; consulta el recuento en [Pruebas](docs/pruebas.md). Hay avisos de dependencias y recursos. Las pruebas de integración con SQLite y preferencias se ejecutaron con Robolectric en el ordenador.

Se ha revisado la app en un Xiaomi Redmi Note 8 Pro con Android API 36: creación, agenda, recordatorios, notificación en segundo plano, compartir TXT/PDF y apariencia clara/oscura. Los resultados y límites están en [Pruebas](docs/pruebas.md). Las imágenes de [screenshots/](screenshots/) pertenecen a una interfaz anterior y no representan el diseño actual. Hay previsualizaciones de Compose en [HomePreviews.kt](app/src/main/java/com/example/cleannotes/presentation/ui/home/HomePreviews.kt).

## Rama de UI de Figma

`feat/figma-complete-ui` implementa el nuevo flujo de Inicio, Notas, Recordatorios, editor. Consulta [implementación, capturas y límites de fidelidad](docs/figma-ui.md).

Los últimos ajustes de marca, agenda, iconos, formato y recordatorios están descritos en [Cambios de Clean Mind](docs/cambios-clean-mind.md).

La variante debug incluye [seis notas científicas breves](docs/ciencia-breve.md), que se incorporan una sola vez sin sobrescribir contenido. Release no instala muestras. La revisión se verificó localmente y en un realme RMX3370 tras recuperar la conexión USB; consulta los [resultados de pruebas](docs/pruebas.md).

La revisión de la nota del dispositivo está descrita en [To Dos · 28 de noviembre](docs/to-dos-28-noviembre.md). Se implementan sus puntos 2–8; la biometría queda excluida por indicación del usuario.

La revisión de [barras, Home y editor](docs/barras-y-editor.md) restaura la navegación inferior Material 3 Expressive, mueve el dictado y retira concentración y archivo.
