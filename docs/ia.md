# Inteligencia artificial

La rama `feat/ai-integration` parte de `feat/figma-complete-ui` e incorpora un recorrido completo: configurar la API, probarla, generar una propuesta a partir de una nota, revisarla y añadirla mediante el guardado normal.

## Configuración y uso

1. Abre **Ajustes → Inteligencia artificial**.
2. Introduce la URL base HTTPS de una API compatible con Chat Completions, el modelo y tu clave. Los valores iniciales son `https://api.openai.com/v1` y `gpt-4.1-mini`; el modelo debe estar disponible para tu cuenta.
3. Pulsa **Probar conexión**. Se comprueban los campos actuales sin guardarlos y se hace una generación breve con un mensaje de prueba. No incluye datos de notas y puede tener coste.
4. Pulsa **Guardar configuración de IA**. Los cambios no guardados no se usan desde el editor.
5. Abre una nota y pulsa **IA**. Si falta configuración, **Configurar IA** abre Ajustes; al volver se conserva el borrador del editor.
6. Elige **Resumir**, **Mejorar redacción**, **Extraer tareas** o **Instrucción propia**. Esta última admite una nota vacía, con una instrucción como «Redacta una invitación breve».
7. Pulsa **Generar**, revisa la propuesta y acepta con **Añadir a la nota**. El resultado se añade como texto normal al final del cuerpo; las tareas se presentan como líneas de texto. El original, su formato, imágenes, casillas y metadatos se conservan.

**Generar otra propuesta** repite la petición. **Cancelar generación** cancela la petición HTTP local; el proveedor puede haber comenzado a procesarla o facturarla. **Cerrar** descarta la propuesta. La aceptación verifica que el título y el documento coincidan con los usados para generar; una propuesta antigua o ya aplicada no se puede incorporar de nuevo.

El texto aceptado sigue `EnteredContent`, el borrador en `SavedStateHandle`, el debounce y el guardado Room existentes. Un error de guardado conserva el borrador para volver a intentarlo. La integración aparece en las notas; el formulario independiente de recordatorios mantiene su flujo actual.

## Compatibilidad y límites

- La URL base se concatena con `/chat/completions`; conserva rutas de proxy y `/v1`. No introduzcas la ruta completa del método. Solo admite HTTPS y rechaza credenciales dentro de la URL, parámetros de consulta y fragmentos.
- Se usa `POST` con `model`, `messages`, `stream: false` y `max_completion_tokens: 4096`, autenticado mediante `Authorization: Bearer`. El proveedor debe admitir este contrato y devolver texto en `choices[0].message.content`.
- Límite de entrada: 24.000 caracteres entre título, texto, subtítulo y casillas; 2.000 para la instrucción propia. Se rechaza un exceso, sin recortar silenciosamente la nota.
- No hay streaming ni selección de imágenes, herramientas, conversación persistente o procesamiento del resto del cuaderno. El resultado no se aplica automáticamente.
- Conexión de hasta 15 segundos, lectura de hasta 60 y petición completa de hasta 75. Sin redirecciones ni reintentos HTTP automáticos. Respuesta limitada a 256.000 bytes y 32.000 caracteres de texto.
- Una respuesta truncada, vacía, rechazada o incompatible se muestra como error y no se ofrece para guardar. Se distinguen problemas de clave/permisos, límite o saldo, modelo/ruta, red, tiempo de espera y proveedor.

## Credenciales y datos

`EncryptedAiSettingsRepository` cifra la configuración completa con AES-256-GCM. La clave criptográfica reside en Android Keystore, cada escritura genera un IV nuevo y `AtomicFile` protege frente a escrituras incompletas. El archivo queda en `noBackupFilesDir`, fuera del backup y la transferencia de dispositivo. No se necesitan migraciones Room.

La clave de API no está en código, recursos, `BuildConfig`, preferencias sin cifrar, navegación o `SavedStateHandle`. El campo está enmascarado y la representación diagnóstica de la configuración oculta la clave. La app no registra cuerpos de peticiones/respuestas ni muestra errores crudos del proveedor. **Eliminar clave y configuración** borra el archivo persistido y los campos de Ajustes. Una instalación nueva o una restauración requiere configurar de nuevo la API.

Solo se envía el título y el texto visible de la nota abierta, incluidos subtítulo y casillas. Los adjuntos y los metadatos de recordatorio no se envían. La URL identifica el destino y la hoja muestra su dominio antes de generar. El proveedor configurado recibe y procesa ese texto según sus propias condiciones.

Este flujo usa una clave aportada por la persona que instala la app. Si se distribuye un servicio con una clave compartida del desarrollador, la integración debe pasar por un backend propio; no se debe empaquetar esa clave en el APK.

## Organización y pruebas

- `ai/`: configuración, acciones, snapshots, validaciones, errores y contratos.
- `data/ai/`: persistencia cifrada y cliente real compatible con OpenAI.
- `di/AiModule`: dependencias compartidas y cliente HTTP.
- `AiSettingsViewModel`: carga, edición, guardado, borrado y prueba de conexión.
- `AiAssistantViewModel`: configuración por apertura, generación, cancelación y propuesta en memoria.
- `NotesApp`, `AiAssistantSheet` y `EditNoteViewModel.applyAiSuggestion`: conexión del recorrido y aceptación protegida contra borradores antiguos.

```bash
./gradlew :app:testDebugUnitTest --tests 'com.example.cleannotes.ai.*'
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
./gradlew :app:compileDebugAndroidTestKotlin
# Requiere emulador/dispositivo Android y ejecuta Android Keystore real:
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.cleannotes.ai.EncryptedAiSettingsTest
```

Las pruebas JVM cubren validación, peticiones HTTPS mediante MockWebServer con certificado de prueba, respuestas y errores, límites, cancelación, ausencia de redirecciones, configuración, propuestas, aceptación explícita y guardado/recuperación. Las pruebas Compose cubren el acceso a Ajustes, la aceptación y el rechazo de una propuesta antigua. La prueba instrumentada verifica persistencia, IV nuevo, detección de corrupción y borrado sobre Android Keystore real. Se ha ejecutado correctamente en un emulador API 35; los resultados están en [Pruebas](pruebas.md).

Las pruebas de red usan un servidor HTTPS local y credenciales ficticias. La comprobación con una API externa y una clave real queda pendiente de configurar la cuenta en la app.

## Referencias

- [Contrato Chat Completions](https://developers.openai.com/api/reference/resources/chat/subresources/completions/methods/create).
- [Android Keystore](https://developer.android.com/privacy-and-security/keystore).
- [Copia automática y exclusión de archivos](https://developer.android.com/identity/data/autobackup#Files).

[Índice](README.md).
