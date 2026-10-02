# TecniHardcore 2.3.0 — entrega y comprobaciones

Minecraft 1.20.1, Fabric 0.19.5, GeckoLib 4.8.4. Protocolo de compatibilidad del paquete 4; protocolo de ritual 3 y estado público 2. El mundo, las tres vidas, resurrecciones ilimitadas y video de muerte se conservan.

## Cambios entregados

- Plaza protegida de radio 32, Y90–128: uso de santuario y mesas permitido; modificaciones de jugadores no operadores, explosiones, pistones, incendios y flujos de agua/lava bloqueados. Los monstruos son trasladados fuera. El exterior mantiene su dificultad.
- Tablón ilustrado en **6,96,−17**. Las imágenes son los objetos reales de Brasa, Bastión y Eco. Interacción en toda la superficie, incluidas las imágenes superiores; guía con efectos, ingredientes, sellos y enfriamiento. No se añadieron contratos semanales ni un tablón de reglas genéricas.
- Custodio de Pizarra: modelo y atlas originales, emisión azul, animaciones, audio posicional, tres ataques anunciados y tres fases. Solo existe en la arena local y está bloqueado en producción. Sin generación natural, recompensas ni modificaciones del terreno.
- Launcher: jugadores autenticados conectados, vidas, contador histórico, versiones y novedades desde el puerto Minecraft. Sondeo cada cinco segundos; datos caducados u offline se muestran como sin datos. Los textos se insertan como texto, sin ejecutar HTML.

## Pruebas completadas

- Compilación del mod y **16 pruebas Java**, sin fallos. Pruebas del panel para roster, resurrecciones, datos inválidos, desconexión y caducidad; cuatro pruebas del actualizador; transacciones de instalador con recuperación, hashes inválidos y rutas maliciosas.
- Copia del mundo: jugador sin permisos intentó romper y colocar bloques; la plaza permaneció intacta. Una explosión no dañó su suelo ni al jugador. Pistón en el borde y agua externa no invadieron la zona. El fuego exterior quemó su bloque de control, pero conservó intacta la madera protegida. El mismo jugador pudo romper un bloque de control fuera de la protección.
- Tablón visible dentro de Minecraft, textura sin sangrado del atlas, etiquetas y tres imágenes; guía revisada a escalas de interfaz 2 y 4. Un cliente sin permisos abrió el panel superior sosteniendo bloques.
- Modelo del jefe renderizado con GeckoLib dentro del juego. Observadas las tres clases de ataques y las tres fases; daño real, pérdida de vidas, reaparición y continuidad del video de muerte. Corregidos duplicación al cargar entidades y salida por empujón; comprobado **un único jefe tras reiniciar**.
- Arena escuchando solo en localhost. Producción rechazó `/tecni jefe invocar`; no se añadió ningún jefe al mundo principal.
- Panel del launcher capturado con Electron y datos de un servidor real de pruebas: jugador autenticado, 3/3 vidas, contador 0 y noticias/versiones coincidentes. Comprobada visibilidad de la pestaña Servidor.
- Instalador ejecutado en carpeta aislada, actualizado sobre ella y paquete preparado: **51 descargas oficiales verificadas y 53 mods**. Reparación de un mod corrupto, conservación de opciones, perfil público y botón personalizado. Comparado el hash del mod instalado con el mod final entregado.
- Actualización del servidor y del cliente del propietario. SHA-256 del archivo de jugador de Doumoment y del registro completo de almas **idénticos** a los anteriores al cambio. Auditoría del spawn conserva su único núcleo, espacio central, generación desactivada y reglas de reaparición.
- Respuesta local y por Playit: paquete 2.3.0, estado vigente y dos novedades. Esto comprueba el túnel desde este equipo; no sustituye una conexión de un amigo desde otra red.

## Rendimiento y límites de validación

La arena, con un jefe y un jugador, sostuvo 20 TPS; muestra de Spark: mediana 1,5 ms, percentil 95 de 2,0 ms en los últimos diez segundos. Capturas de combate estable: aproximadamente 73–119 FPS en este equipo; la primera entrada tuvo una caída puntual a 22 FPS mientras cargaba. No es una prueba de carga de varios jugadores ni una garantía para otros equipos.

El jefe continúa siendo un **prototipo para probar y equilibrar**. Faltan tus pruebas manuales de esquiva/dificultad y una prueba externa desde otra red. No se declara listo para incorporarlo a supervivencia.

## Copias y archivos

Copia consistente previa: `server/backups/tecnihardcore/world-1790973420390.zip`, CRC comprobado. Copia posterior al despliegue: `world-1790973994187.zip`. Versiones anteriores del mod en `backups/pack-before-2.3`; no se incluyeron mundos, identidades ni credenciales en el repositorio público.

Instalador: `dist/TecniHardcore-Setup-2.3.0.exe`. Versiones, tamaños y hashes finales: `dist/manifest-2.3.0.json` y `dist/SHA256SUMS-2.3.0.txt`. Instrucciones de la arena: [PRUEBA-JEFE.md](PRUEBA-JEFE.md). Administración y novedades: [ADMINISTRACION.md](ADMINISTRACION.md).

## Publicación verificada

Publicado en [GitHub Releases v2.3.0](https://github.com/Doumomentss/TecniHardcore/releases/tag/v2.3.0). Los siete archivos subidos coinciden en tamaño y SHA-256 con la entrega local, verificados antes de hacer pública la Release y después. El feed público ofrece actualizar de 2.2.0 a 2.3.0 y no vuelve a ofrecerla a un launcher 2.3.0.
