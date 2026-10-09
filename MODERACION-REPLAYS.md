# Moderación: replays y protección PvP (2.8.0)

## Protección tras una muerte PvP

Cuando otro jugador mata a un superviviente autenticado, el fallecido recibe 30 minutos de protección. Mientras está protegido, no puede hacer daño a jugadores ni recibirlo de ellos. Incluye proyectiles y mascotas con dueño identificable. Mobs, caída, fuego y otros peligros del mundo siguen haciendo daño. No otorga vidas, no devuelve objetos y no evita la eliminación al gastar la quinta vida.

El tiempo solo avanza conectado y autenticado. Desconectarse y reiniciar conservan el tiempo restante. Una muerte del mundo o `/kill` no concede protección. Los daños ambientales sin propietario, como lava colocada anteriormente, no permiten atribuir de forma fiable un atacante.

La interfaz muestra el tiempo restante. `/tecni proteccion` permite consultarlo; un operador de nivel 2 puede consultar `/tecni proteccion Nombre`.

## Replays privados de las muertes

El servidor graba los paquetes de los jugadores autenticados en segmentos de dos minutos. Al morir, conserva el segmento actual, el anterior si existe y quince segundos posteriores. No necesita bots conectados ni instalar ReplayMod en los clientes. Son grabaciones 3D `.mcpr`, no videos MP4; la visibilidad está limitada a los chunks y entidades que recibió el jugador. Una muerte inmediatamente después de autenticarse puede ocurrir antes de iniciar el grabador; se registra explícitamente como `sin_grabacion`.

Los replays y el registro de muertes permanecen en el servidor:

- `server/recordings/tecni-moderacion/<UUID>/*.mcpr`
- `server/world/tecnihardcore-moderacion.json`

El registro incluye nombre, atacante identificado, causa, fecha, dimensión, coordenadas y segundo de la muerte. Los IDs son números cortos. Solo operadores de nivel 4 tienen acceso a los comandos de replays; no se habilitan descargas públicas. Chat y voz no se graban. Los paquetes de juego pueden contener información del inventario: tratar los archivos como evidencia privada.

```text
/tecni replays estado
/tecni replays listar
/tecni replays ver 1
/tecni replays ver 1 antes
```

`ver` abre el segmento de la muerte; `antes` abre el segmento previo. Esperar unos segundos después de morir hasta que figure `lista`. Durante la reproducción:

```text
/replay view pause
/replay view unpause
/replay view speed 0.5
/replay view restart
/replay view jump to marker named "MUERTE-1" -5s
/replay view close
```

Cerrar devuelve al moderador al servidor. Revisar desde una ubicación segura. Los archivos conservados no se eliminan automáticamente. Los buffers sin muertes sí se descartan; al quedar menos de 2 GB libres, se avisa al operador y se pausa el inicio de nuevas grabaciones. Archivar periódicamente la evidencia fuera del disco del servidor y comprobar `/tecni replays estado`.

## Dependencias y recuperación

Grabador: [ServerReplay 1.2.2 para 1.20.1](https://modrinth.com/mod/server-replay/version/1.2.2%2Bmc1.20.1), MIT, y Fabric Language Kotlin. Sus hashes y el Java del servidor están fijados en `tools/moderation28-lock.json`. Estos componentes se instalan solamente en el servidor. Esta versión del grabador requiere Java 21; Minecraft de los jugadores conserva Java 17.

Con el servidor apagado, `python tools/prepare-moderation28.py` verifica los hashes e instala el grabador y el JRE oficial de Eclipse Temurin en `server/runtime`. `INICIAR_SERVIDOR.bat` sigue siendo el inicio normal y abre/reutiliza Playit. `server/runtime-java.json` selecciona Java 21 mediante una ruta relativa, sin depender de las herramientas de desarrollo.

No borrar el registro de moderación: contiene protecciones pendientes y el índice de evidencia. Un archivo inválido detiene el arranque para evitar perder datos. ServerReplay intenta recuperar grabaciones interrumpidas; el índice comprueba los archivos guardados al reiniciar. Una interrupción antes de confirmar la muerte puede dejar un segmento incompleto, que debe revisarse manualmente.
