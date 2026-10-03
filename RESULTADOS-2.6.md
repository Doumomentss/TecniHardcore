# Resultados TecniHardcore 2.6.0

Fecha: 3 de octubre de 2026. Minecraft 1.20.1 Fabric, Java 17. Mod y launcher 2.6.0; pack_v7 y cataclysm_v2. Pruebas realizadas con cuentas E25 de prueba en el servidor aislado de localhost 25568; no se usaron vidas ni inventarios de Doumoment ni de su amigo.

## Comprobado

- 43 pruebas JUnit y 11 pruebas del launcher aprobadas.
- Comandos de eventos: 13 verificaciones reales. Entrar/iniciar/detener sin ID, referencias numéricas y UUID antiguos, rechazo de evento inexistente, inicio vacío y referencia anterior, y restauración de inventario/vidas del ensayo.
- Destrucción: niveles 0–4 de tornado y terremoto en una parcela aislada. Nivel 0 produjo cero cambios de bloques en ambos. Los niveles superiores produjeron cambios y conservaron contenedores y núcleo del santuario. Se rechazó una zona destructiva sobre el spawn. Ancho 600 y radio 512 aceptados; el radio efectivo de un tornado de ancho 120/radio solicitado 32 es 84, sin expansión repetida.
- Espacio real de KeyBinding, compartido: jugador a pie pasó de Y 96 a 97,02; sobre grifo pasó de 97,7 a 114,49 sin cambiar la tecla. No se utilizó la entrada de vuelo artificial para esas pruebas.
- Skins: petición de skin Mojang, propiedades firmadas transmitidas, skin completa visible en inventario y persistencia NBT con UUID offline intacto. La primera sonda de protocolo leyó el campo incorrecto; se corrigió la sonda y se confirmó por NBT y cliente nativo. No es un fallo pendiente del juego.
- Iconos: ocho PNG originales con transparencia. Revisados en inventario y mano; las cinco referencias de montura muestran imágenes distintas según su NBT. Se corrigió el rango del predicado visual a 0–1. También se corrigieron textos españoles que habían sufrido conversiones de UTF-8.
- Presentación nativa de cinco desastres, partículas mínimas y reducidas, tornado en Calidad/MEDIUM y Ultra calidad/ULTRA, tormenta eléctrica en ambos presets y pantalla completa; vuelta a Optimizado y limpieza al detener. Shader ZIP oficial intacto. Sin error de compilación ni caída del cliente. Iris emite advertencias de uniformes/biomas de versiones nuevas que no existen en 1.20.1; no impiden la activación comprobada.
- Sirena, viento, terremoto, lluvia y truenos Ogg mono con niveles limitados; eventos registrados, sin mensajes de sonido desconocido. La grabación real y su licencia están documentadas en AUDIO-CREDITS.json. La valoración subjetiva del volumen y del timbre queda abierta a ajustes del usuario y depende del volumen Clima de cada cliente.
- Baliza: haz y HUD visibles a 99.000 bloques con **seis chunks**. La señal utiliza coordenadas enviadas por el servidor; no necesita cargar los chunks de origen. Máximo 16 señales, actualización una vez por segundo. Es una referencia proyectada, no iluminación física a 100 km. El límite exacto de 100.000 está definido en servidor con cálculo double para evitar desbordamiento.
- Reinicio aislado con santuario persistido: se detectó y corrigió un bloqueo de reparación de colisión dentro de CHUNK_LOAD. La reparación ahora espera al final del tick y no solicita chunks nuevos. Reinicios posteriores y prueba visual del santuario estables. Los desastres no se reanudaron.
- Instalación desde setup en carpeta aislada: 66 descargas oficiales verificadas, 68 JAR en cliente, botón ENTRAR AL SERVIDOR y perfil público conservados. Reparación de un JAR corrupto, respaldos y preferencias verificados. Actualización de la instalación 2.5 aislada a 2.6 también aprobada.

## Rendimiento medido

Misma cámara, arena aislada, Optimizado, dos actores de protocolo y observador Minecraft nativo. 360 ticks por medida y 15 segundos de frames, después de calentamiento. Equipo anfitrión con cliente y servidor de producción coexistiendo. La arena tiene pocos bloques: estas cifras no representan un Overworld densamente construido.

| Escena | FPS medios | Tick medio | Tick p95 | Tick máximo |
|---|---:|---:|---:|---:|
| Sin tornado | 1045,85 | 5,21 ms | 6,42 ms | 17,45 ms |
| Tornado ancho 300 | 1013,44 | 4,84 ms | 5,58 ms | 13,66 ms |

Caída de FPS: **3,1 %** en esta escena; presupuesto de 20 TPS respetado. Mallas cacheadas, menos geometría distante, límite de escombros y trabajo destructivo acotado. No se redujeron vidas, dificultad, botín, radio solicitado ni distancia de visión del servidor. No se garantiza el mismo FPS en todos los equipos ni ausencia de picos al generar chunks nuevos.

## Despliegue y respaldo

Servidor detenido limpiamente y todas las dimensiones guardadas. Respaldo **before-weather26-20261003-190109.zip**, SHA-256 `69cd0abf7f57aa5adf2fad14865199cf5a1fe08a6686bb6e585a47986aca797a`. Se verificó CRC y se restauraron 185 archivos del mundo exactamente en otra carpeta antes de desplegar.

Servidor actualizado y arrancado a las 19:03, puerto 25565, EasyAuth y Playit existentes. Comparación con el respaldo: ocho archivos de jugadores byte a byte intactos; vidas, enfriamientos y resurrecciones sin cambios, permisos e identidades reservadas conservados. Launcher del dueño e instalación cliente también actualizados, manteniendo perfil local.

Setup: **TecniHardcore-Setup-2.6.0.exe**, 307686912 bytes, SHA-256 `3fa3dc8f8a4b705f96795f4cb0a7e7ed2e4ca256f7b3b48fda9001f646986b90`. El mod instalado y empaquetado coincide con el compilado: `fbea9b5491e7dd310790c7d8809f60dcbdf1eda464113f40317252949b996ec7`. Los hashes completos están en SHA256SUMS-2.6.0.txt y manifest-2.6.0.json.

Publicación completada: [release 2.6.0](https://github.com/Doumomentss/TecniHardcore/releases/tag/v2.6.0), marcada como última versión. Los siete archivos de GitHub coinciden en tamaño y SHA-256 con los locales, tanto antes como después de publicar. El feed público del actualizador ofrece 2.6.0 a un launcher 2.5.0, con el hash correcto del setup; una primera consulta devolvió HTTP 502 temporal y la siguiente respondió correctamente. El acceso directo del dueño apunta a la instalación 2.6.0 existente.

Estado consultado desde el anfitrión: localhost 25565 y `rails-acorn.tun.ply.gg:6906` responden con paquete 2.6.0 y datos vigentes. Esto confirma el túnel para la consulta de estado, sin sustituir la entrada de un jugador desde otro equipo. Microsoft Defender terminó el análisis del setup sin detectar amenazas.

## Alcance y pendientes

La recepción desde otro equipo de Internet no se sustituye por una consulta desde el mismo anfitrión. Los presets shader y señales se han comprobado localmente; el estado público se comprobó por Playit tras publicar. No se efectuaron pruebas destructivas en producción. No se garantiza que todas las skins de launchers externos se puedan descubrir por nombre: un nombre sin imagen publicada deberá seleccionar una skin con `/skin set player NombrePremium` o la pantalla de Fabric Tailor.

El instalador permanece sin firma comercial. La verificación por hashes y un análisis antivirus no garantizan que SmartScreen omita advertencias de reputación.
