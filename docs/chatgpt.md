# Usar una cuenta de ChatGPT en Clean Mind

Clean Mind admite **Continuar con ChatGPT**, además de la configuración mediante clave de API. La integración usa el flujo público de Sign in with ChatGPT para proyectos personales/locales y clientes de código abierto. La disponibilidad del uso del plan depende de la cuenta, el espacio de trabajo, los permisos y el despliegue de OpenAI. Distribuir un servicio comercial o alojado requiere revisar las condiciones y el acceso correspondiente con OpenAI.

## Entrar con tu cuenta de Google vinculada

1. Abre **Ajustes → Usar mi cuenta de ChatGPT → Continuar con ChatGPT**.
2. Se abre el navegador del dispositivo en el acceso oficial de OpenAI. Allí elige **Continuar con Google** y usa la cuenta con la que ya accedes a ChatGPT.
3. Elige el espacio de trabajo y autoriza a **Clean Mind** a usar tu plan, cuando OpenAI ofrezca esa opción.
4. Al terminar, pulsa **Volver a Clean Mind** en la página local de retorno o vuelve con Atrás. La app verifica la identidad y los permisos antes de activar la cuenta.
5. Confirma el aviso de primer uso y selecciona uno de los modelos disponibles para esa cuenta.
6. Abre una nota y pulsa **IA**. El panel indica **Usando tu plan de ChatGPT** y conserva el recorrido de generación, revisión y aceptación de la propuesta.

El botón inicia el acceso a ChatGPT. Google interviene dentro del inicio de sesión de OpenAI; Clean Mind no recibe la contraseña de Google ni solicita permisos de Gmail, Drive o Calendar. No necesitas un proyecto de Google Cloud, un secreto OAuth compartido ni copiar una clave de API para este recorrido.

La cuenta conectada se identifica en Ajustes. **Añadir otra cuenta de ChatGPT** conserva registros separados y permite elegir entre ellos. Las etiquetas «Cuenta 1», «Cuenta 2», etc. distinguen registros aunque compartan correo. **Volver a conectar esta cuenta** utiliza el registro existente. **Cerrar sesión de ChatGPT** intenta revocar la sesión renovable y elimina sus tokens locales; conserva el registro para un acceso posterior.

Si el acceso solo autoriza la identidad, la cuenta queda conectada con el uso del plan desactivado. **Autorizar uso de mi plan** vuelve a pedir el permiso con el mismo registro. La app no genera texto hasta obtener los permisos requeridos. El correo se muestra únicamente cuando OpenAI lo declara verificado; en otro caso se conserva la etiqueta local de la cuenta.

**Gestionar uso en ChatGPT** abre los controles de OpenAI. Las peticiones comparten el uso y los límites del plan autorizado. Un error o un límite no cambia automáticamente a una API de pago: **Usar configuración de API** es una elección explícita en Ajustes. La configuración anterior de URL, modelo y clave se conserva de forma independiente.

## Implementación

- Registro dinámico con el nombre real **Clean Mind**, un identificador opaco estable por instalación y el cliente emitido por OpenAI. El identificador emitido se guarda antes del intercambio de código para poder recuperarse de un intercambio fallido.
- OAuth Authorization Code con PKCE S256, `state` y `nonce` nuevos por intento. Navegador externo; sin WebView ni formularios propios para contraseñas.
- Callback HTTP en un puerto efímero de `127.0.0.1`, con ruta `/auth/callback`, validación de estado y duración máxima de cinco minutos. No se expone a la red local. El enlace `cleanmind://chatgpt/return` solo trae la app al frente; no autentica ni transporta tokens.
- Verificación criptográfica del ID token con Nimbus JOSE + JWT y JWKS de OpenAI. Se comprueban firma, algoritmo permitido, emisor, audiencia, expiración, fecha de emisión, identidad y nonce. La reautorización no puede sustituir un registro por otra identidad.
- Los permisos de identidad y de uso del plan se tratan por separado. Se solicitan `openid profile email offline_access resource.invoke chatgpt.tokens.use.direct` y se verifican los permisos realmente concedidos.
- Credenciales y registros cifrados con AES-256-GCM en un archivo privado excluido del backup y de la transferencia de dispositivo. No se guardan tokens en navegación, `SavedStateHandle`, URI de retorno, logs ni estado de presentación.
- Renovación serializada antes de la expiración, con sustitución conjunta del token de acceso y el refresh token rotatorio. Un error terminal de renovación elimina los tokens inutilizables; los errores temporales o de política conservan la conexión para poder recuperarla.
- Catálogo de modelos de la cuenta desde `/v1/models`. Generación mediante `/v1/responses`, con `store: false`, `stream: true`, instrucciones y contexto de la nota. No se usa Chat Completions con tokens de sesión ni endpoints internos de ChatGPT.
- El lector SSE espera `response.completed`. Una respuesta parcial, fallida o demasiado larga no se ofrece para añadir a la nota. Se mantiene el límite local de 32.000 caracteres de salida y 2 MB de eventos, además de los tiempos de espera HTTP existentes.
- El cierre o cambio de cuenta cancela las peticiones etiquetadas de esa cuenta. Si la revocación remota no se confirma, se informa del cierre local y se ofrece gestionar la desconexión desde ChatGPT.

La aplicación no obtiene el historial ni la memoria de ChatGPT por conectar la cuenta. Al generar solo se envía el título y el texto de la nota abierta, incluidos subtítulo y casillas, sin imágenes ni otras notas.

## Comprobación

```bash
./gradlew :app:testDebugUnitTest --tests 'com.example.cleannotes.chatgpt.*'
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:assembleDebugAndroidTest
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.cleannotes.ai.EncryptedAiSettingsTest
```

Las pruebas automatizadas usan cuentas y tokens ficticios: firma de identidad, PKCE, callback real en loopback, intercambio HTTPS, modelos, streaming, renovación concurrente, permisos, cancelación, revocación y selección explícita de fuente. La prueba con una cuenta real de Google/ChatGPT requiere que su titular complete el acceso y el consentimiento en OpenAI.

Si Android cierra el proceso durante el acceso en el navegador, vuelve a abrir Ajustes e inicia otro intento. Si el dispositivo no tiene un navegador activo, la app explica que debe instalarse o habilitarse uno. La app no reanuda un intento de OAuth cuyo estado temporal se haya perdido.

## Referencias oficiales

- [Inicio y disponibilidad](https://developers.openai.com/siwc/quickstart).
- [Registro y acceso](https://developers.openai.com/siwc/token-sharing-open-source/sign-in).
- [Cuentas y renovación](https://developers.openai.com/siwc/token-sharing-open-source/profiles-and-sessions).
- [Modelos e inferencia](https://developers.openai.com/siwc/token-sharing-open-source/models-and-inference).
- [Limitaciones y errores](https://developers.openai.com/siwc/token-sharing-open-source/errors-and-recovery).
- [Usar tu plan en otras aplicaciones](https://help.openai.com/en/articles/20001542-using-your-chatgpt-plan-in-other-apps-and-sites).

[IA con clave de API](ia.md) · [Índice](README.md).
