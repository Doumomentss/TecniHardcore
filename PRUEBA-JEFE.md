# Custodio de Pizarra — arena local

Abre **PROBAR_JEFE.bat** desde este workspace. Necesita Node.js, ya instalado en este equipo. La primera vez puede descargar dependencias y Java 17. Cierra Minecraft para que la ventana de prueba se detenga y guarde su mundo.

La prueba usa el personaje **ProbadorBoss**, una instalación independiente y una arena en `tools/test-runtime/boss-preview`. No utiliza tus vidas ni tu inventario del servidor. Incluye armadura de diamante, espada, arco, escudo y comida. Usa `/tecni prueba reiniciar` para recuperar las tres vidas y crear de nuevo el custodio.

El jefe tiene tres barras consecutivas de 400, 500 y 1000 puntos de salud (1900 en total), 18 de armadura y 8 de dureza. Golpea cuerpo a cuerpo con daño 16/20/24 antes de armadura según la fase. La primera forma tiene tres ataques anunciados:

- **Onda:** prepara los brazos y marca el perímetro; salta sobre el anillo que avanza.
- **Fractura:** marca una línea hacia tu posición; sal de ella antes del golpe.
- **Cristales:** reúne fragmentos y dispara abanicos de proyectiles; esquívalos lateralmente.

Al agotar la primera barra, cae y reconstruye su coraza durante cuatro segundos. La segunda forma usa fractura triple, lluvia dirigida, prisión y embestida. Al agotarla, se transforma durante seis segundos en el Espectro del Núcleo: modelo nuevo, alas y núcleo violeta. Su repertorio incluye doble onda, lanzas perseguidoras, cadenas, convergencia y tormenta. Los objetivos lejanos reciben una rotación completa de ataques de alcance. Solo muere al agotar la tercera barra, con una disolución animada de seis segundos. `/kill` administrativo lo retira directamente. Las pausas bajan a 0,8 / 0,6 / 0,4 segundos según fase; se conservan avisos mínimos y se fijan posiciones y tiempos al iniciar cada habilidad. No rompe terreno ni deja recompensas. Consulta ADMINISTRACION.md para daños y límites.

Los operadores de nivel 4 pueden invocarlo en el mundo principal con `/tecni jefe invocar ~ ~ ~`, fuera de los 32 bloques de radio de la plaza. Se habilita con `-Dtecni.allowTrialBoss=true`, independiente del modo de pruebas. Se rechaza un segundo jefe a menos de 100 bloques. Para retirarlo, usa `/kill @e[type=tecnihardcore:custodio_pizarra,distance=..100]`.

El modelo, textura, emisión, animaciones y sonidos son originales y editables. No requiere shaders. Necesita el paquete 2.4.0; los clientes anteriores deben actualizar. No tiene generación natural, huevo de aparición ni receta.

La arena escucha únicamente en **127.0.0.1:25567** y su voz en **127.0.0.1:25568**. No usa Playit ni abre el servidor de pruebas a Internet. Autenticación y Ledger están excluidos únicamente de este mundo local; producción conserva ambos.

Queda por ajustar la dificultad con tus pruebas de combate. La presencia visual, las tres clases de ataque, las tres fases, el daño y la reaparición se comprobaron dentro del juego; esto no equivale a un balance definitivo para publicarlo en supervivencia.
