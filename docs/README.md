# Documentación de Clean Notes

Esta documentación describe la implementación del repositorio, incluidos el rediseño de la interfaz y la arquitectura MVVM + UDF. Está escrita en español y distingue las funciones disponibles de las comprobaciones pendientes.

## Para usar la aplicación

Empieza por la [guía de uso](guia-de-uso.md). Explica cómo crear y encontrar notas, qué significa el guardado automático y cómo funcionan los recordatorios.

## Para desarrollar

1. Prepara el entorno con [Desarrollo](desarrollo.md).
2. Lee la separación de responsabilidades en [Arquitectura](arquitectura.md).
3. Consulta [Datos y migraciones](datos-y-migraciones.md) antes de modificar almacenamiento.
4. Ejecuta las comprobaciones de [Pruebas](pruebas.md).
5. Sigue [Mantenimiento](mantenimiento.md) al ampliar funcionalidades.

## Fuentes de verdad

| Información | Fuente |
| --- | --- |
| Versiones de librerías y plugins | [Catálogo de versiones](../gradle/libs.versions.toml). |
| SDK, identificador, versión y configuración del módulo | [Configuración de app](../app/build.gradle.kts). |
| JVM del daemon | [Criterios de JVM](../gradle/gradle-daemon-jvm.properties). |
| Comportamiento de pantalla | [Código de presentación](../app/src/main/java/com/example/cleannotes/presentation/). |
| Esquema actual | [Esquema Room 3](../app/schemas/com.example.cleannotes.data.source.NoteDatabase/3.json). |
| Reglas de transición entre esquemas | [NoteDatabase](../app/src/main/java/com/example/cleannotes/data/source/NoteDatabase.kt). |
| Comportamientos comprobados | [Pruebas locales](../app/src/test/java/com/example/cleannotes/). |

Los valores de versión son los configurados en este proyecto; no implican que cada dependencia sea la última publicada. Si cambian las fuentes, actualiza los documentos afectados.

[Volver al proyecto](../README.md).
