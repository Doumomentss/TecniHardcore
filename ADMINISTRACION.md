# TecniHardcore 2.2: La cúpula de las almas

Actualizado el 2 de octubre de 2026. Minecraft 1.20.1, Fabric 0.19.5, TecniHardcore 2.2.0, protocolo de ritual 3, GeckoLib 4.8.4 y EasyAuth 3.3.6.

## Arranque y acceso

Ejecutar `INICIAR_SERVIDOR.bat` y esperar `Done`. Se usa Java 17 y el agente de Playit vinculado. El script evita arranques duplicados y reinicia después de un fallo. Escribir `stop` en la consola guarda el mundo y apaga sin reinicio automático. Actualmente se utilizan 6 GB máximos, visión 11 y simulación 8. No se ha configurado arranque automático con Windows: después de un corte de luz hay que encender la PC y ejecutar el BAT. La suspensión también interrumpe el servicio.

Dirección pública: `rails-acorn.tun.ply.gg:6906`. Perfil local: `127.0.0.1:25565`. Conservar el agente y el túnel existentes para mantener la dirección asignada. No distribuir los secretos de Playit ni las carpetas privadas del servidor.

Entregar a los jugadores `dist/TecniHardcore-Setup-2.1.1.exe` o el enlace de la [última Release de GitHub](https://github.com/Doumomentss/TecniHardcore/releases/latest). Este launcher incluye el actualizador; las mecánicas siguen en 2.1.0. El instalador incorpora modelos, texturas, sonido, menú y los dos mods propios. Los 51 mods externos se obtienen de sus fuentes oficiales en el primer preparado, con hashes fijados. La distribución usa el perfil público y conserva el botón personalizado ENTRAR AL SERVIDOR. Cerrar Minecraft antes de actualizar. Las copias de archivos sustituidos quedan en `backups`; las preferencias existentes se conservan y se activa el resource pack oficial sin borrar los demás.

El launcher consulta GitHub al abrirse, cada hora y desde Ajustes. Una Release posterior muestra ACTUALIZAR: descarga verificada, cierre del launcher, instalación con recuperación y reapertura. Los launchers antiguos necesitan instalar 2.1.1 una vez. Para publicar otra versión, actualizar `launcher/package.json` y `package-lock.json`, preparar el paquete y ejecutar `tools/publish-release.ps1`; no subir el workspace entero. El script publica exclusivamente la copia permitida de `publish/TecniHardcore`. La actualización no reinicia ni actualiza por sí sola el servidor del propietario.

Los clientes anteriores reciben un mensaje para actualizar. No mezclar el mod 2.0 y el 2.1 en `mods`.

## Registro y permisos

Cuentas nuevas: `/register CLAVE CLAVE`, usando dos veces la misma contraseña de 12 a 128 caracteres. En las siguientes conexiones: `/login CLAVE`. No hacen falta códigos. Las cuentas existentes conservan contraseña, UUID, inventario y permisos.

Las identidades anteriores reservadas que todavía no se registraron deben hacerlo una vez desde la conexión local exacta de este equipo. Las conexiones públicas de Playit no tienen ese privilegio. Después pueden iniciar sesión desde cualquier conexión. Doumoment conserva operador de nivel 4; no se han restaurado ni descontado sus vidas para estas pruebas.

## Cómo resucitar

El santuario inicial está en **X −22, Y 88, Z 38**, a unos 44 bloques horizontales del spawn, en terreno natural comprobado. También aparecen santuarios raros en terreno seco del Overworld, exclusivamente al generar chunks nuevos. La distribución utiliza regiones de 96 chunks y separación mínima de 32; el terreno inadecuado puede dejar una región sin santuario.

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

La autoridad es `world/tecnihardcore-souls.json`, esquema 2. La migración conserva vidas y enfriamientos y convierte `revived=true` en una resurrección histórica. El registro de pagos permite recuperar un cobro interrumpido descontando solo una unidad de una pila. No editar este archivo mientras el servidor está encendido ni borrar transacciones pendientes.

Los ensayos destructivos se hicieron en mundos aislados y cuentas TecniSoul de prueba. La copia en `tools/test-runtime/server` debe quedar apagada durante el uso normal; su puerto Minecraft es 25566 y el de voz 24455. Los resultados y límites de validación están en `RESULTADOS-2.1.md`.
