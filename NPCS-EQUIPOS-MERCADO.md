# TecniHardcore 2.7 · NPCs, equipos y mercado

Minecraft 1.20.1 Fabric. No cambia el mundo, las vidas, los inventarios ni los UUID existentes. Los nuevos registros se guardan en `world/tecnihardcore-social.json`; las definiciones de NPCs, en `config/tecnihardcore/npcs.json` del servidor. Estos archivos y `world/tecni-npc-skins` deben incluirse en las copias de seguridad.

## Primeros pasos

En el spawn, clic derecho sobre **Inés**, **Bruno** o **Selma**. Inés explica vidas y equipos, y paga **15 Cristales Tecni** por aceptar su misión, alejarse 128 bloques horizontalmente del spawn en supervivencia y volver a cobrar. Bruno ofrece dos contratos: **16 troncos por 25 CT** y **4 hierros en bruto por 40 CT**. Cada contrato se cobra una sola vez por jugador y consume exactamente los materiales ofrecidos. Creativo, espectadores y participantes de eventos no pueden generar estas recompensas.

**Cristales Tecni (CT)** es una moneda propia virtual: el servidor mantiene el saldo, no un objeto que se pueda copiar. `/cristales` consulta el saldo. Después de las misiones iniciales, se gana vendiendo a otros jugadores. No hay una recompensa diaria gratuita.

## Equipos y chat

```mcfunction
/team crear MiEquipo
/team invitar Nombre
/team aceptar
/team info
/team expulsar Nombre
/team transferir Nombre
/team salir
```

Nombre de 3–20 letras, números o guion bajo; máximo 12 integrantes. Solo el dueño invita, expulsa o transfiere el liderazgo. Las invitaciones caducan en cinco minutos; el destinatario debe aceptarlas. El dueño debe transferir el liderazgo antes de abandonar un equipo que tenga otros integrantes.

`/team` sin argumentos o `/chat` cambia entre **chat privado del equipo** y **general**. La elección se mantiene para los siguientes mensajes de esa conexión; no hace falta escribir un comando antes de cada mensaje. `/chat team` y `/chat general` eligen explícitamente. Al reconectarse comienza en general y recibe la información correspondiente; los mensajes privados solo llegan a integrantes autenticados del equipo.

No hay daño atribuido entre compañeros, incluidos proyectiles y mascotas con dueño. Al expulsar a alguien, la protección es mutua durante **dos horas que solo descuentan mientras el expulsado está conectado y autenticado**. Si está desconectado, se pausa y recibe el aviso al volver. Se protege tanto frente a los antiguos integrantes como frente a nuevos integrantes del mismo equipo. Abandonar voluntariamente un equipo con otros miembros también establece la tregua para evitar un abandono usado como emboscada. Disolver o cambiar de equipo no borra la protección ya registrada.

La protección intercepta daño que Minecraft atribuye a un jugador o a su mascota; no identifica como ataque personal una trampa ambiental sin autor registrado, como lava colocada previamente. No reemplaza las reglas contra trampas o acoso.

## Mercado de jugadores

Cerca de Selma, clic derecho y **Ver mercado**, o `/mercado`.

1. Sostener el lote en la mano principal, escribir su precio y pulsar **Publicar lote en mano**. Alternativa: `/mercado vender 25`.
2. Se publica **todo el lote**, con su nombre, cantidad, propiedades, vendedor y precio. Pasa a custodia del servidor; no queda disponible en el inventario del vendedor.
3. **Comprar** transfiere CT al vendedor, incluso si está desconectado, y entrega el lote al **Buzón** del comprador.
4. **Retirar** requiere espacio para el lote completo. Si falta espacio, permanece en el buzón. **Cancelar** una venta propia también devuelve el lote al buzón.

Precios de 1–1.000.000 CT; hasta diez ofertas abiertas por persona y 300 en total. No hay comisiones en esta versión. No se permite comprar en creativo, eliminado o dentro de eventos, ni vender objetos administrativos, equipo marcado de eventos/recompensas o referencias de monturas vinculadas. Los objetos normales conservan sus datos y encantamientos. No se generan objetos en el suelo cuando el inventario está lleno.

Cada acción se valida en el servidor: distancia al mercader, autenticación, identidad de la oferta, saldo y sesión. Dos compradores simultáneos no pueden comprar el mismo lote. Un registro pendiente permite recuperar el inventario y el pago si se interrumpe una entrega; no editar el registro mientras el servidor está abierto.

## Administración de NPCs

Operador nivel 4, cerca del NPC para editar:

```mcfunction
/tecni npc listar
/tecni npc crear guia_nueva Nombre del personaje
/tecni npc editar guia_nueva
/tecni npc editar guia_nueva otro_nodo
/tecni npc mover guia_nueva
/tecni npc skin guia_nueva https://textures.minecraft.net/texture/HASH
/tecni npc eliminar guia_nueva
/tecni npc recargar
/tecni moneda dar Nombre 100
```

`crear` coloca al NPC en tu posición; `mover` traslada el seleccionado a tu posición. El editor permite cambiar nombre, texto, agregar opciones y borrar una opción por número. **Nodo** identifica el diálogo que estás editando; **Nodo de destino** determina qué diálogo abrirá una opción. Usa `inicio` para la primera pantalla. Crea el nodo de destino antes de probar su opción. Dejar acción vacía permite navegar; `close` cierra, `market` abre el mercado y `quest_start:sendero`, `quest_claim:sendero`, `quest_start:madera`, `quest_claim:madera`, `quest_start:hierro`, `quest_claim:hierro` activan las misiones iniciales. No se ejecutan comandos arbitrarios enviados por clientes.

Ejemplo de una rama, dentro de `dialogue` de un NPC:

```json
{
  "inicio": {
    "text": "Hola. ¿Qué necesitas?",
    "options": [{"label": "Explícame el mercado", "next": "mercado", "action": ""}]
  },
  "mercado": {
    "text": "Selma guarda tus ventas y compras en el spawn.",
    "options": [{"label": "Volver", "next": "inicio", "action": ""}]
  }
}
```

También se puede editar el JSON con el servidor detenido o usar `/tecni npc recargar` después de editarlo. Máximo 64 NPCs, 64 nodos por NPC y ocho opciones por nodo. Los NPCs no empujan jugadores, no reciben daño y solo aparecen cuando su chunk está cargado y hay espacio.

### Skins por URL

La URL debe devolver directamente un PNG **64×64**, no una página web. HTTPS, hasta 64 KB, sin redirecciones. Se admiten `textures.minecraft.net`, `raw.githubusercontent.com`, `cdn.discordapp.com` y `media.discordapp.net`. El servidor valida y vuelve a codificar la imagen, la guarda por hash y la distribuye a los clientes; no necesitan acceder a la URL. Para un host adicional, el operador puede añadirlo explícitamente a `config/tecnihardcore/npc-skin-hosts.txt` y reiniciar. Autoriza únicamente servidores de imágenes que controles o consideres confiables.

En `npcs.json`, `slim: true` selecciona brazos de tres píxeles; `false`, cuatro. Las skins incluidas Alex, Efe y Zuri sirven como alternativas sin descarga. Una skin fallida no elimina el NPC ni cambia su skin anterior.

## Anticheats y diagnóstico

Versiones fijadas para Fabric 1.20.1: **AntiXray 1.4.6** y **Fiw 2.1.0**, junto a **TecniGuard** dentro del mod propio. Fuentes: [AntiXray](https://modrinth.com/mod/anti-xray), [Fiw](https://modrinth.com/mod/fiw-anticheat).

AntiXray oculta minerales completamente cubiertos en los paquetes del cliente sin alterar el terreno; modo 1 en Overworld y Nether, incluidos minerales del paquete. Fiw rechaza clientes que reportan mods de trampas conocidos y requiere la comprobación del cliente oficial. Un cliente manipulado podría mentir sobre sus mods: este filtro complementa las comprobaciones del servidor, no garantiza detectar todo.

TecniGuard bloquea ataques/interacciones adicionales por encima de 80 acciones por segundo y registra el abuso. El límite se reinicia cada segundo; no afecta el combate normal ni castiga automáticamente movimientos. Los desplazamientos inusuales solo producen avisos para revisar su contexto y se excluyen monturas, elytras, efectos y desastres autorizados. No es un detector completo de aimbot, alcance modificado o todos los trucos posibles.

**GrimAC 2.3.73 se probó y se excluyó de producción**: PacketEvents no interpreta ciertos IDs de objetos e inventarios ampliados del paquete. El registro y las dependencias de desarrollo quedan disponibles para investigación, pero no se instala su JAR en el servidor ni se asegura compatibilidad. Las sanciones de movimiento requieren una solución calibrada para este paquete.

```mcfunction
/tecni seguridad estado
/tecni seguridad alertas
/tecni seguridad revisar Nombre
```

`alertas` activa/desactiva avisos TecniGuard para el operador conectado. `revisar` consulta acciones bloqueadas y desplazamientos a revisar de esa conexión. Los comandos vanilla de equipos de administrador quedan disponibles como `/vanillateam`, separados de los equipos sociales.

## Recuperación y actualización

Antes de desplegar: apagado limpio, respaldo del mundo/configuración/auth y prueba de restauración. Actualizar servidor y cliente juntos: paquete **2.7.0**, handshake `pack_v11`, canales `civic_open_v1`, `civic_action_v1`, `npc_skin_request_v1`, `npc_skin_data_v1`. Un cliente antiguo recibe la indicación de actualizar antes de cargar las nuevas entidades. Se conservan `soul_v3`, el ritual y **ENTRAR AL SERVIDOR**.

Si una escritura del registro social falla, el servidor se detiene para evitar continuar con pagos incoherentes. Conserva `tecnihardcore-social.json`, `.previous`, los datos de jugadores y los logs. El servidor recupera una operación pendiente al arrancar; no borrar el campo `pending` manualmente ni resetear los saldos. Restaurar una copia requiere restaurar **mundo y registro social de la misma copia**, no solamente uno de ellos.
