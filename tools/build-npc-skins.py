"""Build original, UV-aligned 64x64 TecniHardcore skins (no external downloads)."""
from pathlib import Path
from PIL import Image, ImageDraw
import hashlib, json

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'assets/npc-skins'
OUT.mkdir(parents=True, exist_ok=True)

def skin(name, cloth, dark, hair, flesh, accent, slim=False):
    im = Image.new('RGBA', (64, 64))
    d = ImageDraw.Draw(im)
    def rect(x, y, w, h, color):
        d.rectangle((x, y, x+w-1, y+h-1), fill=color)
    def part(x, y, w, depth, h, color):
        rect(x+depth, y, w*2, depth, color)
        rect(x, y+depth, (w+depth)*2, h, color)
    part(0, 0, 8, 8, 8, flesh)
    part(16, 16, 8, 4, 12, cloth)
    for x,y in [(0,16),(16,48)]:
        part(x,y,4,4,12,dark)
        rect(x,y+12,16,4,'#262932')
        rect(x+4,y+5,1,6,accent)
    width=3 if slim else 4
    for x,y in [(40,16),(32,48)]:
        part(x,y,width,4,12,cloth)
        rect(x,y+12,(width+4)*2,4,flesh)
        rect(x+4,y+5,width,1,accent)
    # Head: front at 8,8; eyes, brows, nose, hair and sideburns.
    rect(8,0,16,8,hair)
    rect(0,8,32,2,hair)
    rect(24,8,8,8,hair)
    rect(0,8,3,8,hair)
    rect(19,8,5,8,hair)
    rect(8,10,1,3,hair); rect(15,10,1,3,hair)
    rect(9,11,2,1,'#f5ecd9');rect(13,11,2,1,'#f5ecd9')
    rect(10,11,1,1,accent);rect(13,11,1,1,accent)
    rect(11,13,2,1,'#ae735b');rect(10,15,4,1,hair)
    # Tailored front coat, lapels, belt and crystal insignia.
    rect(20,20,8,12,dark)
    rect(20,20,2,9,cloth);rect(26,20,2,9,cloth)
    rect(22,20,4,2,flesh)
    for y in range(23,29,2):rect(23,y,1,1,accent)
    rect(16,29,24,2,'#4c3327');rect(23,29,2,2,accent)
    rect(20,24,2,2,accent)
    rect(32,22,6,5,dark);rect(34,23,2,2,accent)
    # Second layer: thin golden shoulder trim, transparent elsewhere.
    rect(20,37,8,1,accent)
    if name=='ines':
        rect(8,9,8,1,'#b6a375');rect(8,14,2,2,hair)
        rect(26,22,2,5,'#8a6342')
    elif name=='bruno':
        rect(8,10,8,1,'#626d75');rect(9,10,2,2,'#d4b35b')
        rect(13,10,2,2,'#d4b35b');rect(21,24,6,5,'#785238')
        rect(23,24,2,2,'#cad8da')
    else:
        rect(8,9,8,1,accent);rect(11,8,2,2,'#39dfcc')
        rect(9,14,1,1,accent);rect(14,14,1,1,accent)
        rect(22,22,4,1,accent)
    im.save(OUT / (name+'.png'))
    return im

skins = [skin('ines','#3f786e','#223b38','#543929','#dfb28e','#71dbbc',True),
         skin('bruno','#465366','#28323d','#332d29','#c89a79','#d4b35b'),
         skin('selma','#624575','#332b49','#241c30','#b98164','#efc676')]
preview=Image.new('RGB',(384,208),'#15262b')
for i,im in enumerate(skins):
    front=Image.new('RGBA',(16,32))
    front.paste(im.crop((8,8,16,16)),(4,0))
    front.paste(im.crop((20,20,28,32)),(4,8))
    front.paste(im.crop((44,20,48 if i else 47,32)),(0,8))
    front.paste(im.crop((36,52,40 if i else 39,64)),(12,8))
    front.paste(im.crop((4,20,8,32)),(4,20))
    front.paste(im.crop((20,52,24,64)),(8,20))
    front=front.resize((80,160),Image.Resampling.NEAREST)
    preview.paste(front,(i*128+24,24),front)
ImageDraw.Draw(preview).text((35,190),'INES',fill='white')
ImageDraw.Draw(preview).text((160,190),'BRUNO',fill='white')
ImageDraw.Draw(preview).text((285,190),'SELMA',fill='white')
preview.save(OUT/'preview.png')
manifest={p.stem:hashlib.sha256(p.read_bytes()).hexdigest() for p in OUT.glob('*.png') if p.stem!='preview'}
(OUT/'manifest.json').write_text(json.dumps(manifest,indent=2),encoding='utf8')
print(json.dumps(manifest))
