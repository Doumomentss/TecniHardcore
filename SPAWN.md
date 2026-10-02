# Spawn central — 2 de octubre de 2026

Plaza sobria centrada en X=0, Z=0, con suelo a Y=95. El único núcleo de resurrección está en **0, 96, 0**. El punto mundial de aparición está en esa zona, con radio 18 para que los jugadores lleguen a suelo libre alrededor del artefacto.

La plaza tiene 64 bloques de diámetro: piedra, andesita y pizarra, cuatro caminos, bancos de abeto, jardines bajos, iluminación y una mesa de crafteo con cortapiedras. El centro se mantiene abierto; no hay pilares ni techos delante de la cámara de ritual. El terreno exterior se suavizó hasta radio 56. Se trasladó el núcleo anterior de -22, 88, 38; no quedó otro núcleo utilizable.

El datapack `server/world/datapacks/tecni_spawn` desactiva la generación natural de nuevos santuarios. Es configuración del servidor: **no requiere otra actualización del launcher**. El modelo y la animación siguen siendo los del paquete 2.2.0. Un operador todavía puede crear núcleos de manera intencional con comandos; no hacerlo si se desea mantener este como único santuario.

## Validación

Antes de modificar el mundo se creó y verificó el respaldo `server/backups/tecnihardcore/world-1790967145834.zip`. La construcción se ensayó primero en `tools/test-runtime/server/spawn-preview`, una copia de ese respaldo.

Minecraft real, con cuentas de prueba, confirmó la vista de la plaza, una resurrección completa, la cámara desde atrás, el descenso azul, el aterrizaje y el regreso de la cámara. El objetivo recibió una vida y se consumió un solo corazón de una pila de dos. Capturas en `tools/test-runtime/spawn-visuals`.

El servidor principal confirmó el núcleo central y el registro persistente de santuarios. La lectura de todas las regiones existentes del Overworld confirmó un solo núcleo. No se usaron las vidas ni el inventario de Doumoment para las pruebas.

Durante el apagado posterior a la edición masiva, Ledger alcanzó su límite de cinco minutos al vaciar la cola de auditoría y lanzó `TimeoutCancellationException`. Los chunks y los jugadores ya estaban guardados; el proceso terminó por sí mismo con código 0, sin cierre forzado. No se garantiza que Ledger haya registrado cada bloque de esta construcción. Para deshacerla se conserva el respaldo completo y las funciones de construcción, además de las verificaciones del mundo guardado. El registro de vidas y pagos es independiente de Ledger y pasó las comprobaciones de conservación.

Se recolocaron seis posiciones de desconexión dentro del terreno modificado en 0.5, 96, -18.5, para evitar asfixia o caídas al reconectar. Se verificó que todos los demás campos NBT —inventario, experiencia, salud, UUID, modo de juego y demás datos— se conservaran. Los originales están en `server/backups/spawn-0-0-20261002/playerdata`; el registro detallado de posiciones queda privado en `tools/test-runtime/spawn-relocations.json`. Las vidas, resurrecciones y enfriamientos del registro de almas no se modificaron.

## Mantenimiento y restauración

La construcción no se ejecuta al arrancar. `function tecni_spawn:build` tiene una protección contra repeticiones; el marcador `#built th_spawn` vale 1 cuando termina. No reiniciar el marcador para redibujar la plaza sobre construcciones posteriores.

Para volver al mundo anterior: apagar con `stop` y esperar a que termine, guardar aparte el mundo actual y extraer el respaldo completo en el directorio `server/world`. Ese respaldo precede tanto a la plaza como al datapack y a las posiciones recolocadas. No mezclar solo algunas regiones del respaldo con el registro de almas actual.
