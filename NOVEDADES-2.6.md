# TecniHardcore 2.6.0 — tormentas, señales y presentación

Mantiene Minecraft 1.20.1 Fabric, las cuentas, UUID offline, vidas, inventarios y el video de muerte.

## Tormentas y comandos

Todas avisan diez segundos con una sirena. Viento y terremoto tienen audio original; lluvia y truenos incorporan una grabación real de Caesar en dominio público, con créditos en `AUDIO-CREDITS.json`. Respetan el volumen de Clima. Tornado y tormenta eléctrica oscurecen su zona y tienen truenos frecuentes. El tornado mueve bloques exclusivamente visuales cuando la destrucción es cero. Terremoto: polvo, grietas visuales, ondas y mayor sacudida; `/tecni-sacudida` permite desactivarla.

Radio predeterminado: **192** bloques; configurable de **32 a 512**. Duración: 30–600 segundos, incluida la advertencia. Tornado: 200 bloques de alto, **ancho 40–600**; predeterminado 120. Su zona se amplía si hace falta para contener el embudo y la trayectoria. Necesita espacio vertical y chunks cargados. La invocación no carga terreno nuevo.

Ejemplos, fuera de la plaza protegida:

```mcfunction
/tecni desastre iniciar tornado ~ ~ ~
/tecni desastre iniciar tornado ~ ~ ~ 256 180 ancho 300 destruccion 0
/tecni desastre iniciar terremoto ~ ~ ~ 192 180 destruccion 0
/tecni desastre iniciar electrica ~ ~ ~ 256 180
/tecni desastre iniciar acida ~ ~ ~ 256 180
/tecni desastre iniciar meteoritos ~ ~ ~ 256 180
/tecni desastre listar
/tecni desastre detener todos
```

Requiere operador nivel 4. Dos desastres como máximo, sin superposición; el spawn está protegido en toda su columna. Detener y reiniciar retiran fuerzas, audio y efectos; no se reanudan solos.

### Destrucción explícita

**Predeterminado 0. Los niveles superiores sí cambian el mundo y sus daños no se revierten al detener.** Hacer un respaldo antes de probarlos en construcciones que se quieran conservar. Se aplican únicamente a tornado y terremoto, con cantidades y profundidad diferentes. No eliminan contenedores, líquidos, portales, núcleos del santuario ni bloques irrompibles; tampoco afectan la plaza protegida. No dan objetos al romper.

| Nivel | Tornado | Terremoto |
|---|---|---|
| 0 | Sólo efectos y escombros visuales | Sólo efectos |
| 1 leve | Materiales blandos, hasta 12 bloques/s, superficie | Materiales blandos, hasta 6 bloques/s durante el pulso |
| 2 moderado | Materiales blandos, hasta 40 bloques/s, dos capas | Materiales blandos, hasta 24 bloques/s, dos capas |
| 3 severo | Incluye piedra y estructuras, hasta 90 bloques/s, tres capas | Incluye estructuras, hasta 60 bloques/s, cuatro capas |
| 4 realista | Hasta 160 bloques/s, erosión continua de tres capas en el núcleo | Hasta 120 bloques/s durante el pulso, fracturas hasta siete capas |

Son máximos, no cantidades garantizadas: sólo se trabaja sobre bloques permitidos y chunks cargados. El trabajo tiene límite por tick para que un radio grande no congele el servidor.

```mcfunction
/tecni desastre iniciar tornado ~ ~ ~ 256 180 ancho 300 destruccion 4
/tecni desastre iniciar terremoto ~ ~ ~ 192 180 destruccion 3
```

## Eventos sencillos

Un evento simultáneo permite omitir el ID. Siguen admitiéndose el número corto del listado y el UUID antiguo. Un número de un evento anterior no inicia ni detiene el siguiente.

```mcfunction
/tecni evento preparar tormenta ensayo
/tecni evento entrar
/tecni evento iniciar
/tecni evento detener
/tecni evento salir
/tecni evento listar
```

`entrar` y `salir` son para participantes; preparar, iniciar y detener requieren operador. Elegir `hardcore` al preparar implica vidas e inventarios reales. Ensayo restaura el estado original y no concede botín.

## Controles, skins e iconos

Espacio funciona para saltar a pie y ascender en montura, sin cambiar las teclas. Z desciende, R impulsa y Shift desmonta. Nuevos iconos originales para Corazón Sagrado, baliza, trofeo y las cinco monturas. El HUD de vidas y el video de muerte se conservan.

Fabric Tailor 2.1.2 obtiene skins por nombre y conserva los UUID de autenticación. Un nombre sin skin publicada no puede recuperar una imagen que nunca se haya subido: tras autenticarse se puede elegir una skin existente con `/skin set player NombrePremium`. También incorpora una pantalla de skin en opciones. Cambiar de skin no cambia el nombre, las vidas, los permisos ni el inventario. Consultar los resultados para las rutas verificadas.

## Baliza de auxilio

Haz cyan y dirección hasta **100.000 bloques**, durante 60 segundos, en la misma dimensión. Se transmite la posición una vez por segundo y se dibuja una señal en la dirección de destino sin cargar sus chunks. A larga distancia es una referencia visual proyectada: no ilumina físicamente todos los bloques del trayecto. Máximo 16 señales visibles; conserva 16 usos y diez minutos de enfriamiento. No cura ni teletransporta.

## Rendimiento y distribución

Mallas de nubes reutilizadas, menor geometría del embudo distante, escombros limitados por partículas y distancia, avisos de peligro conservados y trabajo de destrucción acotado. No se cambian dificultad, vidas, botín, RAM ni los perfiles gráficos seleccionados. Ver `RESULTADOS-2.6.md` para mediciones y límites de lo comprobado.

Cliente, launcher y servidor 2.6.0, handshake `pack_v7`, tormentas `cataclysm_v2`; los clientes antiguos deben actualizar antes de entrar. Mantiene conexión directa, identidad portable, acceso directo y el actualizador seguro. Las skins se descargan a través de sus servicios; la consulta de terceros puede fallar temporalmente sin impedir entrar al servidor.
