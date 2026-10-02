# Validación de TecniHardcore 2.1.0

Fecha: 2 de octubre de 2026. Servidor y cliente: Minecraft 1.20.1 Fabric; mod 2.1.0 y protocolo de ritual 2. Las pruebas destructivas utilizan copias del mundo y cuentas TecniSoul/TecniVfx, nunca vidas o inventario de Doumoment.

## Pruebas completadas

| Área | Evidencia y resultado |
| --- | --- |
| Compilación | Gradle build correcto con Java 17; 13 pruebas unitarias de reglas, identidad, migración y pago. |
| Migración | Esquema 1→2, `revived=true`→historial 1; conserva vidas, enfriamientos y UUID. Relectura no aumenta el contador. En producción se compararon todos los jugadores y los archivos de playerdata con la copia previa: sin cambios en inventarios. |
| Resurrecciones repetidas | Dos rituales consecutivos del mismo jugador con autenticación activa, incluido historial previo. Cada uno devolvió una vida, aumentó una vez el contador y descontó una unidad de la pila. La prueba gráfica adicional llegó a 11 resurrecciones históricas del jugador de ensayo. |
| Interfaz | Apertura, lista de eliminados autenticados, selección explícita y confirmación mediante nonce. Solicitudes concurrentes del mismo santuario rechazadas, también con objetivos diferentes. |
| Cancelaciones | Distancia, daño, desconexión, cambio de mano/ofrenda y eliminación administrativa del núcleo: ninguna consumió corazón. Un recinto sin espacio seguro canceló al finalizar, conservando los ocho corazones de la pila. |
| Transacciones | Pruebas unitarias de pila 8→7, unidad 1→0, idempotencia y conservación de objetos ajenos. Inyección de un pago pendiente en la copia detenida y recuperación mediante arranque nativo: se descontó una unidad, quedaron siete y el diario quedó vacío. Los rituales completos también dejan el diario vacío. |
| Persistencia | Arranque de un mundo con santuarios guardados; se detectó y corrigió un bloqueo de carga antes de distribuir. Contadores y vidas persisten y no se repite el cobro al arrancar. |
| Generación natural | Mundo normal nuevo, semilla 12345678, nueve chunks candidatos en regiones distintas: tres núcleos generados automáticamente en 824/74/936, 1624/67/−1176 y 2472/83/1736. Se localizaron por comando y quedaron registrados. Los otros emplazamientos no superaron la selección/validación. La generación no se ejecuta retroactivamente sobre chunks completos. |
| Núcleo protegido | Pistón alimentado permaneció sin extender y el núcleo siguió en su posición. Paquetes de minería de un jugador de supervivencia cercano no lo rompieron. Una explosión real de creeper en la copia dejó el núcleo intacto y dañó decoración, como corresponde. Sin receta, objeto de inventario ni botín del núcleo. |
| Presentación | Capturas del framebuffer del Minecraft real: ruina, modelo GeckoLib con iluminación emisiva, fragmentación/reconstrucción, anillos, partículas, silueta y barra. Revisión desde observador, resucitado y oficiante; ventana 1280×720, pantalla completa 1920×1080, escalas de interfaz 2 y 4. Se verificó el pulso del cristal centrado y una vida, salud y hambre completas. |
| Efectos reducidos | Ritual completo con partículas mínimas de Minecraft e intensidad 25 %. El modelo y la barra siguen legibles. Preferencia guardada fuera de los archivos gestionados por el paquete. |
| Reliquias | Tres activaciones nuevas de Brasa, Bastión y Eco con daño letal: eventos visuales diferentes, salud inicial de dos corazones y tres vidas conservadas. La regeneración natural posterior sigue funcionando. |
| Compatibilidad | Un cliente sin protocolo 2 recibe instrucciones de actualización. Se corrigió la negociación de login para conectar desde el arranque temprano; varios arranques gráficos posteriores conectaron correctamente. |
| Launcher y paquete | Consulta local y a través de Playit: paquete 2.1, protocolo 2 y vida/historial de Doumoment coincidentes. Reparación por hashes, archivo del mod 2.0, conservación de preferencias y segunda sincronización sin copias innecesarias. Botón custom ENTRAR AL SERVIDOR preservado y dirección adaptada al perfil. |
| Instalador limpio | Setup final extraído a `tools/test-runtime/setup-21-final-release`. Usa el paquete embebido y perfil público; se descargaron Java 17 con checksum oficial, Minecraft, Fabric, bibliotecas y recursos. El cliente nativo TecniInstall21 se conectó a la copia de pruebas. Mod instalado idéntico por SHA-256 al servidor principal. |
| Copia y restauración | Copia consistente con servidor y SQLite cerrados. Extracción y verificación SHA-256 de 214 archivos. El mundo recuperado arrancó con 2.1 y conservó vidas e historial. Archivo: `backups/before-sanctuaries-21-20261002-023726.zip`. |
| Producción | Servidor actualizado y reiniciado mediante BAT; santuario inicial validado y colocado en −22/88/38 sin sustituir construcciones. EasyAuth activo y permisos existentes conservados. Visión 11, simulación 8 y memoria máxima 6 GB. |

## Rendimiento observado

Durante las capturas se registraron aproximadamente 114–120 FPS en este equipo, con límite del cliente cercano a 120. Spark mostró 20 TPS en la copia con varios jugadores de prueba; durante los primeros rituales, ticks medianos de aproximadamente 5–6 ms. La medición posterior en reposo mostró 3,4 ms de mediana y 4 ms de percentil 95 en el último minuto.

La generación forzada de nueve regiones lejanas a la vez en la copia de 2 GB produjo un retraso de aproximadamente nueve segundos. Esa medición corresponde a generación intensiva de terreno y otros mods; no permite prometer 20 TPS explorando simultáneamente muchas regiones. Las cifras de FPS tampoco garantizan el rendimiento de otros equipos.

## Límites y comprobaciones pendientes

- Falta una conexión desde una red externa. La dirección pública sí respondió desde esta PC a través del túnel, lo cual no reemplaza una prueba externa.
- No se realizó una sesión visual conjunta de tres personas desde tres equipos ni una escucha humana comparativa de la mezcla de sonido. Los recursos OGG y eventos posicionales se cargaron en el motor nativo; la percepción final de volumen y calidad queda por revisar jugando.
- La recuperación de pago se probó con una transacción inyectada y reinicio nativo, junto con pruebas de idempotencia. No se simuló un corte eléctrico físico en cada instrucción de escritura.
- Permanecen advertencias previas de otros mods: modelos ausentes de algunas menas de Nether Ores Reborn y avisos de mixins opcionales. No se atribuyen a los santuarios ni se consideran corregidos en esta entrega.

El video de muerte confirmado por el usuario se conservó. La resurrección no dispara su evento y un tótem activado no descuenta vidas. No se reemplazaron los assets existentes de muerte ni la personalización manual del menú.

## Artefactos

- `dist/TecniHardcore-Setup-2.1.0.exe`: instalador para compartir.
- `dist/manifest-2.1.0.json` y `dist/SHA256SUMS-2.1.0.txt`: versiones, tamaños y hashes.
- `ADMINISTRACION.md`: instrucciones de operación, rituales y restauración.
- `tools/test-runtime/*21-test.json`, `sanctuary-tests.json` y `ritual-edgecase-tests.json`: resultados automáticos.
- `tools/test-runtime/installed-launcher/game/screenshots`: capturas nativas. `tools/test-runtime/visuals21-observer` conserva vistas previas del observador y el pulso centrado del resucitado.
