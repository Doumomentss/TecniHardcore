package net.tecnihardcore;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.PhantomEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import java.util.*;

/** Seasonal difficulty mutations kept separate from the life/ritual authority. */
public final class DifficultyMutations {
    private static final Map<UUID,Integer> food = new HashMap<>();
    private static int ticks;
    private static volatile boolean enabled;
    private DifficultyMutations() {}

    public static boolean enabled() { return enabled; }
    public static void setEnabled(boolean value) { enabled = value; }

    public static void init() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (!enabled) return;
            if (!(entity instanceof HostileEntity mob)) return;
            if (mob instanceof ZombieEntity zombie && zombie.getMainHandStack().isEmpty() && world.random.nextFloat() < .72f) {
                ItemStack weapon = switch (world.random.nextInt(5)) {
                    case 0 -> new ItemStack(Items.IRON_SWORD);
                    case 1 -> new ItemStack(Items.IRON_AXE);
                    case 2 -> new ItemStack(Items.STONE_SWORD);
                    case 3 -> new ItemStack(Items.BOW);
                    default -> new ItemStack(Items.IRON_SHOVEL);
                };
                zombie.equipStack(net.minecraft.entity.EquipmentSlot.MAINHAND, weapon);
                zombie.setEquipmentDropChance(net.minecraft.entity.EquipmentSlot.MAINHAND, 0.03f);
            }
        });
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!enabled) return true;
            if (!(entity instanceof ServerPlayerEntity player) || !AuthBootstrap.authenticated(player) || player.isCreative() || player.isSpectator()) return true;
            if (!(source.getAttacker() instanceof LivingEntity)) return true;
            if (player.getRandom().nextFloat() < .25f) unequipRandomArmor(player);
            return true;
        });
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (!enabled) return ActionResult.PASS;
            if (world.isClient || !(player instanceof ServerPlayerEntity p) || !AuthBootstrap.authenticated(p) || p.isCreative() || p.isSpectator()) return ActionResult.PASS;
            BlockEntity be = world.getBlockEntity(hit.getBlockPos());
            if (be instanceof ChestBlockEntity || be instanceof EnderChestBlockEntity || be instanceof ShulkerBoxBlockEntity) {
                p.damage(p.getDamageSources().generic(), 1.0f);
                p.sendMessage(net.minecraft.text.Text.literal("§cAbrir este cofre te costó medio corazón."), true);
            }
            return ActionResult.PASS;
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!enabled) return;
            if (++ticks % 2 != 0) return;
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                if (!AuthBootstrap.authenticated(p)) continue;
                int now = p.getHungerManager().getFoodLevel();
                Integer before = food.put(p.getUuid(), now);
                if (before != null && now > before && !isPremiumFood(p)) p.getHungerManager().setFoodLevel(before + Math.max(1, (now - before) / 3));
            }
            if (ticks % 20 != 0) return;
            for (ServerWorld world : server.getWorlds()) {
                for (PhantomEntity phantom : world.getEntitiesByType(EntityType.PHANTOM, new Box(world.getSpawnPos()).expand(30000), e -> true)) phantom.setFireTicks(0);
                for (CreeperEntity creeper : world.getEntitiesByType(EntityType.CREEPER, new Box(world.getSpawnPos()).expand(30000), e -> !e.getCommandTags().contains("tecni:charged_mutation"))) {
                    if (world.random.nextFloat() < .0025f) {
                        LightningEntity bolt=EntityType.LIGHTNING_BOLT.create(world);
                        if (bolt!=null) { bolt.refreshPositionAndAngles(creeper.getX(),creeper.getY(),creeper.getZ(),0,0); creeper.onStruckByLightning(world,bolt); bolt.discard(); }
                        creeper.addCommandTag("tecni:charged_mutation");
                        world.playSound(null, creeper.getBlockPos(), SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.HOSTILE, 1.3f, 1f);
                    }
                }
                for (HostileEntity mob : world.getEntitiesByClass(HostileEntity.class, new Box(world.getSpawnPos()).expand(30000), e -> true)) if (mob.age > 0) mob.setFireTicks(0);
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> { if (server.getTicks() % 20 == 0) food.keySet().removeIf(id -> server.getPlayerManager().getPlayer(id) == null); });
    }
    private static boolean isPremiumFood(ServerPlayerEntity p) { ItemStack main=p.getMainHandStack(), off=p.getOffHandStack(); return main.isOf(Items.GOLDEN_CARROT)||main.isOf(Items.GOLDEN_APPLE)||off.isOf(Items.GOLDEN_CARROT)||off.isOf(Items.GOLDEN_APPLE); }
    private static void unequipRandomArmor(ServerPlayerEntity p) {
        var slots = new net.minecraft.entity.EquipmentSlot[]{net.minecraft.entity.EquipmentSlot.HEAD,net.minecraft.entity.EquipmentSlot.CHEST,net.minecraft.entity.EquipmentSlot.LEGS,net.minecraft.entity.EquipmentSlot.FEET};
        var slot=slots[p.getRandom().nextInt(slots.length)]; ItemStack stack=p.getEquippedStack(slot); if(stack.isEmpty()) return;
        p.equipStack(slot, ItemStack.EMPTY); if(!p.getInventory().insertStack(stack)) p.dropItem(stack,false);
        p.sendMessage(net.minecraft.text.Text.literal("§eUn golpe te desequipó una pieza de armadura."), true);
    }
}
