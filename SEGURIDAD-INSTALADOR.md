# Instalador de TecniHardcore 2.5.0

El setup y el launcher se distribuyen desde las releases de `Doumomentss/TecniHardcore` en GitHub. La entrega incluye SHA-256 y tamaño de cada archivo. El actualizador verifica ambos antes de ejecutar el instalador; las dependencias externas se descargan desde las fuentes oficiales, con hashes fijados.

El instalador tiene icono, producto, descripción y versión de TecniHardcore, y un manifiesto `asInvoker`: no solicita elevación ni modifica Defender. Instala en una carpeta elegida por el usuario, conserva preferencias y respaldos, y crea un acceso directo. No se ofusca ni empaqueta con un protector de ejecutables.

El 2 de octubre de 2026 Microsoft Defender local, con protección activa, analizó el setup final y no encontró amenazas. Es un resultado del análisis antivirus de este archivo en este equipo; **no certifica reputación de SmartScreen ni garantiza ausencia de avisos en otros equipos**.

El análisis del parche 2.4.1 del 3 de octubre también terminó sin amenazas. La ventana visible del actualizador y sus reintentos no modifican la protección de Windows.

El 3 de octubre de 2026 Defender volvió a analizar el instalador final 2.5.0 (292 MiB): análisis completado, sin amenazas y con antivirus y protección en tiempo real activos. Las firmas locales estaban actualizadas el 2 de octubre. Sigue siendo un resultado antivirus local, no una validación de reputación de SmartScreen.

Esta entrega **no está firmada con un certificado de editor**. No hay un certificado de firma de código instalado. Un nombre de producto o hash no sustituye una firma. No se usa un certificado autofirmado para aparentar confianza.

Para distribuir versiones con editor verificado, el propietario debe obtener una identidad de firma confiable y firmar los ejecutables con SHA-256 y sello de tiempo, antes de generar hashes y publicarlos. La reputación puede tardar en acumularse incluso con un certificado válido; EV tampoco garantiza eliminar avisos. No reconstruir ni modificar un archivo después de firmarlo.

Referencia: [Microsoft: reputación de SmartScreen para desarrolladores](https://learn.microsoft.com/en-us/windows/apps/package-and-deploy/smartscreen-reputation). Una detección de malware y una advertencia de aplicación desconocida son situaciones diferentes: una detección debe investigarse y, si se confirma un falso positivo, puede enviarse a [Microsoft Security Intelligence](https://www.microsoft.com/security/portal/submit.aspx/). Eliminar la marca de Internet, añadir exclusiones o desactivar la protección no forma parte del instalador.
