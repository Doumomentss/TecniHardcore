# TecniHardcore 2.7.1: cinco vidas

El servidor comienza con **cinco vidas** por jugador. El HUD muestra cinco cristales y el launcher indica el saldo sobre cinco. Los comandos administrativos admiten cantidades de 0 a 5. El libro y los NPCs explican el nuevo límite.

La migración ocurre una sola vez: 3 → 5, 2 → 4, 1 → 3; los eliminados con 0 permanecen en 0. Conserva muertes previas, UUID, inventarios, resurrecciones, enfriamientos, equipos y moneda. Cada ritual continúa devolviendo **una vida**, no cinco. El registro incluye `maxLives: 5` para evitar añadir vidas otra vez tras reiniciar.

Cerrar Minecraft y pulsar **ACTUALIZAR**. El servidor requiere el cliente 2.7.1 para garantizar que todos vean los cinco corazones. La conexión directa, skins personalizadas y ajustes gráficos se mantienen.
