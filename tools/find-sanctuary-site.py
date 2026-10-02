"""Read-only candidate finder. Runtime placement still revalidates every block."""
from pathlib import Path
import io,zlib,nbtlib,math,json,sys
root=Path(sys.argv[1] if len(sys.argv)>1 else 'server/world')
data=nbtlib.load(root/'level.dat')['Data'];sx,sz=int(data['SpawnX']),int(data['SpawnZ'])
chunks={};regions={}
def chunk(cx,cz):
    key=(cx,cz)
    if key in chunks:return chunks[key]
    rx,rz=cx//32,cz//32;path=root/'region'/f'r.{rx}.{rz}.mca'
    if (rx,rz) not in regions:regions[(rx,rz)]=path.read_bytes() if path.exists() else None
    buf=regions[(rx,rz)]
    if not buf:chunks[key]=None;return None
    at=((cx%32)+(cz%32)*32)*4;off=int.from_bytes(buf[at:at+3],'big')*4096
    if not off:chunks[key]=None;return None
    length=int.from_bytes(buf[off:off+4],'big');compression=buf[off+4];raw=buf[off+5:off+4+length]
    if compression!=2:chunks[key]=None;return None
    n=nbtlib.File.parse(io.BytesIO(zlib.decompress(raw)));sections={int(s['Y']):s for s in n.get('sections',[])};chunks[key]=(n,sections);return chunks[key]
def block(x,y,z):
    c=chunk(x//16,z//16)
    if not c:return 'unknown'
    s=c[1].get(y//16)
    if not s or 'block_states' not in s:return 'minecraft:air'
    states=s['block_states'];palette=states['palette'];index=(y%16)*256+(z%16)*16+x%16
    if len(palette)==1:return str(palette[0]['Name'])
    bits=max(4,(len(palette)-1).bit_length());per=64//bits;value=(int(states['data'][index//per])&((1<<64)-1))>>((index%per)*bits)&((1<<bits)-1)
    return str(palette[value]['Name'])
natural={'minecraft:'+s for s in ['grass_block','dirt','coarse_dirt','podzol','rooted_dirt','stone','sand','gravel','deepslate','snow_block','mycelium']}
empty={'minecraft:'+s for s in ['air','cave_air','grass','tall_grass','fern','snow','dandelion','poppy','blue_orchid','allium','azure_bluet','oxeye_daisy','cornflower','lily_of_the_valley','sunflower','lilac','rose_bush','peony']}
heights={}
def top(x,z):
    key=(x,z)
    if key in heights:return heights[key]
    c=chunk(x//16,z//16)
    if not c:return None
    h=c[0]['Heightmaps'].get('MOTION_BLOCKING_NO_LEAVES')
    if h is None:return None
    i=(z%16)*16+x%16;y=((int(h[i//7])&((1<<64)-1))>>((i%7)*9)&511)-64
    # The heightmap reports one above the highest solid or fluid surface.
    heights[key]=y;return y
coords=sorted(((x,z) for x in range(sx-256,sx+257,6) for z in range(sz-256,sz+257,6)),key=lambda p:(p[0]-sx)**2+(p[1]-sz)**2)
for x,z in coords:
    if (x-sx)**2+(z-sz)**2<32**2:continue
    samples=[top(x+dx,z+dz) for dx in range(-5,6) for dz in range(-5,6)]
    if None in samples or max(samples)-min(samples)>3:continue
    y=max(samples);good=True
    for dx in range(-5,6):
        for dz in range(-5,6):
            t=top(x+dx,z+dz)
            if block(x+dx,t-1,z+dz) not in natural:good=False;break
            for yy in range(t,y+8):
                if block(x+dx,yy,z+dz) not in empty:good=False;break
            if not good:break
        if not good:break
    if good:
        print(json.dumps({'x':x,'y':y,'z':z,'distance':round(math.hypot(x-sx,z-sz))}));break
else:raise SystemExit('No untouched candidate found within 256 blocks; no world changes made.')
