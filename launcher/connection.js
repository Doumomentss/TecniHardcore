const fs=require('fs'),path=require('path');
const {findPayload}=require('./client-install');
function getConnection(resourcesPath) {
  const payload=findPayload(resourcesPath);
  const config=JSON.parse(fs.readFileSync(path.join(payload,'config','tecnihardcore','connection.json'),'utf8'));
  const ownerLocal=path.resolve(resourcesPath,'../../connection.local.json');
  const local=fs.existsSync(ownerLocal)?ownerLocal:path.join(path.dirname(payload),'connection.local.json');
  const override=fs.existsSync(local)?JSON.parse(fs.readFileSync(local,'utf8')):{};
  const profile=override.profile || config.profile;
  const selected={...config.profiles[profile],...override.endpoint};
  if(!selected?.host)throw new Error('Falta configurar la dirección pública de Playit. Abre Ajustes y completa el servidor.');
  if(!/^[a-zA-Z0-9.:-]+$/.test(selected.host)||!Number.isInteger(selected.port)||selected.port<1||selected.port>65535)throw new Error('Dirección de servidor inválida.');
  return {...selected,profile,packVersion:config.packVersion};
}
module.exports={getConnection};
