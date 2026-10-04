# Verificación 2.6.3

- Compilación del mod y 54 pruebas JUnit sin fallos. Incluye escalado, niveles destructivos, presupuesto y límites visuales por distancia y partículas.
- Estructura aislada: 196.992 bloques de piedra, madera, obsidiana, cofres y escaleras con agua, sobre una base de bedrock. El recuento final encontró cero bloques de construcción en su volumen después de menos de 35 segundos activos a nivel 20. Se verificó que los cofres y la obsidiana desaparecieron y que la bedrock se conservó.
- Sincronización: 52 lotes, 1.664 muestras de bloques reales y 16.070 bytes de carga útil, representando 196.992 eliminaciones. Cero eventos individuales de rotura y ninguna entidad `falling_block` en la zona.
- 800 ticks medidos: media 5,50 ms, percentil 95 de 8,16 ms y máximo de 14,31 ms. El ensayo no demuestra el mismo rendimiento en todos los equipos ni durante todos los tipos de construcciones.
- Cliente nativo con Optimizado: revisadas las capturas de arranque, vórtice denso y parada. Lecturas puntuales de 119 y 118 FPS; no se presenta como medición sostenida ni comparación con una escena idéntica sin efectos.

Las cuentas y el mundo de prueba están aislados. No se utilizaron inventarios ni vidas de Doumoment o su amigo para ensayar destrucción. Se mantienen los resultados locales en `tools/test-runtime/rubble263`.

Pendientes: revisión visual específica de este cambio con shaders, prueba prolongada de FPS y medición del presupuesto máximo sobre estructuras mayores. El nuevo lote se valida y está acotado; la geometría representa materiales reales sin pretender que cada bloque visible sea una entidad física de servidor.
