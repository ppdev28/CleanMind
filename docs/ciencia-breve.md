# Ciencia breve

La colección solicitada contiene seis notas, cada una con una estrofa de cuatro líneas y un subtítulo breve. Su fuente editable es [science-notes.json](../design/content/science-notes.json).

| Nota | Tema |
| --- | --- |
| Luz en viaje | Luz y distancia |
| Gravedad | Mundos en movimiento |
| Hojas y luz | Fotosíntesis |
| Materia | Enlaces y propiedades |
| ADN | Información biológica |
| El planeta azul | Mar y clima |

En la variante **debug**, el primer arranque de esta versión incorpora las notas a una carpeta llamada Ciencia. Si ya existe una carpeta con ese nombre, se reutiliza. Si ya existe una nota con el mismo título, subtítulo y texto, se conserva sin modificar sus metadatos ni moverla.

La importación es aditiva: no elimina ni sobrescribe notas. Un marcador persistido indica que terminó, de modo que las notas que después edites o elimines no vuelven a generarse al reabrir. Si se interrumpe antes de finalizar, se comparan las notas ya incorporadas antes de reintentar.

La variante **release** no incluye el archivo de muestra ni ejecuta la importación. Cambiar de debug a release conserva los datos que ya hubiera en el dispositivo; no retira las notas que ya se hayan creado.

La colección se instaló en el realme RMX3370 después de recuperar la conexión USB. Se verificaron los seis textos y su pertenencia a Ciencia, sin modificar los cuatro registros que ya existían. Los resultados están en [Pruebas](pruebas.md).
