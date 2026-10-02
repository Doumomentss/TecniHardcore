package net.tecnihardcore;

import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.item.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.*;
import java.nio.file.Files;

/** Applies only to the official plaza, never to unrelated single-player saves. */
public final class SpawnProtection {
    private static Boolean installed;
    public static boolean active(World w) {
        if(!(w instanceof ServerWorld sw)||w.getRegistryKey()!=World.OVERWORLD)return false;
        if(installed==null)installed=Files.isDirectory(sw.getServer().getSavePath(WorldSavePath.DATAPACKS).resolve("tecni_spawn"));return installed;
    }
    public static boolean inside(World w,BlockPos p) {return active(w)&&contains(p.getX(),p.getY(),p.getZ());}
    public static boolean contains(int x,int y,int z) {return y>=90&&y<=128&&(long)x*x+(long)z*z<=32L*32;}
    private static boolean deny(net.minecraft.entity.player.PlayerEntity p,World w,BlockPos pos) {
        if(!inside(w,pos)||p.hasPermissionLevel(2))return false;
        if(p.age%10==0)p.sendMessage(Text.literal("Plaza protegida. Puedes usar el santuario, el tablón y las mesas."),true);
        return true;
    }
    public static void init() {
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(s->installed=null);
        PlayerBlockBreakEvents.BEFORE.register((w,p,pos,state,entity)->!deny(p,w,pos));
        UseBlockCallback.EVENT.register((p,w,hand,hit)->{
            var block=w.getBlockState(hit.getBlockPos()).getBlock();
            if(!p.isSneaking()&&(block==Sanctuaries.CORE||block==TotemBoard.BLOCK||block==TotemBoard.PANEL||block==net.minecraft.block.Blocks.CRAFTING_TABLE||block==net.minecraft.block.Blocks.STONECUTTER))return ActionResult.PASS;
            Item i=p.getStackInHand(hand).getItem();
            boolean alters=i instanceof BlockItem||i instanceof BucketItem||i instanceof FlintAndSteelItem||i instanceof FireChargeItem||i instanceof BoneMealItem;
            return alters&&(deny(p,w,hit.getBlockPos())||deny(p,w,hit.getBlockPos().offset(hit.getSide())))?ActionResult.FAIL:ActionResult.PASS;
        });
        UseItemCallback.EVENT.register((p,w,hand)->{
            ItemStack held=p.getStackInHand(hand);
            return held.getItem() instanceof BucketItem&&deny(p,w,p.getBlockPos())?TypedActionResult.fail(held):TypedActionResult.pass(held);
        });
        AttackEntityCallback.EVENT.register((p,w,hand,e,hit)->deny(p,w,e.getBlockPos())?ActionResult.FAIL:ActionResult.PASS);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((e,source,amount)->
            !(e instanceof ServerPlayerEntity&&inside(e.getWorld(),e.getBlockPos())&&!source.isIn(net.minecraft.registry.tag.DamageTypeTags.BYPASSES_INVULNERABILITY)));
        ServerTickEvents.END_WORLD_TICK.register(w->{
            if(w.getTime()%20!=0||!active(w))return;
            for(var e:w.getEntitiesByClass(HostileEntity.class,new net.minecraft.util.math.Box(-33,90,-33,33,129,33),m->inside(w,m.getBlockPos()))) {
                double a=Math.atan2(e.getZ(),e.getX());int x=(int)Math.round(Math.cos(a)*37),z=(int)Math.round(Math.sin(a)*37);
                int y=w.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,x,z);
                e.refreshPositionAndAngles(x+.5,y,z+.5,e.getYaw(),e.getPitch());
            }
        });
    }
}
