# TecniHardcore 2.7.0 · Resultados

Fecha: 4 de octubre de 2026. Minecraft 1.20.1 Fabric; cliente y servidor 2.7.0; handshake `pack_v11`. Pruebas con cuentas **E25CivicA/B/C**, E25Guard y E25OreCheck en servidores aislados, puertos locales 25568/25569. No se usaron vidas ni inventarios de Doumoment o sus amigos.

## Comprobaciones completadas

| Área | Resultado observado |
|---|---|
| Compilación y pruebas Java | 66 pruebas, cero fallos; incluye límites de saldo, pertenencia a equipos, protección mutua y validación del registro social. |
| Launcher | 11 pruebas, cero fallos: identidad portable, gráficos, descargas, manifiestos y actualizador. |
| Equipos y chat | Cuentas sin OP crean/invitan/aceptan; el tercero no recibe mensajes privados y sí recibe los generales. Daño atribuido entre compañeros bloqueado. |
| Expulsión | Expulsión de integrante desconectado; contador inmóvil sin conexión, aviso al volver y daño mutuo bloqueado. Política de las dos horas comprobada en pruebas de reglas; no se esperaron dos horas de reloj. |
| Misiones | Entrega exacta de 16 troncos, pago de 25 CT y rechazo del segundo cobro. Interacción nativa con Inés: altura por sí sola no completa exploración; distancia horizontal completa y paga exactamente 15 CT. |
| Mercado | Lote retirado del inventario a custodia; dos compras simultáneas generan una venta, un pago y un buzón. Cancelar devuelve el lote al buzón. |
| Inventario lleno / repetición | Retiro sin espacio conserva el lote. Repetir una solicitud con la misma sesión no entrega un segundo objeto. |
| Reinicios | Recuperación del pago/inventario antes de aplicar inventario, después de guardarlo y tras confirmar el registro. Estado exacto sin duplicación; otros registros conservados. |
| Permisos | Editor rechazado para no OP; mercado rechazado en creativo. Sesiones y distancia comprobadas en servidor. |
| Skins y NPCs | Modelos humanos renderizados. Skin PNG descargada por URL, validada y distribuida desde el servidor; cambio visible en el cliente. NPCs aparecen a Y=96 en la copia del spawn. |
| Spawn interno diferente | Se repitió la prueba con spawn interno −231,75,154: los NPCs permanecen en la plaza 0,96,0 y la misión no se completa automáticamente allí. Solo la exploración horizontal de la plaza habilita el cobro. Se corrigió el cálculo con chunks aún sin cargar. |
| Interfaces nativas | Diálogos, mercader, mercado con siete ofertas, objetos/precios/vendedor, escalas 2 y 4 y pantalla completa; navegación de subpáginas sin omitir ofertas. |
| X-ray | En paquetes reales de chunks, diamante completamente cubierto se transmite como piedra; expuesto se transmite como diamante. El bloque real no se modifica. |
| Fiw | Cliente oficial con el paquete completo aceptado; cliente de prueba que reporta `meteor-client` rechazado por la comprobación. |
| TecniGuard | Ráfaga real de 220 ataques: 140 acciones adicionales bloqueadas. Interacción normal con el NPC vuelve a funcionar al reiniciarse la ventana. |
| Compatibilidad | Cliente antiguo con protocolo 10 rechazado antes de entrar al mundo, con indicación de actualizar a 2.7.0. Cliente completo conecta con los filtros finales sin los errores de Grim. |
| Rendimiento básico | Copia completa, un cliente conectado: 20 TPS; percentil 95 de tick 2,2 ms en los últimos 10 s y 2,6 ms en el último minuto. No constituye una prueba de carga con muchos jugadores. |
| Instalación independiente | Setup desde carpeta vacía; 67 descargas oficiales verificadas y 69 JAR finales. Reparación de JAR corrupto, opciones y botón conservados. Perfil público de Playit sin dependencia de `D:\servers\minecraft`. |
| Instalador | Rollback, recuperación tras interrupción, archivo bloqueado, hash inválido y ruta ZIP que intenta salir del destino; datos de juego conservados. |
| Instalación del propietario | Launcher/cliente actualizados a 2.7.0; nombre, opciones, ruta, perfil local y layout idénticos. Acceso directo apunta al launcher real. |
| Respaldo / producción | Apagado limpio; respaldo con CRC y restauración exacta de los 261 archivos del mundo. Tras arrancar 2.7.0 se comparan almas, archivos de jugadores y propiedades contra la copia; sin alterar vidas, UUIDs o inventarios existentes. |

El servidor de producción quedó encendido con AntiXray, Fiw y TecniGuard. Inés, Bruno y Selma se generan al cargar los chunks de sus espacios libres en la plaza; se comprobó suelo y espacio de aire en el mundo real sin modificar terreno ni el spawn interno. No se copiaron registros de prueba ni cuentas QA a producción.

## Exclusiones y límites

**GrimAC 2.3.73 se descartó** tras comprobar errores de PacketEvents con IDs de objetos modificados y slots ampliados de inventario. Los logs de esa evaluación se conservan en el entorno aislado. No se instala en producción. Su API solo es una dependencia de compilación para la integración experimental y no va dentro del cliente.

Fiw complementa la protección: un cliente manipulado puede mentir sobre sus mods. AntiXray oculta minerales cubiertos, no minerales expuestos ni cofres. TecniGuard limita exceso de acciones y registra desplazamientos a revisar; no hay sanciones automáticas de movimiento ni se declara cobertura completa de aimbot, alcance o todas las trampas.

Quedan para pruebas de carga/revisión adicional: muchos usuarios simultáneos, combate prolongado con todas las combinaciones de armas, evasión avanzada de clientes modificados y comprobación exhaustiva de daño ambiental sin atribución. La protección de equipos no puede atribuir al dueño una trampa ambiental que Minecraft no registra como daño del jugador.

La prueba visual se realizó sin shaders en perfil Optimizado; no se repitieron los dos presets de shader ni todos los eventos/desastres antiguos para esta entrega. Tampoco se declaró una nueva prueba desde otra conexión de Internet: el endpoint público existente se conserva y el cliente antiguo debe actualizar para entrar.

## Evidencias y entrega

Scripts: `test-social27.cjs`, `test-security27.cjs`, `test-guard27.cjs`, `test-social27-recovery.py`, `qa-civic27-native.cjs`, `backup-before27.py` y `verify-production27.py`. Resultados locales bajo `tools/test-runtime/social27` y `tools/test-runtime/expansion25/server/qa-results`. Capturas seleccionadas se exportan al directorio `docs` del repositorio público; no se exportan logs, mundo, cuentas ni respaldos privados.

Instalador: **TecniHardcore-Setup-2.7.0.exe**, aproximadamente **293 MiB**. Versiones/tamaños/hashes en `manifest-2.7.0.json` y `SHA256SUMS-2.7.0.txt`; aviso de actualización en `update.json`. Comandos y reglas en [NPCS-EQUIPOS-MERCADO.md](NPCS-EQUIPOS-MERCADO.md).
