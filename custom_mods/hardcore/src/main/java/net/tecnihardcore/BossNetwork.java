package net.tecnihardcore;
import java.util.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.server.world.ServerWorld;
public final class BossNetwork {
 public static final Identifier FX=Hardcore.id("boss_fx_v1");
 public static void send(TrialBoss boss,UUID cast,int type,int age,int windup,int finish,int state,Vec3d origin,double aim,List<Vec3d> marks){
  for(var p:((ServerWorld)boss.getWorld()).getPlayers())if(p.squaredDistanceTo(boss)<96*96&&ServerPlayNetworking.canSend(p,FX)){
   var b=PacketByteBufs.create();b.writeInt(boss.getId());b.writeUuid(cast);b.writeVarInt(type);b.writeVarInt(age);b.writeVarInt(windup);b.writeVarInt(finish);b.writeByte(state);vec(b,origin);b.writeDouble(aim);b.writeVarInt(marks.size());for(var m:marks)vec(b,m);ServerPlayNetworking.send(p,FX,b);
  }
 }
 private static void vec(net.minecraft.network.PacketByteBuf b,Vec3d p){b.writeDouble(p.x);b.writeDouble(p.y);b.writeDouble(p.z);}
}
