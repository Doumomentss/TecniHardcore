# TecniHardcore 2.6.1 — desastres sobre el terreno

Minecraft 1.20.1 Fabric. Paquete y launcher 2.6.1, handshake `pack_v8`, desastres `cataclysm_v3`. Mantiene las vidas, inventarios, cuentas, monturas y santuario. Actualizar cliente y servidor juntos.

## Tornado

- Se puede invocar bajo techos, entre obstáculos y desde posiciones altas: no exige 200 bloques libres dentro del límite de construcción. La geometría puede extenderse sobre ese límite.
- La punta sigue el suelo conectado a su altura anterior. Puede subir pendientes y bajar a un valle; la parte superior baja más despacio, alargando el embudo hacia abajo. El servidor sincroniza punta y corona con interpolación en el cliente.
- Desplazamiento circular un 50 % más rápido: aproximadamente 1,2 bloques/s con órbita de 24 bloques. Giro del embudo independiente.
- Sólo necesita el chunk inicial cargado; si su recorrido entra en un chunk descargado, espera en la última posición cargada. No genera terreno ni carga regiones por invocarlo.
- Viento suave y truenos secos originales, sin grabación de lluvia en el tornado. Sirena de diez segundos y controles de volumen conservados.

```mcfunction
/tecni desastre iniciar tornado ~ ~ ~ 192 180 ancho 300 destruccion 0
/tecni desastre iniciar tornado ~ ~ ~ 192 180 ancho 300 destruccion 4
```

## Meteoritos

Hay impactos visibles incluso si sólo hay observadores en creativo o espectador. Las zonas de impacto se fijan al comenzar cada aviso, con tres segundos para apartarse. Los meteoros terminan en el suelo o en el techo que encuentran; un techo intacto bloquea el daño. Los impactos hacen 14 puntos antes de armadura, en radio 4, a supervivientes autenticados; no se acumulan varios golpes de la misma tanda sobre el mismo jugador. Creativos y espectadores no reciben daño.

```mcfunction
/tecni desastre iniciar meteoritos ~ ~ ~ 192 180 destruccion 0
/tecni desastre iniciar meteoritos ~ ~ ~ 192 180 destruccion 4
```

| Nivel | Cráter máximo |
|---|---|
| 0 | Sólo cráter visual temporal, sin cambios de bloques |
| 1 leve | Radio 2, profundidad 1 |
| 2 moderado | Radio 3, profundidad 2 |
| 3 severo | Radio 5, profundidad 4 |
| 4 realista | Radio 7, profundidad 7 |

El cráter tiene forma de cuenco. Su excavación se reparte entre ticks; la profundidad máxima se encuentra en el centro. No produce fuego ni objetos para recoger.

## Terremoto

Ahora abre fracturas conectadas desde la superficie, en lugar de eliminar bloques dispersos que no se veían desde arriba. Durante cada pulso se dibujan cortes progresivos, con dirección que cambia entre pulsos.

| Nivel | Ancho y profundidad máximos | Límite de bloques por segundo durante el pulso |
|---|---|---:|
| 0 | Efectos visuales | 0 |
| 1 | Ancho 1, profundidad 1; materiales blandos | 6 |
| 2 | Ancho 2, profundidad 2; materiales blandos | 24 |
| 3 | Ancho 3, profundidad 8; incluye estructuras | 160 |
| 4 | Ancho 5, profundidad 16; incluye estructuras | 480 |

```mcfunction
/tecni desastre iniciar terremoto ~ ~ ~ 192 180 destruccion 4
/tecni desastre detener todos
```

Los límites son máximos: la carga del servidor y los bloques protegidos pueden reducirlos. Cada desastre procesa hasta 24 intentos de bloque y 1,2 ms de trabajo por tick. Los meteoritos limitan su cola a 4096 bloques. El nivel 0 sigue siendo el predeterminado.

**La destrucción 1–4 es permanente; detener el desastre no restaura el terreno.** Se conservan la protección del spawn, contenedores, santuario, líquidos, portales y bloques irrompibles. Radio máximo 512, ancho del tornado máximo 600, duración 30–600 segundos y dos desastres sin superposición. Estos límites también se aplican al invocar entre obstáculos.
