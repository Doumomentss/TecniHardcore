# Custodio de Pizarra — arena local

Abre **PROBAR_JEFE.bat** desde este workspace. Necesita Node.js, ya instalado en este equipo. La primera vez puede descargar dependencias y Java 17. Cierra Minecraft para que la ventana de prueba se detenga y guarde su mundo.

La prueba usa el personaje **ProbadorBoss**, una instalación independiente y una arena en `tools/test-runtime/boss-preview`. No utiliza tus vidas ni tu inventario del servidor. Incluye armadura de diamante, espada, arco, escudo y comida. Usa `/tecni prueba reiniciar` para recuperar las tres vidas y crear de nuevo el custodio.

El jefe tiene 320 puntos de salud, armadura, tres fases y ataques anunciados:

- **Onda:** prepara los brazos y marca el perímetro; salta sobre el anillo que avanza.
- **Fractura:** marca una línea hacia tu posición; sal de ella antes del golpe.
- **Cristales:** reúne fragmentos y dispara abanicos de proyectiles; esquívalos lateralmente.

Las fases aumentan al quedar por debajo del 65 % y del 35 % de salud. Los ataques se aceleran y la descarga gana una ronda. El prototipo no rompe terreno ni deja recompensas.

El modelo, textura, emisión, animaciones y sonidos son originales y editables. No requiere shaders. El mod y estos recursos se distribuyen con el paquete, pero el servidor principal rechaza `/tecni jefe invocar x y z` y elimina cualquier ejemplar cargado accidentalmente. No tiene generación natural, huevo de aparición ni receta.

La arena escucha únicamente en **127.0.0.1:25567** y su voz en **127.0.0.1:25568**. No usa Playit ni abre el servidor de pruebas a Internet. Autenticación y Ledger están excluidos únicamente de este mundo local; producción conserva ambos.

Queda por ajustar la dificultad con tus pruebas de combate. La presencia visual, las tres clases de ataque, las tres fases, el daño y la reaparición se comprobaron dentro del juego; esto no equivale a un balance definitivo para publicarlo en supervivencia.
