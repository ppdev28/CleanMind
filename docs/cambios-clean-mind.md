# Cambios de Clean Mind

Revisión del 24 de noviembre de 2026, rama `feat/figma-complete-ui`.

| Petición | Resultado |
| --- | --- |
| Nombre | Clean Mind en launcher, cabeceras, información, temas y documentación. Lectura compatible con documentos anteriores. |
| Agenda | Separadores con fecha completa y notas del mismo día debajo; sin círculos ni etiquetas laterales de día. |
| Formato | Código Kotlin, Gradle, XML y configuración formateados según CustomTopBar; estilo compartido y script reproducible. |
| Archivos sin versionar | Originales del logo y SVG históricos en `design/`, licencias, scripts y criterios portables de JVM incorporados. Cachés y estado local del IDE ignorados. |
| Iconos | Material Symbols Outlined, peso 200, grade -25, óptica 24 y sin relleno; vectores locales en toda la UI. |
| Crear | Hoja Material 3 con dos filas grandes, descripción, iconos y cierre. |
| Recordatorios | Formulario propio con tarea, fecha/hora y detalle; guardado explícito y confirmación al descartar. Lista de pendientes con casilla y posposición, separada de la agenda. |

## Compatibilidad

Se conserva `com.example.cleannotes` como identificador y namespace, además de la base y las rutas existentes. Renombrarlos instalaría otra app o exigiría trasladar datos. El prefijo histórico del documento permanece únicamente en el lector y su prueba de compatibilidad. La URL GitHub y las capturas de `screenshots/` son históricas.

## Verificación

Compilación debug, lint y 62 pruebas locales sin fallos ni omisiones. El formateador de Android Studio valida 95 archivos. Las capturas de [ui-preview](ui-preview/) usan datos sintéticos de Robolectric.

En Xiaomi se probaron los flujos principales, una notificación con la app en segundo plano y la generación/compartición de texto y PDF. Se eliminaron los datos de prueba y se verificó que los registros originales permanecen idénticos. Consulta [Pruebas](pruebas.md) para los resultados y los casos aún pendientes.

## Mantener el estilo

Las instrucciones de formato están en [Desarrollo](desarrollo.md#formato-compartido). Los iconos proceden de la fuente oficial de Google; [design](../design/README.md) conserva procedencia, ejes, huella y generador. Las licencias de Inter y Material Symbols están en `app/src/main/assets/licenses/`.

Las ampliaciones posteriores de carpetas, arranque y transiciones se documentan en [Carpetas](carpetas.md) y [Movimiento](movimiento.md).
