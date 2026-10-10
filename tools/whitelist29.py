"""Manage the offline-UUID whitelist used by EasyAuth. Run while stopped, then /whitelist reload."""
import hashlib, json, os, re, sys, uuid
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
FILE=ROOT/'server/whitelist.json'
INITIAL=['Doumoment','quasar_18','FRANC0CUL0SPINT0','L1MON9373','Jobloxcee',
         'Adolf_Hitler','elveeer','wend121','PicanteSD','LukitasLA2244',
         'Pessuti_','TOT1','rastreo','pdiddy67cityboy','GaspidoxMC',
         'rosamelano','matheusqq','Kuclesss','sancsoff','caminoterreno3',
         'maehl','MRPOPONADA','BENJAMINPEROREAL']

def identity(name):
    if not re.fullmatch(r'[A-Za-z0-9_]{3,16}',name):raise SystemExit('Nombre inválido: usa 3–16 letras, números o _.')
    return str(uuid.UUID(bytes=hashlib.md5(('OfflinePlayer:'+name).encode()).digest(),version=3))

def save(entries):
    if len({p['name'].lower() for p in entries})!=len(entries):raise SystemExit('Nombres duplicados.')
    temp=FILE.with_suffix('.json.tmp')
    with temp.open('w',encoding='utf8') as output:
        json.dump(entries,output,ensure_ascii=False,indent=2);output.write('\n');output.flush();os.fsync(output.fileno())
    os.replace(temp,FILE)

def main(args):
    if len(args)==1 and args[0]=='preguntar':
        name=input('Nombre exacto de Minecraft para la whitelist: ').strip()
        return main(['agregar',name])
    if len(args)==1 and args[0]=='inicial':
        entries=[{'uuid':identity(name),'name':name} for name in INITIAL]
        save(entries);print(f'Whitelist inicial: {len(entries)} nombres.');return
    if not args or args[0] not in ('agregar','quitar','listar'):raise SystemExit('Uso: python tools/whitelist29.py preguntar|listar|agregar Nombre|quitar Nombre')
    entries=json.loads(FILE.read_text(encoding='utf8')) if FILE.exists() else []
    if args[0]=='listar':
        for person in entries:print(person['name'])
        return
    if len(args)!=2:raise SystemExit('Falta el nombre del jugador.')
    name=args[1];candidate=identity(name)
    if args[0]=='agregar':
        if not any(p['name'].lower()==name.lower() for p in entries):entries.append({'uuid':candidate,'name':name})
    else:entries=[p for p in entries if p['name'].lower()!=name.lower()]
    save(entries);print(f'{name}: {args[0]}. El servidor 2.9 recargará la lista automáticamente.')

if __name__=='__main__':main(sys.argv[1:])
