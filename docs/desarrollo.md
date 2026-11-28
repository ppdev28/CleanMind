# Desarrollo y compilación

## Requisitos del proyecto

| Elemento | Configuración |
| --- | --- |
| Módulo | `app` |
| Identificador y namespace | `com.example.cleannotes` |
| Android mínimo | API 24, Android 7.0 |
| SDK de compilación y destino | API 36 |
| Gradle Wrapper | 9.4.1 |
| Android Gradle Plugin | 9.1.0 |
| JVM del daemon | JetBrains, versión 21 |
| Bytecode Java/Kotlin | Destino JVM 11 |
| Versión de la aplicación | `versionCode = 1`, `versionName = "1.0"` |

La JVM que ejecuta Gradle y el destino del bytecode son ajustes diferentes. La configuración del daemon está en [gradle-daemon-jvm.properties](../gradle/gradle-daemon-jvm.properties); las opciones del compilador están en [app/build.gradle.kts](../app/build.gradle.kts).

Utiliza Android Studio con soporte para el plugin configurado. Necesitas acceso a los repositorios de dependencias durante la primera sincronización; Gradle también puede necesitar descargar su distribución o la JVM correspondiente.

## Preparar el entorno

1. Clona el repositorio o abre la copia de trabajo existente.
2. Abre su carpeta raíz en Android Studio.
3. Instala Android SDK Platform 36 desde la configuración del SDK.
4. Comprueba que la configuración de Gradle utiliza la JVM requerida.
5. Sincroniza el proyecto.

`local.properties` contiene la ruta local del SDK y no debe compartirse. Android Studio puede generarlo. Si lo editas manualmente, utiliza tu propia ruta:

```properties
sdk.dir=/ruta/al/Android/Sdk
```

No copies rutas de otro equipo. Utiliza el wrapper del repositorio en lugar de una instalación global de Gradle.

## Comandos locales

Ejecuta los comandos desde la raíz. En Windows sustituye `./gradlew` por `gradlew.bat`.

```bash
# Consultar la JVM y la versión de Gradle utilizadas.
./gradlew --version

# Compilar el APK debug.
./gradlew :app:assembleDebug

# Analizar el código y ejecutar las pruebas locales.
./gradlew :app:lintDebug :app:testDebugUnitTest

# Verificación conjunta.
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
```

Estos comandos no instalan ni abren la aplicación en un teléfono o emulador. Consulta [Pruebas](pruebas.md) para ejecutar grupos concretos y localizar informes.

## Artefactos

| Tarea | Salida habitual |
| --- | --- |
| `:app:assembleDebug` | `app/build/outputs/apk/debug/app-debug.apk` |
| `:app:assembleRelease` | APK release en `app/build/outputs/apk/release/` |
| `:app:bundleRelease` | AAB en `app/build/outputs/bundle/release/` |

No existe una configuración de firma release en el módulo. Generar un artefacto no implica que esté preparado para publicarse. La variante release tiene desactivada la minificación.

## Dependencias

Las versiones se centralizan en [libs.versions.toml](../gradle/libs.versions.toml). El módulo utiliza sus alias desde el script de compilación.

| Responsabilidad | Bibliotecas |
| --- | --- |
| Interfaz | Compose, Foundation, Material 3 y Material Symbols locales. |
| Ciclo de vida | Lifecycle, ViewModel y recopilación de estado en Compose. |
| Navegación | Navigation 3 y Kotlin Serialization. |
| Inyección y generación | Hilt y KSP. |
| Almacenamiento | Room y Preferences DataStore. |
| Verificación local | JUnit, kotlinx-coroutines-test y Robolectric. |

Las versiones concretas deben consultarse en el catálogo para evitar duplicarlas en documentación que pueda quedar obsoleta. No es necesario actualizar todas las librerías para implementar una funcionalidad.

## Previsualizaciones

[HomePreviews.kt](../app/src/main/java/com/example/cleannotes/presentation/ui/home/HomePreviews.kt) contiene ejemplos de Inicio claro, Inicio oscuro, cuadrícula en un ancho pequeño y estado vacío con texto ampliado.

Se pueden abrir en el panel de previsualización de Android Studio sin ejecutar la app en un dispositivo. Los datos de muestra solo se usan en las previsualizaciones. Estas no sustituyen una revisión de navegación, accesibilidad o teclado en Android.

## Automatización existente

El flujo [android-build-manual.yml](../.github/workflows/android-build-manual.yml) se inicia manualmente con `workflow_dispatch`. Ejecuta pruebas y permite generar debug, release o ambos; para release también genera un AAB. Publica artefactos del proceso en GitHub Actions, sin desplegar la aplicación en una tienda.

El workflow instala inicialmente JDK 17, mientras que los criterios del daemon solicitan JVM 21. No se ha validado este flujo de CI durante el trabajo local. Al revisarlo, comprueba la resolución de la JVM solicitada y la disponibilidad del SDK; no des por hecho que la instalación inicial de Java determina la JVM del daemon. El workflow actual no tiene un paso explícito de lint.

## Problemas habituales

| Síntoma | Qué comprobar |
| --- | --- |
| No se encuentra el SDK | Ruta de `local.properties` e instalación de Platform 36. |
| Error de JVM o de toolchain | Criterios del daemon y salida de `./gradlew --version`. |
| Fallo al resolver dependencias | Conectividad, repositorios y versiones del catálogo. |
| Error de Room tras cambiar una entidad | Versión del esquema, migración registrada y JSON exportado. |
| Fallo solo en integración local | Informe de pruebas, configuración de Robolectric y descarga de sus dependencias. |
| Lint muestra avisos | Distingue avisos de errores; el informe incluye también dependencias y recursos sin uso. |

No soluciones un error de migración borrando datos como comportamiento de producción. Consulta [Datos y migraciones](datos-y-migraciones.md).

[Índice de documentación](README.md).


### Paleta y hoja de compartir

`Color.kt` define los colores y `Theme.kt` adopta la paleta neutra de Figma en esta rama; `Theme.kt` asigna todos los roles de superficie, contenedores, texto, bordes e inversos para claro y oscuro. Los errores mantienen un color semántico propio. Evita colores literales en pantallas: utiliza `MaterialTheme.colorScheme`. Las variantes XML cubren el arranque y los diálogos nativos de fecha y hora, incluso con apariencia forzada desde Ajustes.

`ExportMenu.kt` presenta una hoja inferior con formato seleccionable, vista previa y acciones de compartir, guardar y copiar texto. Incluye previsualizaciones claras, oscuras y a 320 dp con texto grande. Los resultados del selector de archivos conservan una instantánea del documento y el formato solicitado; abrir la hoja de compartir no se considera una entrega confirmada.

La UI activa de la libreta está en `presentation/notebook/NotebookScreen.kt` y los componentes importados en `presentation/ui/design/DesignComponents.kt`. Los wrappers antiguos se conservan para compatibilidad; las nuevas referencias son [UI de Figma](figma-ui.md).

## Formato compartido

El estilo sigue `CustomTopBar`: tabuladores de ancho 2 y continuación de 4. `.editorconfig` y `.idea/codeStyles/Project.xml` se comparten en Git. El estado local de Android Studio permanece ignorado.

```bash
STUDIO_FORMATTER="/ruta/Android Studio.app/Contents/bin/format.sh" ./scripts/format-code.sh
# Verificar sin modificar archivos:
STUDIO_FORMATTER="/ruta/Android Studio.app/Contents/bin/format.sh" ./scripts/format-code.sh -d
```

El script aísla la configuración del formateador para poder usarlo con Android Studio abierto. Si se define `KTFMT_JAR`, aplica primero ktfmt con estilo Kotlin. Los iconos se regeneran con `scripts/update_material_icons.py`; consulta [Recursos](../design/README.md).

## Integración opcional de IA

La configuración de URL, modelo y clave se introduce en **Ajustes → Inteligencia artificial**; no hay secretos de IA en Gradle, recursos o `BuildConfig`. El cliente usa OkHttp, y las pruebas HTTPS locales emplean MockWebServer y `okhttp-tls`, todos fijados en el catálogo de versiones. Consulta [IA](ia.md) para el contrato compatible y los comandos de verificación, incluida la prueba de Android Keystore en dispositivo/emulador.

El acceso opcional con cuenta de ChatGPT usa Nimbus JOSE + JWT para verificar ID tokens y el mismo cliente OkHttp para OAuth y Responses. No requiere un cliente Google Cloud ni secretos OAuth embebidos: Google se elige en el navegador de OpenAI y el cliente se registra mediante el flujo público para proyectos locales compatibles. Consulta [Cuenta de ChatGPT](chatgpt.md) antes de distribuirlo como servicio comercial.
