"""Prepare the transparent death animation for FancyMenu.

FancyMenu's FMA animation format keeps the alpha channel and works without
installing a video backend.  The original WebM audio is exported separately
as OGG and played by an invisible FancyMenu audio element.
"""
from pathlib import Path
import json, shutil, subprocess, tempfile, zipfile
from PIL import Image
import imageio_ffmpeg

ROOT = Path(__file__).resolve().parents[1]
src = ROOT / 'assets' / 'video_sin_fondo_transparente.webm'
if not src.exists():
    raise SystemExit(f'Missing source video: {src}')

ffmpeg = imageio_ffmpeg.get_ffmpeg_exe()
fps = 24
with tempfile.TemporaryDirectory(prefix='tecni-death-') as td:
    frames = Path(td) / 'frames'
    frames.mkdir()
    # FFmpeg's native VP9 decoder drops WebM alpha. libvpx preserves it.
    subprocess.run([ffmpeg, '-y', '-c:v', 'libvpx-vp9', '-i', str(src), '-vf', f'fps={fps},format=rgba',
                    str(frames / '%05d.png')], check=True,
                   stdout=subprocess.DEVNULL, stderr=subprocess.PIPE)
    pngs = sorted(frames.glob('*.png'))
    if not pngs:
        raise SystemExit('The video did not produce any frames')
    if Image.open(pngs[0]).getchannel('A').getextrema()[0] == 255:
        raise SystemExit('Transparency was lost during WebM decoding')

    # Keep each frame as PNG so transparent pixels remain transparent.
    fma = ROOT / 'assets' / 'video_sin_fondo_transparente.fma'
    metadata = {'loop_count': 0, 'frame_time': round(1000 / fps),
                'frame_time_intro': round(1000 / fps),
                'custom_frame_times': {}, 'custom_frame_times_intro': {}}
    with zipfile.ZipFile(fma, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=6) as z:
        z.writestr('metadata.json', json.dumps(metadata, separators=(',', ':')))
        for i, p in enumerate(pngs):
            z.write(p, f'frames/{i}.png')

    audio = ROOT / 'assets' / 'video_sin_fondo_transparente.ogg'
    subprocess.run([ffmpeg, '-y', '-i', str(src), '-vn', '-c:a', 'libvorbis', '-q:a', '5', str(audio)],
                   check=True, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE)

    for target in [ROOT / 'client/config/fancymenu/assets', ROOT / 'installer_payload/config/fancymenu/assets']:
        target.mkdir(parents=True, exist_ok=True)
        shutil.copy2(fma, target / fma.name)
        shutil.copy2(audio, target / audio.name)

print(f'Built {len(pngs)} RGBA frames at {fps} fps: {fma.name}')
print(f'Extracted audio: {audio.name}')
