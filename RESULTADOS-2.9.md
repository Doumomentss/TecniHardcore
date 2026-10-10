# Verificación de TecniHardcore 2.9.0

Pruebas completadas en copia aislada, sin usar vidas ni inventarios de Doumoment ni de su amigo:

- Respaldo de 549 archivos antes del cambio: ZIP de 296.942.420 bytes, SHA-256 `9df9814e7c875b0a1684483fb13978a1e7c22ea8c37104dd94d5be8e057f978b`. Extracción completa y comparación de hashes en directorio aislado: correctas.
- Semilla nueva `20261009`: 9.845 columnas inspeccionadas dentro del radio 56 sin agua superficial. Las dimensiones nuevas se generaron sin copiar regiones antiguas.
- Traslado de la plaza: 115.524 posiciones entre Y 95 y 130 cotejadas con el original, sin diferencias de bloque ni propiedades. Núcleo (0,96,0), tablón (6,96,-17), spawn seguro (0,96,20), tres PNG de NPC comprobados por SHA-256.
- Estado inicial: almas vacías y límite de cinco vidas; inventarios, estadísticas y logros sin datos previos; registros nuevos de equipos, moneda, mercado, recompensas y monturas vacíos. Datapacks antiguos ausentes del baseline; generación de otros santuarios desactivada.
- Build Fabric 2.9.0 y pruebas JUnit: correctos. Inicio real del servidor de prueba y conexión de un cliente Fabric 2.9.0: correctos.
- Cuenta de prueba existente: intro tras autenticación, final registrada a los 30 segundos; reconexión sin repetirla. Reinicio administrativo del registro, desconexión a mitad de la secuencia y reconexión: se reprodujo completa y solo entonces quedó registrada.
- Cuenta nueva de prueba: `/register` con contraseña de 12+ caracteres, intro posterior y 5/5 vidas. Capturas en ventana a escala de interfaz normal y 4; intento de abrir inventario durante la intro no interrumpió la toma.

Validaciones adicionales:

- Dos clientes de prueba autenticados recibieron la introducción al mismo tiempo y ambos registraron su finalización por separado.
- El arranque del servidor QA con Java 21 y su apagado limpio dieron código 0. Se eliminó del catálogo de servidor la segunda versión de Fabric Language Kotlin, que causaba fallos al detener.
- El instalador 2.9.0 reconstruido se ejecutó en una carpeta aislada. Se extrajo su `app.asar` y se comprobó el mod final, catálogo de tres shaders opcionales, perfil Juana manso, 69 mods instalados, botón de conexión, reparación y conservación de preferencias. Se ensayó una actualización desde 2.8.1 en otra carpeta aislada.
- Descargas oficiales de BSL 10.1.8, MakeUp Ultra Fast 9.5g y Complementary Unbound r5.9.3 cotejadas por tamaño y SHA-512; se instalaron en la carpeta del cliente de pruebas sin activarse. Los perfiles de calidad siguen con Complementary Reimagined.
- Fiw AntiCheat admitió un cliente 2.9.0 con 195 identificadores de mods oficiales y rechazó otro con un mod QA adicional. La comprobación usa el reporte del cliente y no demuestra autenticidad de launcher, shaders o resource packs.
- La whitelist contiene 23 nombres únicos, incluidos los añadidos posteriormente. UUID de Doumoment y LukitasLA2244 cotejados con el `usercache.json` del servidor.
- El prechequeo de arranque detectó el puerto UDP 24454 ocupado por el servidor de pruebas y explicó el conflicto, sin lanzar una segunda instancia. La precarga Chunky de un cuadrado de radio 512 completó 4.225 chunks en 1:11, con apagado limpio. El verificador de temporada volvió a cotejar los 115.524 bloques de la plaza tras la precarga.
- El contador de gracia arrancó en QA, persistió en `tecnihardcore-phase.json` y solo descontó tiempo con el servidor encendido. Las reglas de distancia y reloj superaron pruebas unitarias; la prueba de PvP y pérdida de vidas con dos jugadores reales sigue pendiente.

Pendientes de comprobar durante o después del despliegue: conexión externa por Playit, entrada de las cuentas reales, PvP y muertes durante la gracia, visibilidad de recetas en JEI y los shaders en cada GPU. Los clientes manipulados pueden falsear el reporte de mods: el control no es una garantía contra trampas.
