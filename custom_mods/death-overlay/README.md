# TecniHardcore death overlay (Minecraft 1.20.1 Fabric)

The client plays the supplied transparent WebM as 240 RGBA frames at 24 fps,
with its original audio converted to Vorbis. Playback lasts 10 seconds. A
GameRenderer overlay continues across immediate respawn and spectator mode.
The local-player death packet triggers it regardless of the lives scoreboard.
`/tecni-video` previews the overlay locally without killing the player.

The previous FancyMenu death layout is disabled by `tools/build-menu.py` to
avoid duplicate playback. The mod is client-only; no server restart is needed.
Restart Minecraft after updating the mod. The production launcher copies it
from `installer_payload/mods` along with the rest of the client package.

Build from the workspace root:

```powershell
python tools/build-death-video.py
python tools/build-death-mod.py
python tools/build-menu.py
powershell -NoProfile -ExecutionPolicy Bypass -File tools/package-launcher.ps1
```

The Java sources use Fabric intermediary names from Yarn 1.20.1+build.10 and
compile directly against the installed remapped client JAR and libraries.
The mixins use `remap=false` because production Fabric uses those names.
The `libvpx-vp9` decoder is required to preserve the WebM alpha channel.
Only one decoded frame is retained on the GPU; compressed PNGs are preloaded
asynchronously and the previous native image is closed on every frame change.
