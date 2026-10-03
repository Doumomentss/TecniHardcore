# TecniHardcore 2.4.0 — pruebas y entrega

Actualización del 2 de octubre de 2026. Minecraft 1.20.1, Fabric 0.19.5, GeckoLib 4.8.4, Iris 1.7.6, Sodium 0.5.13 y Complementary Reimagined r5.9.3. Handshake `pack_v5`, almas `soul_v3`, ritual `ritual_v3`, efectos de jefe `boss_fx_v1`.

## Implementación

El proceso principal del launcher guarda nombre, carpeta y preferencias de forma atómica en `%APPDATA%\TecniHardcore\settings.json`, con copia anterior. Migra el almacenamiento antiguo disponible. Si falta el nombre, pide escribirlo; no interpreta un UUID como nombre ni hereda silenciosamente «Jugador». El selector copia y comprueba los archivos por SHA-256, conservando el original; también permite verificar una instalación ya trasladada. El instalador y el launcher actualizan el acceso directo del escritorio real de Windows. Un fallo del acceso directo no invalida la instalación.

Los cinco perfiles modifican únicamente gráficos. Optimizado es el predeterminado en instalaciones nuevas; las existentes conservan sus preferencias. Seleccionar o restaurar un perfil aplica sus ajustes; arrancar normalmente no reaplica el preset. Calidad y Ultra calidad descargan el ZIP original desde Modrinth, verifican SHA-256/SHA-512 y aplican MEDIUM/ULTRA mediante un archivo externo. El ZIP no se distribuye en el instalador ni en GitHub. Un fallo impide iniciar fingiendo que el shader está activo; el usuario puede reintentar o seleccionar Optimizado. Un error de compilación confirmado ofrece reiniciar sin shaders, conservando la elección del perfil.

El Custodio conserva entidad y UUID durante sus barras 400/500/1000. La segunda y tercera tienen repertorios diferentes. La transformación final dura seis segundos y alterna el modelo de piedra con el Espectro del Núcleo: coraza fragmentada, brazos largos, alas y cristal violeta emisivo. Daño, posiciones fijadas, prisiones, obstáculos y tiempos son autoridad del servidor. Los avisos geométricos siguen visibles con partículas mínimas. Las prisiones duran dos segundos, permiten mirar/atacar/usar objetos y tienen diez segundos posteriores de inmunidad. Los proyectiles de seguimiento giran de forma limitada y caducan en cuatro segundos. No se rompe terreno ni se generan jefes naturalmente.

Todos los tótems usan la misma última activación persistida. Dentro de 48 bloques de un jefe en combate exigen 60 segundos; fuera, 300. Cambiar de zona modifica el requisito, sin reiniciar ni regalar una activación. El esquema 3 deriva la activación antigua del vencimiento menos cinco minutos y conserva vidas, resurrecciones y otros registros.

## Pruebas completadas

- 22 pruebas Java: reglas, migración, pago, selección, fases, lanzamientos diferidos y límites exactos 59/60 y 299/300 segundos.
- 10 pruebas Node: identidad persistente, rechazo de UUID/default heredado, archivo dañado conservado, traslado verificado, conservación de preferencias ajenas a gráficos, fallo de shader, actualización y hashes.
- Launcher real en Electron a 1100×700 y 950×620: identidad y cinco perfiles visibles; corregido el recorte del botón JUGAR AHORA en ventana compacta.
- Minecraft real en una arena aislada, sin usar inventarios ni vidas de Doumoment o su amigo: Iris y Sodium cargan juntos; las doce habilidades se ejecutaron y se capturaron en el framebuffer. Se comprobó la transición por daño de la segunda barra al Espectro de 1000 de vida con el mismo UUID; muerte definitiva de la tercera forma y animación de muerte del jugador conservada.
- Reinicio de la arena durante transformación: fase 3 y UUID conservados; ataques incompletos no se reanudaron. Modelo final restaurado al entrar.
- MEDIUM activo al entrar: Iris informa `Profile: MEDIUM (+0 options changed by user)` y el ZIP correcto. Capturas del modelo emisivo y la transformación con shaders. Después de la compilación inicial, capturas entre 115 y 119 FPS en la RTX 3070 local; no representa otros equipos ni una pelea con carga máxima.
- Tres clientes de protocolo en la arena. Activación vanilla: 59.995 segundos restantes dentro; al salir, 294.317 segundos del mismo uso; después de 60 segundos dentro, cero. Fuera, una activación antes de cinco minutos fue bloqueada. Datos de vidas/enfriamiento sincronizados por `soul_v3`.
- Instalador: recuperación tras interrupción, rollback tras sustitución parcial, preferencias conservadas, rechazo de hash incorrecto y rutas ZIP que escapan de la instalación.

## Verificaciones pendientes de equilibrio y cobertura

La duración de 4–6 minutos contra dos jugadores humanos con diamante sin encantamientos sigue pendiente. Las pruebas de habilidades y rendimiento no validan por sí solas ese equilibrio. Falta completar una pelea real con esquivas, escudos, comida, blancos elevados/lejanos y observador; revisar avisos desde todos los ángulos y medir FPS/TPS bajo varios combates simultáneos. También falta ensayar toda la matriz de cancelación/obstáculos e inmunidad de prisiones y activación de cada reliquia exactamente en los límites temporales dentro del juego, más allá de las pruebas de reglas y sincronización realizadas.

Las pruebas de cliente y protocolo se realizaron localmente; esta entrega no declara una nueva prueba de conexión desde una red externa de Internet.

## Archivos de entrega

`dist/TecniHardcore-Setup-2.4.0.exe`, `dist/update.json`, `dist/manifest-2.4.0.json`, `dist/SHA256SUMS-2.4.0.txt` y `ADMINISTRACION.md`. Las evidencias privadas y logs de prueba se conservan en `tools/test-runtime`; no se exportan cuentas, contraseñas ni datos del mundo.

## Ampliación de contenido, altar y plaza

Se incorporan Simply Swords 1.70.2, Better Combat 1.9.0, playerAnimator 1.0.2-rc1, Immersive Armors 1.7.2, AdventureZ 1.4.20 y Simply Tooltips 0.1.5. Fabric 1.20.1 verificado; dependencias ya presentes reutilizadas. Las seis descargas se fijan por versión, tamaño y SHA-256/SHA-512 desde Modrinth. El catálogo pasa a 58 mods externos y dos propios. No se republican sus JARs. Se corrige por resource pack un identificador erróneo en el icono del libro de AdventureZ. Simply Swords conserva botín único desactivado en aldeas y probabilidades de sus autores; no se añaden armas gratuitas al spawn.

El mod se muestra como **TecniHardcore**, conservando su identificador interno y los objetos existentes. La guía incorpora los mods y la baliza de auxilio. Esta nueva herramienta requiere ocho lingotes de cobre y un fragmento de eco: 16 usos, señal de 60 segundos a compañeros autenticados de la misma dimensión a 256 bloques y enfriamiento de diez minutos persistido por UUID. No cura, resucita ni transporta. La durabilidad se descuenta solo al activar; repetir durante el enfriamiento no gasta otro uso. La mesa de desencantado inverso ya excluye todos los objetos TecniHardcore. El HUD indica dirección y distancia y avisa de armaduras por debajo del 15% de durabilidad.

El pedestal recupera los 28 píxeles de ancho a escala 3,5 (6,125 bloques) y su escalonado original; las patas dejan de sobresalir de su base. Las celdas auxiliares invisibles aportan colisión local al pedestal y las cuatro patas, permiten abrir la misma interfaz y resisten minería, explosiones y pistones. Al retirar administrativamente el núcleo se limpian y se cancela el ritual. La reparación de núcleos existentes solo ocupa aire; conserva bloques ajenos. El cálculo de aterrizaje busca también fuera del pedestal ampliado.

Nueva plaza manual auditada: 1.392 posiciones revisadas, centro existente conservado, caminos ceremoniales, talleres cubiertos, postes y cartel real de bloques TECNIHARDCORE. El generador solo lee regiones; la función valida de nuevo TODOS los bloques antes de colocar el primero. Ningún contenedor se sustituye. La actualización se ensaya sobre copia consistente del mundo y requiere aplicación explícita; no hay función automática de carga.

Daño reforzado: melee 16/20/24 antes de armadura; onda 18, fractura 16, cristales 14; fase 2 triple 20, lluvia 18, prisión 8, embestida 24; fase 3 onda 24, lanzas 18, cadenas 12, convergencia 26, tormenta 22. Los avisos, obstáculos y ventanas de esquiva se conservan. Primera caída/fractura/reconstrucción de cuatro segundos; transformación final de seis segundos; derrota definitiva de seis segundos con caída, separación de alas y dispersión del núcleo. Nueve movimientos específicos originales y audio original por capas, posicional. La desaparición final y su edad se persisten; no se prolongan ataques tras morir.

Se cierra el ejecutor de configuración de Fzzy al terminar un servidor dedicado, después del guardado. Esto evita un proceso Java residual tras un apagado solicitado; no afecta a servidores integrados en el cliente.

### Pruebas de la ampliación completadas

- Cliente nativo y servidor con los seis mods adicionales; inventario/modelos cargados sin incompatibilidad de Fabric/Iris/Sodium/GeckoLib.
- Plaza ampliada aplicada primero a copia restaurada de backup consistente. Captura nativa del cartel y talleres. Todos los UUID, vidas e inventarios originales de esa copia mantienen sus valores/hashes; solo se utilizan cuentas de prueba nuevas.
- Clic nativo en una celda lateral del pedestal abre la selección y detecta Alma24 cerca. Dos rituales completos devuelven una vida y consumen exactamente una unidad: pila 3 → 2 → 1, contador 0 → 1 → 2, sin fallo de posición segura.
- Baliza activada en servidor con cliente observador: aviso, dirección, distancia y tiempo visibles. Segunda solicitud durante enfriamiento rechazada; daño del objeto permanece en 1. Enfriamiento almacenado en el registro de almas.
- Las doce habilidades nuevas se observan en el cliente; las cadenas (10) se comprobaron también sobre un objetivo elevado después de corregir la rotación a distancia. Fase 2 entra por daño con caída propia; fase 3 conserva UUID y 1000 HP. La derrota final genera capturas de caída, fractura y disolución. El tinte rojo vanilla se limita al impacto inicial para mantener visibles cristal y emisión durante la animación final.
- Apagado limpio de la copia con el nuevo conjunto: guardado y salida 0 sin proceso Java residual. Instalación aislada del EXE: 58 descargas oficiales y 60 JARs; hashes, reparación, preferencias y botón personalizado verificados.
- Capturas nativas de estas pruebas entre 112 y 120 FPS en el equipo local. No es una medición de equilibrio entre dos jugadores ni una garantía para otros equipos.

La plaza se aplicó a producción con validación íntegra: 2785 comandos ejecutados, sin sustituir el núcleo. Perfil de ticks después de la ampliación: 333 ticks en 16,60 segundos (20,06 TPS), con cero jugadores; no demuestra rendimiento durante un combate. Instalación aislada verificada nuevamente con el núcleo final, 58 descargas oficiales y 60 mods. Se conserva una advertencia/error de integración JEI/Farmer's Delight ya presente en el paquete anterior; el cliente inicia y carga el mundo, pero esta integración no se declara corregida.

El setup final incorpora icono, metadatos de producto/versión y manifiesto sin elevación. Defender local con protección activa terminó un análisis personalizado sin detectar amenazas; no equivale a reputación SmartScreen. No hay certificado de firma disponible y el EXE sigue sin firma de editor; detalles en SEGURIDAD-INSTALADOR.md. Se reinstaló y verificó este EXE final en carpeta aislada. Tras desplegar, ocho registros originales de almas y todos los hashes NBT de jugadores coinciden con el respaldo previo. El nombre y UUID offline de la cuenta de prueba se conservaron al trasladar C/D. La dirección Playit responde con versión 2.4.0 desde este equipo; continúa pendiente una prueba desde otra conexión de Internet.
