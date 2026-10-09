const {test}=require('node:test'),assert=require('node:assert/strict');
const status=require('../server-status');
test('launcher accepts and displays all five lives and crystals',()=>{
 const result={online:true,status:{tecnihardcore:{protocol:2,updatedAt:Date.now(),maxLives:5,players:[{name:'Prueba',lives:5,resurrections:0,online:true}]}}};
 const lives=status.livesFrom(result,'Prueba');assert.equal(lives.max,5);assert.equal(lives.display,'5 / 5 VIDAS');assert.equal(status.dashboard(result).players[0].lives,5);
 result.status.tecnihardcore.players[0].lives=0;assert.equal(status.livesFrom(result,'Prueba').max,5);assert.match(status.livesFrom(result,'Prueba').description,/Eliminado/);
 result.status.tecnihardcore.players[0].lives=6;assert.equal(status.livesFrom(result,'Prueba').lives,null);
});
