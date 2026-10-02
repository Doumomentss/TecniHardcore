# Spawn de TecniHardcore

Plaza centrada en **0, 0**, suelo Y=95 y núcleo en **0, 96, 0**. Diámetro de 64 bloques, espacio central sin techos ni pilares, taludes de transición hasta radio 56. Materiales sobrios: piedra, andesita, pizarra, madera de abeto y jardines bajos.

Este datapack es **solo del servidor**. Sobrescribe la colocación natural `tecnihardcore:sanctuary` con un contador cero: no aparecerán nuevos núcleos al explorar. El único núcleo anterior, en -22, 88, 38, se traslada a la plaza; la decoración antigua queda integrada en el terreno.

La construcción no se ejecuta al cargar ni cada tick. Se aplica manualmente con `function tecni_spawn:build`, en 40 etapas espaciadas para limitar el trabajo por tick. Un marcador impide volver a ejecutarla, incluso mientras se construye. No debe borrarse ese marcador ni ejecutarse sobre futuras construcciones: primero restaurar una copia y auditar el área.

El spawn mundial está en 0, 96, 0, con radio de llegada de 18 bloques, para evitar que los jugadores nazcan dentro del núcleo. Se conserva la dificultad difícil, las vidas, los inventarios y el resto del mundo. Los operadores pueden crear otros núcleos expresamente con comandos administrativos; no se generarán de manera natural.

Generador auditable: `tools/build-spawn.py`. Lee los chunks, comprueba conflictos y crea funciones Minecraft; no edita los archivos de regiones directamente. La mesa de crafteo localizada en -16, 102, 12 se traslada al área de servicios de la plaza.
