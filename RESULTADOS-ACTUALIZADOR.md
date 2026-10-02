# Validación del launcher 2.1.1

Fecha: 2 de octubre de 2026. Mecánicas 2.1.0, Minecraft 1.20.1, ritual protocolo 2. El mundo y el servidor de producción no se modificaron para estas pruebas.

| Prueba | Resultado |
| --- | --- |
| Versiones | Compara correctamente versiones numéricas, incluida 2.1.10 frente a 2.1.9; no ofrece versiones iguales ni retrocesos. |
| Descarga | Descarga en streaming; valida URL del repositorio oficial, longitud y SHA-256; rechaza contenido alterado o excesivo, y elimina descargas parciales/canceladas. |
| Fallo de instalación | Fallo inducido después de reemplazar un archivo: recupera el original y retira archivos nuevos. |
| Interrupción | Recuperación desde un registro persistente de actualización interrumpida: restaura originales, retira los nuevos y conserva preferencias. |
| Paquete inválido | ZIP con hash incorrecto o ruta fuera de la instalación: rechazado antes de modificar archivos instalados. |
| Interfaz real | Ventana nativa de Electron: aviso y botón ACTUALIZAR para una versión posterior, mensaje útil sin conexión y acceso a Ajustes. Captura revisada visualmente. |
| Instalación aislada | Instalador ejecutado fuera de D:\\servers\\minecraft, en una carpeta de usuario de Windows. Extrae y verifica el launcher. |
| Mods oficiales | Primer preparado en carpeta vacía: 51 descargas desde Modrinth, hashes coincidentes y 53 mods finales. Nuestros dos mods vienen incluidos. |
| Reparación | Mod externo alterado en la copia de pruebas: descarga y reparación correcta. Preferencias `options.txt` conservadas. |
| Menú y conexión | Se conserva el custom button ENTRAR AL SERVIDOR y su conexión directa al perfil público. |

Las pruebas del ciclo de descarga pública, cierre, instalación y reapertura se incorporarán después de publicar la Release y verificar el enlace anónimo de GitHub. No se presenta esta prueba pendiente como completada.

Las pruebas de los santuarios, efectos y video se documentan por separado en `RESULTADOS-2.1.md`. Sigue pendiente la comprobación desde un equipo ajeno a esta red; publicar el instalador no equivale a validar esa conexión externa.
