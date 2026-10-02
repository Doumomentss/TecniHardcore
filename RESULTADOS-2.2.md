# TecniHardcore 2.2.0 — registro de validación

Minecraft 1.20.1 Fabric, protocolo de ritual 3. Cambios visuales: núcleo monumental, cúpula negra de radio 32, cámara de participantes desde el segundo 10, aparición de la skin y descenso azul de cuatro segundos. Las reglas de pago y vidas conservan la autoridad del servidor y el diario de transacciones de 2.1.

## Pruebas realizadas

- Compilación Java 17 y 16 pruebas JUnit: vidas, cuentas, persistencia, recuperación de pagos y reglas temporales de cúpula/cámara/descenso.
- Minecraft real en una copia aislada del mundo, puerto 25566, cuentas `TecniSoul*`; sin cambios destructivos en Doumoment.
- Inspección de capturas del framebuffer: núcleo entero y oficiante desde atrás, cúpula negra, skin del objetivo, descenso, aterrizaje y regreso a la vista original. Ventana 1280×720, pantalla completa y escalas de interfaz 2 y 4. Entre 108 y 119 FPS observados en este equipo; no extrapolar a otros equipos.
- El video de muerte y su mod se conservan sin modificar; el ritual no llama al evento de muerte.

- Jugabilidad en la copia: dos resurrecciones consecutivas del mismo jugador, consumo unitario, conservación del contador, concurrencia, cancelación por daño, cambio de mano, desconexión y destrucción administrativa. Prueba de espacio seguro bloqueado: cancelación sin gasto; el oficiante se coloca fuera de los bloques de prueba para no sufrir asfixia y falsear el caso.
- La cámara del revivido también se verificó con Minecraft real: comienzo en el segundo 10, skin en lo alto, hélice azul durante el descenso, recuperación de cámara al completar y al cancelar a los 14 segundos. Capturas en `tools/test-runtime/visuals22` (privadas, salvo dos imágenes seleccionadas para documentación).
- Compatibilidad con Sodium 0.5.13: su programa de terreno omite ColorModulator, por lo que se añadió niebla limitada exclusivamente a su terreno; el núcleo y la skin usan sus renderizados iluminados. Se inspeccionó el resultado real, no solo el arranque.
- Spark durante los ensayos: 20 TPS; última muestra con mediana 1,9 ms y percentil 95 de 2,6 ms. Los efectos se calculan en el cliente; estas cifras corresponden a la copia y a pocos jugadores.
- Instalador 2.2.0 ejecutado en carpeta aislada, seguido de actualización del mismo destino: 51 descargas oficiales y hashes, 53 mods, reparación de corrupción, conservación de preferencias y botón personalizado. Cuatro pruebas del actualizador, pruebas de reversión/recuperación del instalador y prueba de interfaz Electron nativa completadas.
- Mundo principal respaldado mediante `/tecni backup`: `server/backups/tecnihardcore/world-1790964625147.zip`, con verificación CRC del servicio. Apagado limpio, sustitución del mod y arranque en 2.2.0 con EasyAuth. Las vidas, resurrecciones, enfriamiento y SHA-256 del archivo de jugador de Doumoment coinciden antes y después. La copia del mod anterior está en `server/backups/update-2.2.0/mods`.
- Estado Minecraft verificado tanto en `127.0.0.1:25565` como por `rails-acorn.tun.ply.gg:6906`: paquete 2.2.0. Esto se ensayó desde el equipo del propietario; una conexión de un amigo desde otra red permanece pendiente.

## Artefactos

Instalador: `TecniHardcore-Setup-2.2.0.exe`, SHA-256 `c04b20b910aa639bc7997f7ecde6f766e0c1286adb3f6ce754afd4bb0f0a1d04`.

Mod: `tecnihardcore-2.2.0.jar`, SHA-256 `829e692336d16789f952c0cd9a0f7bb49391b30adb2cd3a5c740298121202e71`.

El manifiesto y los hashes completos se distribuyen con la Release 2.2.0. La descarga y los hashes públicos se verifican antes de marcarla como última versión.
