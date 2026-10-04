# TecniHardcore 2.6.3: arranque masivo de bloques

El tornado ahora muestra muchos más bloques **fuera del embudo**, con ascenso y giro rápidos. Los bloques realmente arrancados generan muestras sincronizadas: salen desde su ubicación original, entran en el remolino y se dispersan. El volumen del vórtice se completa con geometría representativa de esos materiales. No hay una entidad física por cada bloque destruido.

Se triplica el presupuesto máximo de demolición respecto a 2.6.2: hasta 36.000 bloques por segundo, sujeto al límite de procesamiento de 3 ms por tick y a los chunks cargados. El nivel 20 no garantiza esa tasa en cualquier PC o construcción.

**Los niveles 16–20 también arrancan obsidiana, netherita y contenedores. El contenido de sus inventarios se destruye sin generar objetos sueltos.** La protección del spawn, los núcleos de santuario, los portales y los bloques irrompibles permanece. Los bloques con agua pueden ser arrancados, pero el tornado no elimina los fluidos. Los niveles menores conservan la protección de contenedores y bloques duros.

Máxima intensidad, fuera del spawn protegido:

```mcfunction
/tecni desastre detener todos
/tecni desastre iniciar tornado ~ ~ ~ 512 600 ancho 600 destruccion 20
```

El último `600` antes de `ancho` es la duración en segundos. El nivel 0 conserva el terreno. La destrucción de niveles superiores es permanente. Los escombros son efectos visuales y no pueden recogerse ni vuelven a colocar bloques.

Se limita la geometría por distancia y configuración de partículas. Las muestras viajan en lotes de hasta 32 bloques, cuatro veces por segundo; al detenerse, cambiar de mundo o desconectarse, se limpian los efectos. Cliente y servidor deben ser 2.6.3 (`pack_v10`, canal adicional `storm_rubble_v1`). Cierra Minecraft y pulsa **ACTUALIZAR** en el launcher.
