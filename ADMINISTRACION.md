# TecniHardcore 2.5: Cataclismos y Bestias de Cristal

Actualizado el 3 de octubre de 2026. Minecraft 1.20.1, Fabric 0.19.5, TecniHardcore 2.5.0, protocolo de paquete 6, protocolo de ritual 3, GeckoLib 4.8.4 y EasyAuth 3.3.6. Las instrucciones describen la versión candidata; consulta [las pruebas y el estado de despliegue](RESULTADOS-2.5.md) antes de distribuirla.

La guía [Eventos y monturas](EVENTOS-Y-MONTURAS.md) contiene comandos copiables para los cinco desastres, las cinco monturas, los tres eventos y la recuperación de incidentes. Esas funciones no empiezan aleatoriamente. Los desastres dañan jugadores sin romper bloques; los biomas se añaden únicamente a terreno nuevo. Respaldar el mundo antes de cambiar el mod o instalar los biomas y reiniciar limpiamente.

## Arranque y acceso

Ejecutar `INICIAR_SERVIDOR.bat` y esperar `Done`. Se usa Java 17 y el agente de Playit vinculado. El script evita arranques duplicados y reinicia después de un fallo. Escribir `stop` en la consola guarda el mundo y apaga sin reinicio automático. Actualmente se utilizan 6 GB máximos, visión 11 y simulación 8. No se ha configurado arranque automático con Windows: después de un corte de luz hay que encender la PC y ejecutar el BAT. La suspensión también interrumpe el servicio.

Dirección pública: `rails-acorn.tun.ply.gg:6906`. Perfil local: `127.0.0.1:25565`. Conservar el agente y el túnel existentes para mantener la dirección asignada. No distribuir los secretos de Playit ni las carpetas privadas del servidor.

Entregar a los jugadores el instalador de la versión publicada en la [última Release de GitHub](https://github.com/Doumomentss/TecniHardcore/releases/latest). El candidato 2.5 incorpora modelos, texturas, sonido, menú y los dos mods propios. Sus 65 mods externos se obtienen de fuentes oficiales en el primer preparado, con hashes fijados. La distribución usa el perfil público y conserva el botón personalizado ENTRAR AL SERVIDOR. Cerrar Minecraft antes de actualizar. Las copias de archivos sustituidos quedan en `backups`; las preferencias existentes se conservan y se activa el resource pack oficial sin borrar los demás.

El launcher consulta GitHub al abrirse, cada hora y desde Ajustes. Una Release posterior muestra ACTUALIZAR: descarga verificada, cierre del launcher, instalación con recuperación y reapertura. Los launchers anteriores al actualizador necesitan instalar 2.4.0 una vez. Para publicar otra versión, actualizar `launcher/package.json` y `package-lock.json`, preparar el paquete y ejecutar `tools/publish-release.ps1`; no subir el workspace entero. El script publica exclusivamente la copia permitida de `publish/TecniHardcore`. La actualización no reinicia ni actualiza por sí sola el servidor del propietario.

Los clientes anteriores reciben un mensaje para actualizar. No mezclar distintas versiones del mod en `mods`.

## Registro y permisos

Cuentas nuevas: `/register CLAVE CLAVE`, usando dos veces la misma contraseña de 12 a 128 caracteres. En las siguientes conexiones: `/login CLAVE`. No hacen falta códigos. Las cuentas existentes conservan contraseña, UUID, inventario y permisos.

Las identidades anteriores reservadas que todavía no se registraron deben hacerlo una vez desde la conexión local exacta de este equipo. Las conexiones públicas de Playit no tienen ese privilegio. Después pueden iniciar sesión desde cualquier conexión. Doumoment conserva operador de nivel 4; no se han restaurado ni descontado sus vidas para estas pruebas.

## Cómo resucitar

El único santuario está en el centro del spawn: **X 0, Y 96, Z 0**. La plaza tiene 64 bloques de diámetro y conserva espacio abierto para la cámara, la aparición y el descenso. El núcleo anterior se trasladó y el datapack del mundo `tecni_spawn` desactiva la generación natural de nuevos santuarios. No hace falta actualizar el launcher por la construcción del spawn. La configuración y restauración están documentadas en [SPAWN.md](SPAWN.md).

1. El eliminado debe estar autenticado, en espectador y a menos de cuatro bloques del núcleo.
2. Otro jugador autenticado, con vidas y en supervivencia, se acerca al mismo núcleo con un Corazón Sagrado equipado en cualquiera de sus manos.
3. Interactúa con el núcleo o utiliza el corazón junto a él, selecciona explícitamente al eliminado y pulsa **Iniciar resurrección**. También puede usar `/tecni ritual Nombre`.
4. Ambos permanecen cerca durante 30 segundos. El oficiante mantiene equipado el corazón. Daño, distancia, desconexión, cambio de ofrenda o desaparición administrativa del núcleo cancelan sin consumirlo.
5. Al completar se consume exactamente una unidad y el eliminado vuelve con **una vida**, salud y hambre completas, en una posición libre y segura. Si no hay posición segura, se cancela sin coste.

Las resurrecciones son ilimitadas, incluso para quien ya fue resucitado antes de 2.1. El contador es historial, no un límite. No se pueden recuperar vidas mediante este ritual mientras se sigue vivo. Un bloque de netherita común ya no funciona como altar. Cada santuario y participante solo puede tener un ritual activo; la dispersión final dura cuatro segundos.

Receta del Corazón Sagrado: cuatro estrellas del Nether en las esquinas, cuatro lingotes de netherita en los lados y un cristal del End en el centro. La guía nueva explica las reliquias, efectos, costes y reglas. Pedir una copia con `/tecni guia` si se conserva un libro antiguo.

El núcleo no se fabrica, no suelta objetos y resiste minería, explosiones y pistones. Su torre y anillos tienen escala 3,5, alcanzando aproximadamente 10–11 bloques durante la activación; la base conserva su tamaño. La decoración exterior sí puede modificarse. Los santuarios nuevos requieren espacio libre de 11×11×14. Los existentes se conservan y reciben el modelo nuevo sin regenerar el terreno.

El ritual expande una cúpula negra de 32 bloques de radio en cuatro segundos. Oscurece el entorno de quienes entren, conservando la iluminación del artefacto. A los diez segundos comienza una cámara detrás del oficiante para ambos participantes; los demás observadores conservan su cámara. La toma busca ángulos libres antes de acortarse para evitar atravesar bloques. La skin del objetivo se materializa sobre el cristal y, tras confirmar el pago a los 30 segundos, desciende durante cuatro segundos con una hélice y aura azules hasta una posición segura próxima al frente del oficiante. La aparición es visual; la posición real de reaparición permanece en el suelo, con resistencia durante esos cuatro segundos.

La cámara, movimiento y vista normal se recuperan al terminar, cancelar, desconectarse o abrir una pantalla. `Esc` permite salir de la cinemática sin cancelar el ritual. No se cambian las preferencias de perspectiva, la posición del oficiante ni la orientación guardada. `/tecni-efectos` alterna partículas entre 100 %, 50 % y 25 %; respeta las opciones de partículas y el volumen del juego. La cúpula, el núcleo y la aparición permanecen para conservar la presentación incluso con partículas reducidas.

## Comandos

- `/tecni guia`: entrega la guía actualizada.
- `/tecni ritual Nombre`: inicia el mismo ritual con un objetivo explícito.
- `/tecni vidas Nombre`: consulta vidas e historial de un jugador conectado; requiere operador.
- `/tecni vidas Nombre 0..3`: ajuste administrativo registrado. Ejemplo: `/tecni vidas Doumoment 3`. No reinicia enfriamientos ni borra historial.
- `/tecni santuario crear x y z`: crea la ruina de 11×11 tras comprobar suelo natural seco, altura y espacio libre; requiere operador. Las coordenadas son las del núcleo, un bloque encima del suelo ceremonial. La zona debe estar cargada. Rechaza bloques ocupados y contenedores.
- `/tecni santuario localizar`: muestra el santuario descubierto más cercano en la dimensión; requiere operador. No busca ni genera chunks lejanos.
- `/tecni backup`: copia consistente; requiere operador.
- `/spark tps`: mide TPS y duración de ticks.

En la consola del servidor se omite `/`. Para eliminar un núcleo administrativamente, usar `setblock x y z air`: cancela el ritual sin coste. No copiar un núcleo con herramientas externas para saltarse la validación del emplazamiento.

## Copias, migración y restauración

Copia previa a 2.1: `backups/before-sanctuaries-21-20261002-023726.zip`. Incluye mundo, mods, configuración, autenticación y permisos. Se realizó con Minecraft y SQLite cerrados, verificando y extrayendo sus 214 archivos por SHA-256. El mundo extraído también arrancó con 2.1 en el puerto de pruebas y conservó los datos.

Copias periódicas cada seis horas en `server/backups/tecnihardcore`, conservando las siete últimas. El servidor guarda jugadores, vidas y mundo y pausa ticks durante la copia, verifica CRC y conserva las anteriores ante un fallo. Se reintenta en cinco minutos. Estas copias contienen los datos del mundo; la copia previa a 2.1 además incluye configuración y mods.

Para restaurar: detener el servidor, conservar la carpeta actual aparte y extraer el mundo elegido a una carpeta nueva. No mezclar regiones de copias distintas. Recuperar también mods, configuración y autenticación compatibles cuando se retrocede de versión. Nunca sobreescribir un mundo activo.

La autoridad es `world/tecnihardcore-souls.json`, esquema 3. La migración conserva vidas y enfriamientos y convierte `revived=true` en una resurrección histórica. El registro de pagos permite recuperar un cobro interrumpido descontando solo una unidad de una pila. No editar este archivo mientras el servidor está encendido ni borrar transacciones pendientes.

Los ensayos destructivos se hicieron en mundos aislados y cuentas TecniSoul de prueba. La copia en `tools/test-runtime/server` debe quedar apagada durante el uso normal; su puerto Minecraft es 25566 y el de voz 24455. Los resultados y límites de validación están en `RESULTADOS-2.1.md`.

## Plaza, tablón y novedades (2.3)

La plaza del Overworld está protegida en un radio de 32 bloques, entre Y90 e Y128. Los operadores pueden editarla. Los demás pueden usar el santuario, las mesas y el tablón; se bloquean roturas, colocaciones, explosiones, pistones y líquidos que invadan el área. El daño normal se cancela dentro de ella, y los monstruos se trasladan fuera; el resto del mundo conserva su dificultad. `/kill` administrativo conserva su comportamiento.

El tablón de reliquias está en **6, 96, −17**, orientado hacia el norte. Se puede pulsar tanto abajo como sobre sus imágenes. `/tecni tablon crear x y z` exige un espacio libre de 3×3 y suelo sólido. Para retirarlo, eliminar su bloque central elimina también sus paneles auxiliares.

El launcher muestra jugadores autenticados, vidas, contador de resurrecciones, versión y novedades usando el puerto Minecraft. Las novedades se editan en `server/config/tecnihardcore/public-news.json`, como una lista de objetos con `title` y `body`; máximo cinco anuncios, 100 caracteres de título y 600 de texto. No admite HTML. El servidor relee el archivo cada cinco segundos. No añadir datos privados a este archivo público.

Para el jefe experimental, consultar [PRUEBA-JEFE.md](PRUEBA-JEFE.md). No se puede invocar en producción.

## Launcher portable y gráficos 2.4

El nombre y la carpeta viven en `%APPDATA%\TecniHardcore\settings.json`, con copia `.previous`. Mover el ejecutable no cambia la identidad. Si el nombre falta, hay que escribir el nombre original; nunca adivinar otro, porque cambiar el nombre offline cambia el UUID y el inventario asociado. En Ajustes, «Carpeta del juego» permite trasladar una copia verificada o usar una carpeta ya trasladada. La original se conserva para recuperar archivos si hace falta. El escritorio usa la ubicación real de Windows, también con OneDrive.

La pantalla Jugar ofrece Vanilla, Optimizado, Ultra optimizado, Calidad y Ultra calidad. Solo modifica gráficos al seleccionar o restaurar. Instalaciones nuevas: Optimizado. Calidad descarga Complementary Reimagined r5.9.3 original, MEDIUM; Ultra calidad, ULTRA. Créditos: Complementary Development / EminGT. No se redistribuye su ZIP. Reintentar o elegir Optimizado ante un fallo de descarga. Si la compilación falla, cerrar Minecraft y usar «Reiniciar sin shaders»; se conserva la preferencia del perfil. La preparación de shaders se hace antes de conectar para evitar el timeout del primer ingreso.

## Custodio 2.4

Invocación administrativa: `/tecni jefe invocar x y z`, fuera de la plaza protegida y sin otro jefe cercano. No hay generación natural. Tres barras de 400, 500 y 1000 de vida; seis segundos de transformación a Espectro del Núcleo tras la segunda. Daños base antes de armadura/escudo, marcas visibles para esquivar y sonidos en volumen de criaturas hostiles. Las prisiones duran dos segundos y dejan diez segundos de inmunidad posterior. El jefe limita su persecución y no rompe construcciones. Para retirarlo: `/kill @e[type=tecnihardcore:custodio_pizarra]`; se limpian sus proyectiles y efectos.

Los tótems exigen 60 segundos desde el último uso dentro de 48 bloques de un jefe en combate y 300 fuera. Salir y entrar no reinicia el contador. El HUD muestra «Zona del jefe: 1 minuto». Se conservan exclusiones de vacío y `/kill`.

Consultar `RESULTADOS-2.4.md` para pruebas realizadas y pendientes; la duración de 4–6 minutos contra dos jugadores humanos con diamante todavía requiere una pelea de evaluación.

## Contenido y plaza 2.4

El nombre visible del mod es TecniHardcore. Se suman Simply Swords, Better Combat, playerAnimator, Immersive Armors, AdventureZ y Simply Tooltips. Los JARs externos se descargan con hashes fijados desde sus autores. El libro `/tecni guia` explica la baliza de auxilio y las mecánicas; JEI muestra las recetas nuevas.

Baliza de auxilio: ocho lingotes de cobre y un fragmento de eco. La señal dura 60 segundos, llega a compañeros autenticados de la misma dimensión a 256 bloques, tiene 16 usos y diez minutos de enfriamiento por UUID. No ofrece vidas, curación ni transporte. La señal desaparece al desconectarse su emisor; el enfriamiento sobrevive a reconexiones y reinicios. La armadura con 15% o menos de durabilidad genera aviso en el HUD.

El pedestal del santuario tiene colisión por celdas protegidas y abre su interfaz desde la base y las patas. Retirar administrativamente el núcleo elimina las celdas asociadas y cancela el ritual. No retirar soportes individuales. En núcleos existentes, la reparación coloca solo en aire, conservando construcciones; si alguien añadió bloques dentro de su modelo, el operador debe despejarlos explícitamente.

La plaza ampliada mantiene núcleo en 0,96,0 y protección de 32 bloques, añade talleres, caminos, luces y un cartel construido TECNIHARDCORE. `tools/build-spawn24.py` audita el mundo y produce `tools/spawn24-pack`; no escribe regiones. Se copia como datapack del servidor y se aplica una vez con `function tecni_spawn24:upgrade`, después de backup y prueba sobre copia. La función revalida todas las posiciones; si alguna cambió, cancela sin colocar ningún bloque. No aplicar el constructor antiguo del spawn sobre una plaza existente.

El daño base del Custodio aumenta según fase: melee 16/20/24; ataques finales hasta 26 antes de armadura. La primera transición dura cuatro segundos, la transformación final seis y la derrota definitiva seis. Se conserva entidad/UUID, sin repetir impactos al reiniciar. El equilibrio de duración requiere una pelea real de dos jugadores con diamante sin encantamientos; las pruebas automatizadas no sustituyen esa medición.
