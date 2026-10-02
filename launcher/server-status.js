const net=require('net');
function varint(n){const bytes=[];do{let b=n&127;n>>>=7;if(n)b|=128;bytes.push(b);}while(n);return Buffer.from(bytes);}
function readVarint(buffer,start=0){let v=0;for(let i=0;i<5;i++){if(start+i>=buffer.length)return null;let b=buffer[start+i];v|=(b&127)<<(i*7);if(!(b&128))return {value:v>>>0,bytes:i+1};}throw new Error('VarInt inválido');}
function string(s){let b=Buffer.from(s);return Buffer.concat([varint(b.length),b]);}
function frame(data){return Buffer.concat([varint(data.length),data]);}
function query(endpoint){return new Promise(resolve=>{
  const started=Date.now(),socket=new net.Socket();let data=Buffer.alloc(0),done=false;
  const finish=result=>{if(done)return;done=true;socket.destroy();resolve({...result,host:endpoint.host,port:endpoint.port,ping:Date.now()-started});};
  socket.setTimeout(3500);socket.on('timeout',()=>finish({online:false}));socket.on('error',()=>finish({online:false}));socket.on('end',()=>finish({online:false}));
  socket.connect(endpoint.port,endpoint.host,()=>{const port=Buffer.alloc(2);port.writeUInt16BE(endpoint.port);socket.write(frame(Buffer.concat([varint(0),varint(763),string(endpoint.host),port,varint(1)])));socket.write(frame(varint(0)));});
  socket.on('data',chunk=>{
    try{data=Buffer.concat([data,chunk]);if(data.length>131072)throw Error('Respuesta demasiado grande');const packet=readVarint(data);if(!packet||data.length<packet.bytes+packet.value)return;
      const id=readVarint(data,packet.bytes);if(!id||id.value!==0)throw Error('Estado inválido');const length=readVarint(data,packet.bytes+id.bytes);if(!length) return;
      if(length.value>32767||packet.bytes+id.bytes+length.bytes+length.value>data.length)throw Error('JSON inválido');
      const status=JSON.parse(data.subarray(packet.bytes+id.bytes+length.bytes,packet.bytes+id.bytes+length.bytes+length.value).toString());
      finish({online:true,status});
    }catch(e){finish({online:false,error:e.message});}
  });
});}
let cache=null,pending=null;
async function snapshot(endpoint){let key=endpoint.host+':'+endpoint.port;if(cache?.key===key&&Date.now()-cache.time<4000)return cache.value;if(pending?.key===key)return pending.promise;
  const promise=query(endpoint).then(value=>{cache={key,time:Date.now(),value};return value;}).finally(()=>{pending=null;});pending={key,promise};return promise;}
function livesFrom(status,username){const extra=status.online&&status.status?.tecnihardcore;const player=[1,2].includes(extra?.protocol)&&Date.now()-extra.updatedAt<15000&&extra.players?.find(p=>p.name.toLowerCase()===username.toLowerCase());
  if(!player||!Number.isInteger(player.lives)||player.lives<0||player.lives>3)return {lives:null,max:3,display:'SIN DATOS',description:status.online?'El servidor todavía no tiene datos de este jugador':'Servidor sin conexión'};
  const resurrections=Number.isInteger(player.resurrections)&&player.resurrections>=0?player.resurrections:(player.revived?1:0);
  return {lives:player.lives,max:3,resurrections,display:player.lives+' / 3 VIDAS',description:(player.lives===0?'Eliminado · Ritual disponible':player.lives===1?'Última vida':player.lives+' vidas disponibles')+' · '+resurrections+' resurrecciones'};
}
module.exports={query,snapshot,livesFrom,varint,readVarint};
