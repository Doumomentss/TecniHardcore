package net.tecnihardcore;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.*;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.*;
import static net.minecraft.server.command.CommandManager.*;
public final class TrialArena {
    public static boolean enabled(){return Boolean.getBoolean("tecni.testServer")&&Boolean.getBoolean("tecni.bossPreview");}
    public static void init(){
        ServerLifecycleEvents.SERVER_STARTED.register(s->{if(!enabled())return;var w=s.getOverworld();
            for(int x=-28;x<=28;x++)for(int z=-28;z<=28;z++){
                int r=Math.max(Math.abs(x),Math.abs(z));w.setBlockState(new BlockPos(x,94,z),Blocks.BEDROCK.getDefaultState());w.setBlockState(new BlockPos(x,95,z),(r==28?Blocks.POLISHED_BLACKSTONE_BRICKS:(x%7==0||z%7==0)?Blocks.CUT_COPPER:Blocks.DEEPSLATE_TILES).getDefaultState());
                if(r==28){w.setBlockState(new BlockPos(x,96,z),Blocks.POLISHED_BLACKSTONE_WALL.getDefaultState());for(int y=97;y<=104;y++)w.setBlockState(new BlockPos(x,y,z),Blocks.BARRIER.getDefaultState());}
            }
            w.setSpawnPos(new BlockPos(0,96,-18),0);s.getGameRules().get(GameRules.DO_MOB_SPAWNING).set(false,s);s.getGameRules().get(GameRules.DO_DAYLIGHT_CYCLE).set(false,s);s.getGameRules().get(GameRules.KEEP_INVENTORY).set(true,s);w.setTimeOfDay(1000);
            // Entity sections load asynchronously. Wait before deciding whether the arena needs a boss.
            pending=80;
        });
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(s->{if(!enabled()||pending<=0||--pending!=0)return;var w=s.getOverworld();var existing=w.getEntitiesByClass(TrialBoss.class,new net.minecraft.util.math.Box(-50,90,-50,50,120,50),b->true);for(int i=1;i<existing.size();i++)existing.get(i).discard();if(!existing.isEmpty()){existing.get(0).setHealth(existing.get(0).getMaxHealth());existing.get(0).refreshPositionAndAngles(.5,96,.5,180,0);}if(existing.isEmpty()){var boss=new TrialBoss(TrialBoss.TYPE,w);boss.refreshPositionAndAngles(.5,96,.5,180,0);w.spawnEntity(boss);}});
        ServerPlayConnectionEvents.JOIN.register((h,sender,s)->{if(!enabled())return;var p=h.player;p.teleport(s.getOverworld(),.5,96,-18.5,0,0);p.changeGameMode(GameMode.SURVIVAL);Hardcore.soul(p).lives=Rules.MAX_LIVES;Hardcore.souls.save();Hardcore.send(p);p.setHealth(20);p.getHungerManager().setFoodLevel(20);
            p.equipStack(EquipmentSlot.HEAD,new ItemStack(Items.DIAMOND_HELMET));p.equipStack(EquipmentSlot.CHEST,new ItemStack(Items.DIAMOND_CHESTPLATE));p.equipStack(EquipmentSlot.LEGS,new ItemStack(Items.DIAMOND_LEGGINGS));p.equipStack(EquipmentSlot.FEET,new ItemStack(Items.DIAMOND_BOOTS));p.getInventory().setStack(0,new ItemStack(Items.DIAMOND_SWORD));p.getInventory().setStack(1,new ItemStack(Items.BOW));p.getInventory().setStack(2,new ItemStack(Items.COOKED_BEEF,64));p.getInventory().setStack(3,new ItemStack(Items.ARROW,64));p.equipStack(EquipmentSlot.OFFHAND,new ItemStack(Items.SHIELD));p.sendMessage(Text.literal("ARENA AISLADA · No afecta al servidor. /tecni prueba reiniciar recupera vidas y reinicia el jefe."),false);
        });
        CommandRegistrationCallback.EVENT.register((d,a,e)->d.register(literal("tecni").then(literal("prueba").requires(s->enabled()).then(literal("reiniciar").executes(c->{var p=c.getSource().getPlayerOrThrow();var w=p.getServerWorld();for(var b:w.getEntitiesByClass(TrialBoss.class,new net.minecraft.util.math.Box(-60,80,-60,60,140,60),b->true))b.discard();for(var b:w.getEntitiesByClass(TrialShard.class,new net.minecraft.util.math.Box(-60,80,-60,60,140,60),b->true))b.discard();Hardcore.soul(p).lives=Rules.MAX_LIVES;Hardcore.souls.save();Hardcore.send(p);p.changeGameMode(GameMode.SURVIVAL);p.setHealth(20);p.getHungerManager().setFoodLevel(20);p.teleport(w,.5,96,-18.5,0,0);var boss=new TrialBoss(TrialBoss.TYPE,w);boss.refreshPositionAndAngles(.5,96,.5,180,0);w.spawnEntity(boss);return 1;})))));
    }
    private static int pending;
}
