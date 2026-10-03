# TecniHardcore 2.6: guía de cataclismos, monturas y eventos

Minecraft 1.20.1 Fabric. Estas funciones requieren el mod 2.6.0 en servidor y cliente. Los desastres y eventos solo se inician por orden administrativa; no existe un temporizador de aparición aleatoria.

## Cataclismos

Los cinco tipos de comando son `tornado`, `terremoto`, `acida`, `electrica` y `meteoritos`. Primero visita la zona para cargarla. El lugar indicado es el suelo del fenómeno. Mantén sus límites fuera de los 32 bloques del spawn; la protección incluye toda su columna vertical.

Ejemplo en una zona cargada y suficientemente baja:

```mcfunction
/tecni desastre iniciar tornado 400 95 400 96 180
/tecni desastre iniciar acida 400 95 400 64 120
/tecni desastre listar
/tecni desastre detener ID
/tecni desastre detener todos
```

Los primeros dos ejemplos son alternativas: no pueden estar activos simultáneamente en la misma zona. Puedes omitir radio y segundos para usar 192 bloques y 180 segundos. Radio permitido: 32–512. Duración: 30–600 segundos, incluidos diez de advertencia. Máximo dos desastres activos sin superposición. El evento Ojo de la tormenta reserva un espacio de desastre mientras está preparado o activo.

El tornado necesita 200 bloques de altura disponibles bajo el techo del mundo; rechaza lugares demasiado altos. Su embudo alcanza 120 bloques de diámetro en la parte superior. Su trayectoria circular permanece en la zona y su fuerza afecta a quienes estén a menos de 48 bloques del embudo. El núcleo hace 2 puntos de daño por segundo. En destrucción 0 no arranca bloques: los fragmentos son visuales. Ancho configurable de 40–600 con `ancho N`; niveles destructivos 1–4 requieren `destruccion N`. Ver [la guía 2.6](NOVEDADES-2.6.md) para límites y ejemplos.

El terremoto anuncia cada pulso y golpea a quienes están en el suelo. Agacharse reduce el daño de 6 a 3. La lluvia ácida causa 2 puntos cada dos segundos a quienes estén expuestos; un techo protege. Las descargas eléctricas anuncian círculos de dos bloques y hacen 10 de daño. Los meteoritos anuncian círculos de cuatro bloques y hacen 14. Estos valores se aplican antes de armadura. Tornado y terremoto sólo destruyen bloques si se elige un nivel 1–4 explícitamente; los demás fenómenos no destruyen construcciones ni encienden fuego. Los jugadores creativos, espectadores y sin autenticar quedan excluidos.

Una montura también puede sufrir el daño del desastre. Morir consume vidas normalmente y activa el video existente. Los tótems mantienen sus reglas habituales. `/tecni-sacudida` permite desactivar o activar la sacudida de cámara; los avisos importantes siguen visibles con partículas mínimas. El volumen utiliza la categoría Clima del juego.

Parar un desastre produce una retirada visual de tres segundos. Un reinicio lo cancela; no lo vuelve a iniciar automáticamente ni mantiene fuerzas sobre los jugadores. Invocar un desastre no carga ni genera chunks lejanos.

## Bestias de cristal

| Nivel | Bestia | Tirada personal | Salud | Vuelo / impulso, bloques por segundo |
|---|---|---:|---:|---:|
| 1 | Mantarraya de cristal | 20 % | 30 | 8 / 12 |
| 2 | Ave de cuarzo | 15 % | 40 | 10 / 15 |
| 3 | Grifo de jade | 8 % | 55 | 12 / 18 |
| 4 | Wyvern de amatista | 4 % | 70 | 14 / 21 |
| 5 | Dragón del núcleo | 1 % | 90 | 16 / 24 |

Cada participante elegible recibe una tirada al derrotar definitivamente las tres fases del Custodio. Es una sola tirada con categorías: 48 % de obtener alguna montura, 52 % de no obtenerla. No hay garantía acumulada y Saqueo no altera las probabilidades.

Se requiere causar al menos el 5 % del daño efectivo de las tres barras, estar autenticado y seguir conectado en la misma dimensión, a menos de 96 bloques al derrotarlo. El daño creativo o administrativo invalida el botín de esa pelea. Los jefes de ensayo no dan premios. Pasar de fase tampoco concede premios.

Cada elegible obtiene además 4–8 diamantes, 1–3 bloques de oro y 2–4 bloques de hierro. Un mensaje indica el resultado. Reclama con:

```mcfunction
/tecni recompensas
```

Libera cinco espacios de inventario antes de reclamar. El buzón conserva recompensas pendientes y recupera entregas interrumpidas mediante identificadores persistentes; repetir el comando no repite una recompensa ya entregada.

La invocación no tiene receta. Haz clic derecho con el objeto en una zona despejada, después clic sobre la criatura para montarla. Controles iniciales: WASD para desplazarse, Espacio para ascender, Z para descender, R para impulso y Shift para desmontar. Se pueden cambiar desde Controles. El impulso dura hasta cuatro segundos y se recarga durante veinte. La montura permite usar tus armas; no ataca por sí sola. El Custodio alterna ataques a distancia cuando su objetivo vuela.

Los fragmentos de amatista curan cuatro puntos por unidad, fuera de combate y con cinco segundos entre curaciones. Para guardar la bestia, aterriza, desmonta, espera quince segundos sin daño y haz clic agachado sobre ella. Solo puede haber una montura personal activa. Guarda tu bestia antes de inscribirte en un evento.

Puedes regalar o vender el objeto mientras esté guardada. Conserva salud, identidad y tiempos de impulso. Invocar no cura. Si la bestia muere, se pierde definitivamente y el objeto queda inválido; el jinete puede sufrir daño de caída. Una desconexión inicia un aterrizaje controlado antes de guardarla. Si no puede aterrizar todavía, permanece activa hasta hacerlo. Copiar un objeto no copia la criatura.

Para pruebas administrativas, en una copia del mundo:

```mcfunction
/tecni montura dar CuentaDePrueba 1
/tecni montura dar CuentaDePrueba 5
```

La entrega queda registrada con operador, UUID de montura, nivel y destinatario. No se publican registros privados ni inventarios en el launcher.

## Organizar eventos

El panel `/tecni evento panel` permite seleccionar tipo y modo, preparar, iniciar y detener. Preparar crea una parcela nueva y vacía en `tecnihardcore:eventos`. La construcción se reparte entre ticks y la lista indica PREPARANDO ARENA o LISTO. Nunca sustituye una parcela ocupada. Solo hay un evento simultáneo.

```mcfunction
/tecni evento preparar tormenta ensayo
/tecni evento listar
/tecni evento entrar
/tecni evento iniciar
/tecni evento salir
/tecni evento detener
```

Preparar, iniciar, detener y abrir el panel requieren operador nivel 4. Entrar exige autenticación, vidas disponibles, inventario sin una operación de cursor pendiente y ninguna montura personal activa. La inscripción es voluntaria y se cierra al iniciar. Tras iniciar hay diez segundos de cuenta atrás.

**ENSAYO:** equipo temporal, sin premios, sin tumbas y sin pérdida de vidas. La derrota convierte al participante en observador hasta terminar o salir. Se guarda previamente el estado del jugador, incluidos componentes de inventario de otros mods. Al salir se recupera ese estado. No se permite sacar objetos ni usar contenedores del equipo original para exportar equipo temporal. Una desconexión o reinicio conserva el respaldo de recuperación.

**HARDCORE:** vidas y equipo reales. Una muerte tiene las mismas consecuencias que en el mundo principal. Salir o cancelar no devuelve objetos perdidos ni vidas consumidas. El juego lo anuncia antes de inscribirse.

### Ojo de la tormenta

```mcfunction
/tecni evento preparar tormenta hardcore
```

Dura diez minutos, con tres etapas de 200 segundos: lluvia ácida, terremoto y tornado. Hay cuatro refugios con techo de cobre alrededor del núcleo. La interfaz indica etapa, tiempo y ubicación de refugios. Ganan los participantes que sobrevivan hasta el final. Premio hardcore: cuatro diamantes y un trofeo cosmético por ganador.

### Circuito de cristal

```mcfunction
/tecni evento preparar circuito ensayo
```

Tres vueltas por doce anillos ordenados. Todos reciben una montura temporal de nivel 3; no se obtiene su objeto ni se puede conservar al salir. El HUD muestra vuelta y siguiente anillo. Tiempo máximo: diez minutos. Saltarse un anillo no avanza la vuelta. En hardcore, los tres primeros reciben seis, tres y un diamante; el primero también recibe trofeo. Un ensayo solo registra el resultado deportivo.

### Defensa del núcleo

```mcfunction
/tecni evento preparar defensa hardcore
```

Cinco oleadas de zombis, esqueletos y sus variantes de equipo. Cada oleada contiene `6 + 4 × inscritos`, con máximo 24; no puede haber más de treinta enemigos activos. El núcleo empieza con 200 de salud. Los enemigos que lleguen junto a él lo dañan; los jugadores deben interceptarlos. Victoria al completar las cinco oleadas conservando el núcleo. Premio hardcore: cuatro diamantes y trofeo por superviviente.

### Cancelación y recuperación

Detener cancela sin premios, limpia criaturas temporales y devuelve participantes. Los respaldos individuales están en `world/tecni-event-recovery/UUID.nbt`. **No borrarlos para resolver un problema:** contienen el inventario anterior de ensayos. La recuperación se procesa al reconectar, antes de dejar actuar al jugador. Si un respaldo no se puede leer, se rechaza la conexión conservando el archivo para intervención administrativa.

El registro independiente `world/tecnihardcore-expansion.json` contiene monturas y buzón; no sustituye `tecnihardcore-souls.json`. Respaldar ambos junto al mundo. No editar ninguno con el servidor encendido.

## Nuevos paisajes y decoración

Nature’s Spirit 2.2.5 y Regions Unexplored 0.5.6 amplían biomas en terreno nuevo. No se regeneran regiones existentes. Handcrafted 3.0.6 aporta muebles y Chipped 3.0.7 variaciones decorativas de bloques. Sus dependencias están fijadas en el catálogo oficial del launcher. Usa JEI para las recetas. Todos los perfiles gráficos mantienen estos mods y las mismas mecánicas.

## Distribución

El launcher 2.5.0 conserva el actualizador corregido, nombre portable, carpeta seleccionada, acceso directo y botón ENTRAR AL SERVIDOR. El paquete requiere handshake `pack_v6`; los clientes anteriores deben actualizar. Cambiar los mods del servidor exige un apagado limpio, respaldo verificado y reinicio. No quitar los mods de biomas o decoración de un mundo que ya contiene sus bloques.
