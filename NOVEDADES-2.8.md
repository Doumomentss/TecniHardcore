# TecniHardcore 2.8.0

Esta versión añade herramientas privadas de moderación y protección después de una muerte PvP.

- El servidor graba segmentos de dos minutos de cada jugador autenticado y conserva el segmento anterior, el de la muerte y quince segundos posteriores.
- El operador puede abrir una reproducción 3D con `/tecni replays ver <id>` y cerrarla con `/replay view close`.
- El índice privado guarda atacante, causa, posición, tiempo y archivos asociados. No se publican replays, chat ni voz.
- Una muerte causada por otro jugador activa 30 minutos de protección de conexión para ambos sentidos del PvP. Mobs y peligros del mundo siguen activos. Desconectarse pausa el contador.
- El launcher y el servidor quedan en 2.8.0; el botón personalizado `ENTRAR AL SERVIDOR`, el perfil público y las cinco vidas se conservan.

El instalador contiene el cliente. ServerReplay y Java 21 son dependencias privadas del servidor y no se descargan en los equipos de los jugadores.
