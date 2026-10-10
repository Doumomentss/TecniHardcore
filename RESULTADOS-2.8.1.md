# Pruebas de TecniHardcore 2.8.1

## Completadas en entorno aislado

- Gradle `build` del mod y las 12 pruebas del launcher.
- Servidor de prueba 1.20.1 con EasyAuth, Carpet, Simple Voice Chat, AntiXray y ServerReplay: bot autenticado, muerte registrada como #10 y archivo MCPR guardado.
- Reproducción por red: 77 chunks, entidades de jugadores y paquetes de movimiento recibidos tras el parche de AntiXray.
- Cliente nativo: reproducción visible y salida con `/replay view close`; captura en `docs/replay-2.8.1.png`.
- Índice por jugador: tests de IDs estables y prueba de servidor con dos muertes de `e25replaybot`, listadas como 0 y 1.
- `/tecni replay play e25replaybot 1`: el servidor confirmó el salto a 30.000 ms antes de la muerte #12; el cliente de red recibió 149 chunks y 351 paquetes de movimiento.
- Instalador Windows 2.8.1 ejecutado con `--dir` en una carpeta aislada; `app.asar` coincide por SHA-256. Desde el paquete instalado se descargaron y validaron los 67 mods externos (69 JAR en total), se reparó un archivo alterado y se conservaron menú y preferencias.
- Pruebas de transacciones del instalador: reemplazo, reversión, recuperación tras interrupción, rechazo de rutas fuera de destino y hashes inválidos.
- Release público 2.8.1: el comprobador validó tamaño y SHA-256 de los siete archivos publicados. Un launcher 2.8.0 detectó la 2.8.1 y descargó el instalador real desde GitHub con tamaño y hash correctos.
- Actualización de la instalación local: el instalador 2.8.1 sustituyó el launcher; los 73 archivos del manifiesto pasaron SHA-256, el acceso directo apunta a su ubicación y el nombre y las preferencias de `%APPDATA%` conservaron exactamente su hash. Cliente y servidor contienen el mismo mod 2.8.1.
- Limpieza de 62 directorios residuales verificados dentro del workspace: 25,15 GiB recuperados, con el mundo y los respaldos de producción preservados.

## Pendientes antes de declarar el despliegue completo

- Comprobar arranque de producción y una grabación real posterior al despliegue. No usar vidas ni inventarios de jugadores para una prueba destructiva.
- Comprobar visualmente el salto automático 30 segundos antes de morir en una grabación nueva con movimiento del jugador.
