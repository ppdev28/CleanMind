# Clean Notes

Aplicación Android para escribir, consultar y organizar notas y recordatorios personales. Funciona sin conexión y guarda las notas en una base de datos local.

La interfaz está construida con Jetpack Compose y Material 3. La arquitectura utiliza MVVM con flujo unidireccional de datos, dos capas y un único módulo Gradle.

## Funciones

- Crear, editar y eliminar notas con formato, listas, imágenes y deshacer/rehacer.
- Guardar automáticamente mientras escribes y mostrar el estado de guardado.
- Consultar la actividad por fecha o cambiar a una cuadrícula adaptable.
- Buscar por título y contenido.
- Crear recordatorios como apuntes independientes de las notas normales.
- Compartir texto mediante el selector de aplicaciones de Android.
- Elegir un tema claro, oscuro o vinculado al dispositivo.
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

La revisión visual e interactiva en teléfono o emulador está pendiente. Las imágenes de [screenshots/](screenshots/) pertenecen a una interfaz anterior y no representan el diseño actual. Hay previsualizaciones de Compose en [HomePreviews.kt](app/src/main/java/com/example/cleannotes/presentation/ui/home/HomePreviews.kt).

## Rama de UI de Figma

`feat/figma-complete-ui` implementa el nuevo flujo de Inicio, Notas, Recordatorios, editor y concentración. Consulta [implementación, capturas y límites de fidelidad](docs/figma-ui.md).
