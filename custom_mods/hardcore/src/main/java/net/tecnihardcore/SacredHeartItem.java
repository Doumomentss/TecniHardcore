package net.tecnihardcore;
import net.minecraft.item.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.*;
import net.minecraft.world.World;
/** The floating crystal extends beyond its block; using the offering nearby also opens its altar. */
public final class SacredHeartItem extends Item {
    public SacredHeartItem(){super(new Item.Settings().maxCount(16).fireproof().rarity(Rarity.EPIC));}
    @Override public TypedActionResult<ItemStack> use(World world,PlayerEntity player,Hand hand){
        if(player instanceof ServerPlayerEntity p){var altar=Sanctuaries.nearby(p);if(altar!=null){RitualNetwork.open(p,altar);return TypedActionResult.success(player.getStackInHand(hand));}}
        return TypedActionResult.pass(player.getStackInHand(hand));
    }
}
