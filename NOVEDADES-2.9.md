# TecniHardcore 2.9.0 · Temporada nueva

Overworld, Nether y End nacen de una semilla nueva (`20261009`). La plaza original permanece en **0, 96, 0** con su santuario, tablón, cartel y NPCs. El terreno se une a la plaza; fuera de ella no se trasladan chunks, cuevas ni construcciones.

Todos comienzan en supervivencia, con **5/5 vidas** y progreso vacío. Se conservan cuentas EasyAuth, contraseñas, UUID y permisos. La primera entrada autenticada reproduce una introducción individual de 30 segundos; si se interrumpe, empieza de nuevo al reconectar.

El operador inicia el primer día con `/tecni fase 1`. Durante 24 horas de **servidor encendido** se muestra “Fin del día de gracia”, no se pierden vidas y el PvP queda desactivado. Cada jugador autenticado recibe el libro “Día de gracia”. Al agotarse, la dificultad pasa a Normal; en días sucesivos pasa a Difícil y crece gradualmente. Los enemigos hostiles ganan vida, daño y probabilidad acotada de refuerzo cada 3.000 bloques desde 0,0: +2 %, +4 %, +8 %, etc.

La whitelist inicial incluye los 23 nombres proporcionados. Para sumar otro, ejecutar `AGREGAR_JUGADOR.bat`; el servidor recarga la lista. El arranque comprueba los puertos de Minecraft y voz antes de abrir otra instancia y detiene los reintentos si encuentra un puerto ocupado. Chunky precargó 4.225 chunks en torno al spawn.

El launcher incorpora **Juana manso**, perfil de renderizado de 4 chunks y efectos mínimos. Ofrece BSL, MakeUp Ultra Fast y Complementary Unbound como shaders opcionales descargados desde Modrinth y verificados por hashes; los perfiles Calidad siguen usando Complementary Reimagined. Los paquetes opcionales no se activan solos. La ventana de actualización solo indica que hay una versión nueva.

El paquete oficial sincroniza mods, pack y recetas de TecniHardcore; JEI muestra las tres reliquias, Corazón Sagrado y Baliza de auxilio. Las adiciones externas en las carpetas de mods, shaders y resource packs se mueven a respaldos al iniciar. El servidor contrasta los mods reportados por el cliente con una lista oficial y rechaza versiones ajenas. Esto no prueba de forma infalible el launcher ni los archivos locales de un cliente alterado.

**Para entrar:** cerrar Minecraft, actualizar el launcher, abrir el servidor con `INICIAR_SERVIDOR.bat` y usar **ENTRAR AL SERVIDOR**. El respaldo privado anterior a la temporada está en `season-archives/season-2026-10-09-pre29.zip`; no compartirlo.
