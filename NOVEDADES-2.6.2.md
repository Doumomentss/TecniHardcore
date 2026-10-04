# TecniHardcore 2.6.2

El tornado admite **destrucción 0–20**. El terremoto y los meteoritos conservan 0–4. El radio y la anchura aumentan el alcance y la fuerza del tornado, además del presupuesto de demolición, con límites para proteger el rendimiento.

Ejemplo extremo, fuera del spawn protegido:

```mcfunction
/tecni desastre iniciar tornado ~ ~ ~ 512 600 ancho 600 destruccion 20
/tecni desastre detener todos
```

Radio permitido: 32–512 bloques; anchura: 40–600; duración: 30–600 segundos. El nivel 0 no modifica bloques, pero conserva daño y arrastre. Los niveles 1–4 son leves frente a los 5–20; el nivel 20 derriba columnas de hasta 296 bloques de profundidad. La destrucción es permanente: usar una copia para ensayos.

La protección del spawn, los contenedores y otros bloques protegidos permanecen intactos. No se generan chunks por invocar una tormenta. El agua y la lava pueden extenderse al desaparecer sus bloques de soporte. Detener el desastre termina la actividad; no restaura el terreno.

Se conserva el seguimiento del terreno, el viento corregido, las vidas, monturas, eventos, santuario y vídeo de muerte. Se requiere actualizar cliente y servidor: protocolo de paquete 9 (`pack_v9`); el canal de tormentas sigue siendo `cataclysm_v3`.

Actualización: cerrar Minecraft y pulsar **ACTUALIZAR** en el launcher. El instalador mantiene nombre, carpeta, preferencias y **ENTRAR AL SERVIDOR**. Los mods externos siguen descargándose de sus fuentes oficiales.
