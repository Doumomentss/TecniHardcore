# Launcher 2.4.1 — actualización fiable

La captura reportó `EPERM` al renombrar el instalador descargado. El registro de la primera instalación confirmó un bloqueo de `chrome_100_percent.pak` por otro proceso. La descarga había terminado, pero la instalación no pudo reemplazar los archivos del launcher.

## Corrección

- Cada descarga usa un EXE y archivo parcial distintos. Los reintentos no reemplazan un instalador anterior todavía en ejecución ni comparten archivos temporales.
- El porcentaje llega a 100 solo después de verificar tamaño, SHA-256 y renombrado. El texto distingue descarga de instalación; un error muestra «La actualización no se completó».
- El setup muestra una ventana de instalación. Extrae y verifica una sola vez; espera hasta tres minutos por los archivos ocupados antes de iniciar la transacción. Si siguen bloqueados, explica cómo cerrar las otras ventanas y reintentar, conservando el error detallado en TEMP.
- Verifica de nuevo los hashes de los archivos instalados antes de confirmar la transacción, actualizar el acceso directo y abrir el launcher. Los fallos de reemplazo conservan el rollback.
- Se separa versión del launcher (2.4.1) de mecánicas (2.4.0). No cambia el mod, el protocolo, el mundo ni requiere reiniciar el servidor.

## Pruebas completadas

- Cinco pruebas Node del actualizador, incluidas dos descargas simultáneas y conservación del EXE anterior. Se mantienen las seis pruebas de identidad, traslado y gráficos.
- Pruebas Windows/.NET de recuperación, rollback, hashes y rutas. Un bloqueo exclusivo del `.pak` retrasa la instalación hasta liberarse; un bloqueo permanente termina con explicación de reintento, sin modificar el archivo.
- Ensayo del instalador completo sobre una instalación 2.4.0 aislada: bloqueo real de `.pak`, ventana con estado de espera, liberación, instalación 2.4.1 y apertura del nuevo launcher. El hash de `game/options.txt` se conserva exactamente. Captura nativa de la propia ventana en `docs/actualizacion-espera.png`.
- Verificación del paquete instalado, 58 descargas oficiales y 60 mods, reparación, preferencias y botón ENTRAR AL SERVIDOR. El mod conserva su hash de 2.4.0.
- Defender local analizó el setup 2.4.1 sin detectar amenazas. Sigue sin certificado de editor; no se garantiza ausencia de SmartScreen.

El resultado en el equipo del amigo requiere que instale el parche y confirme la versión. Si su launcher anterior quedó en un bucle, debe cerrar todas sus instancias e instalar el setup 2.4.1 en la misma carpeta, sin borrar el juego.
