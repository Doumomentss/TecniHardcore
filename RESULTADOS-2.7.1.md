# Verificación 2.7.1

- 68 pruebas JUnit y 12 pruebas del launcher aprobadas.
- Migración ensayada con una copia del registro real: presupuesto 5, +2 a supervivientes, eliminados sin cambios, mismos UUID, historial y enfriamientos. El presupuesto persistido evita repetir la bonificación al reiniciar.
- Minecraft real con una cuenta aislada: cinco cristales, escalas 2 y 4, pantalla completa; cinco muertes reales descontaron 4, 3, 2, 1 y 0. Eliminación y video de muerte conservados. Comando administrativo acepta 5.
- Cliente del propietario sincronizado; opciones y botón personalizado de conexión conservados.
- Respaldo consistente antes del despliegue y restauración exacta de 285 archivos del mundo comprobados. Resultado privado en `tools/test-runtime/lives271/backup-restoration-result.json`; no se publican mundos ni datos de cuentas.

Se probaron los cambios de vidas y presentación. No constituye una nueva prueba de carga ni una repetición completa de jefes, eventos, mercado o todas las combinaciones de shaders.
