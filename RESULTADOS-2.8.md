# Resultados 2.8.0

Comprobado en una copia aislada de Minecraft 1.20.1 Fabric:

- Compilación Gradle y pruebas unitarias de límites, pausa offline, persistencia y rutas seguras.
- Muerte PvP: descuento de una vida y protección de 30 minutos.
- Bloqueo en ambos sentidos para golpes y proyectiles.
- Daño de mundo y mobs mantenido durante la protección.
- Pausa al desconectar y continuación al reconectar.
- `/kill` genera registro de replay sin conceder protección PvP.
- Replay `.mcpr` guardado con paquetes, coordenadas y metadatos; el servidor aislado creó archivos válidos.
- Visor nativo probó que inventario y salud del moderador se restauran al cerrar.
- Instalador limpio, reparación, hashes, preferencias, perfil público y botón `ENTRAR AL SERVIDOR` comprobados.

La reproducción visual se mantiene privada y depende de que el servidor conserve los chunks que el jugador observó. Los replays guardados inmediatamente después de autenticarse pueden quedar marcados `sin_grabacion` si la muerte ocurre antes de iniciar el segmento; el índice lo informa sin inventar evidencia.
