# Verificación de TecniHardcore 2.6.2

## Comprobaciones completadas

- Compilación Fabric y 52 pruebas JUnit, incluidos límites por tipo y escala de destrucción/arrastre.
- Prueba sobre el modelo real Keep Kayra (143×260×143), en el mundo aislado de pruebas y con cuentas de prueba. Ninguna construcción, vida o inventario de producción se utilizó para este ensayo.
- Nivel 4: 3.136 posiciones menos tras unos 20 segundos activos. Nivel 20, radio 512 y anchura 600: tres tandas de 180 segundos activos. La lectura final del mundo guardado encontró 744 bloques de construcción frente a 959.590 en el modelo original: **99,922 % eliminado**. El modelo original y la colocación inicial difieren en 27 posiciones no vacías; no se presenta el porcentaje como una medición exacta de cada bloque destruido.
- Se encontraron además 236.192 posiciones con agua y 5.810 con lava. El contador inicial consideraba estos fluidos como construcción y falló su umbral del 80 %. Se conserva ese resultado original; la verificación corregida separa fluidos y construcción mediante `tools/verify-kayra262.py`, sin modificar el mundo. Los últimos bloques incluyen contenedores, cabezas, fogatas, generadores y obsidiana protegidos.
- Tres ventanas de 2.400 ticks: medias 10,50 / 5,86 / 4,47 ms; percentiles 95 de 17,79 / 7,60 / 5,51 ms. El pico de la primera ventana fue 184,51 ms durante carga y conexión. La colocación inicial de la enorme plantilla produjo una pausa y se distingue del coste del tornado.
- Cliente nativo con HUD y tornado de nivel 20 visibles; captura `docs/t262-kayra-storm.png`. La lectura puntual de 119 FPS no constituye una medición sostenida ni una comparación de rendimiento.
- Instalador final en una carpeta aislada: actualización, 66 mods oficiales y dos propios, hashes, reparación de un mod corrupto y conservación de preferencias y botón de conexión público.
- Respaldo consistente con servidor detenido y restauración byte por byte de 249 archivos del mundo. Conservación de ocho registros de almas y ocho archivos de jugadores durante el despliegue; cliente y servidor comparten el hash final del mod.

## Límites y pendientes

La prueba automática específica de paquetes de velocidad del arrastre no llegó a ejecutarse: esperaba el resultado del contador inicial fallido. La escala de fuerza está cubierta por pruebas unitarias, pero no se declara validada esa comparación física dentro del juego. Tampoco se declara una medición sostenida de FPS ni una prueba visual nueva de todos los shaders. La protección del spawn se conserva y tuvo pruebas en 2.6.1; no se repitió su caso extremo en esta tanda.

Los resultados, registros originales y mundo de ensayo permanecen en `tools/test-runtime/tornado262` y `tools/test-runtime/expansion25`, fuera del repositorio público. Los respaldos de producción contienen datos privados y tampoco se publican.
