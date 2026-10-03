module.exports=client=>{
 const accepted=new Map(),write=client.write.bind(client);
 client.prependListener('login_plugin_request',p=>{if(/^tecnihardcore:pack_v[234567]$/.test(p.channel))accepted.set(p.messageId,Number(p.channel.slice(-1)));});
 client.write=(name,p,...rest)=>write(name,name==='login_plugin_response'&&accepted.has(p.messageId)?{...p,data:Buffer.from([accepted.get(p.messageId)])}:p,...rest);
};
