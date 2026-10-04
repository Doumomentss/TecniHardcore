# TecniHardcore 2.6.1 — comprobaciones

Minecraft 1.20.1 Fabric. Cliente, mod y launcher 2.6.1; conexión `pack_v8`, efectos `cataclysm_v3`, almas `soul_v3` y santuario sin cambios. Pruebas del 4 de octubre de 2026 en una copia aislada del mundo, cuentas E25Ground/E25View y puerto 25568. No se usaron vidas ni inventarios de Doumoment o de su amigo.

## Comprobaciones completadas

- Compilación del mod y 50 pruebas JUnit correctas. Once pruebas del launcher correctas.
- Tornado aceptado bajo techo, entre bloques y al solicitar Y=300. La punta pasó del terreno Y=96 a Y=64; la corona descendió más lentamente, prolongando el embudo. La posición sincronizada corresponde al movimiento un 50 % más rápido. Una zona con sólo el chunk inicial cargado se admite sin generar el recorrido.
- Meteoritos visibles para un observador creativo: modelos con estela y avisos de suelo, en ventana, pantalla completa, partículas mínimas y Complementary Reimagined MEDIUM. El servidor aplicó un impacto de 14 puntos a una cuenta de prueba sin armadura: salud 20 → 6, sin descuento de vidas por ese golpe.
- Los cuatro niveles de destrucción de meteoritos cambiaron bloques reales. En una muestra de 25×25×16 se observaron respectivamente 18, 38, 188 y 615 bloques retirados. El nivel 0 conservó todos los bloques de esa muestra, comprobados directamente por el servidor.
- Terremoto nivel 4: 800 bloques retirados y 50 posiciones de superficie abiertas en la muestra. Una fractura quedó abierta hasta Y=80, con suelo intacto en Y=79: 16 bloques desde la superficie inicial Y=95.
- Medición de 360 ticks durante el terremoto: media 6,00 ms, percentil 95 6,67 ms y máximo 12,15 ms. Son resultados de esa escena aislada, con el servidor de producción inactivo de jugadores; no garantizan el mismo rendimiento para cualquier terreno, número de jugadores o varios desastres.
- Capturas revisadas del tornado, meteoritos y fractura. Meteoritos y avisos siguen visibles con partículas mínimas. No hubo fallo de compilación del shader ni cierre del juego. Las advertencias de uniforms del shader para biomas ausentes en 1.20.1 son anteriores a este parche.
- Sonido de tornado reemplazado por viento filtrado y tres truenos secos originales. Archivos Vorbis mono, sin saturación digital; créditos y hashes de audio incorporados al mod.
- Protección del spawn conservada: el comando destructivo sobre su columna fue rechazado. La destrucción mantiene exclusiones de contenedores, núcleo del santuario, líquidos, portales y bloques irrompibles.
- Instalador probado al actualizar una instalación aislada desde 2.6.0: catálogo de 66 mods oficiales más dos propios, hashes correctos, reparación de un JAR corrupto, preferencias conservadas y botón personalizado **ENTRAR AL SERVIDOR**. La instalación nueva independiente se había comprobado en 2.6.0; esta versión conserva el mismo instalador y añade el paquete revisado.

La primera prueba de cráter falló porque el cliente de protocolo no contaba fiablemente las notificaciones de bloques de un servidor con registros modificados. Se conservó su resultado y se sustituyó esa medición por una consulta directa del terreno del servidor; las cuatro pruebas pasaron con ese método. No se usa el contador de paquetes como prueba final de preservación del nivel 0.

## Respaldo y despliegue

El servidor se apagó mediante `stop`, guardó todas las dimensiones y cerró SQLite. Se creó `backups/before-weather261-20261004-045720.zip`; se verificó su CRC y se restauraron los 199 archivos del mundo en otra carpeta, comparando sus SHA-256 con el original. Todos coincidieron. Se conserva el mod anterior en `backups/deploy261-core`.

El launcher del propietario y su cliente se actualizaron con el mismo instalador verificado. El arranque vuelve a usar el agente Playit vinculado y distingue el apagado solicitado de un fallo. Los resultados locales detallados quedan en `tools/test-runtime/weather261`; los scripts de prueba sólo aceptan el entorno aislado.

Tras arrancar, se compararon los ocho registros completos de almas y los ocho archivos de jugadores con el respaldo: coincidieron. Cliente y servidor contienen el mismo JAR final. Se verificaron versión 2.6.1 del launcher instalado, destino del acceso directo, botón personalizado y respuestas vigentes del servidor local y de Playit.

La release `v2.6.1` está publicada. Sus siete archivos coinciden en tamaño y SHA-256 con los artefactos locales. El feed oficial ofrece 2.6.1 a un launcher 2.6.0 y no ofrece otra actualización al launcher 2.6.1. Código y capturas están publicados en el repositorio.

## Artefactos

- Instalador: `TecniHardcore-Setup-2.6.1.exe`, 307808256 bytes, SHA-256 `441bf4f8372272099bce88466ea3dadd3fdaed0185e5580dbf3602507cbc75b4`.
- Mod: `tecnihardcore-2.6.1.jar`, SHA-256 `266becde18e146917ffd7a6f2516be23060af31fbafb76df222fa305b1ab82f3`.
- Manifiesto y listado de hashes: `manifest-2.6.1.json` y `SHA256SUMS-2.6.1.txt`.
- Guía y comandos: [NOVEDADES-2.6.1.md](NOVEDADES-2.6.1.md).

La comprobación pública de Playit se realiza desde este mismo equipo. Una partida desde otro acceso a Internet y una medición prolongada con varios jugadores quedan fuera de estas pruebas.
