package net.tecnihardcore;
import net.minecraft.entity.*;import net.minecraft.item.*;import net.minecraft.world.World;import net.minecraft.entity.effect.*;import net.minecraft.util.*;
public final class CustodianHeartItem extends Item{
 public CustodianHeartItem(){super(new Settings().maxCount(1).fireproof().rarity(Rarity.EPIC));}
 @Override public ItemStack finishUsing(ItemStack stack,World world,LivingEntity user){if(!world.isClient){user.setHealth(user.getMaxHealth());user.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION,20*30,2,false,false,true));world.playSound(null,user.getBlockPos(),net.minecraft.sound.SoundEvents.ITEM_TOTEM_USE,net.minecraft.sound.SoundCategory.PLAYERS,1.2F,1.1F);}return super.finishUsing(stack,world,user);}
 @Override public int getMaxUseTime(ItemStack stack){return 32;}
 @Override public UseAction getUseAction(ItemStack stack){return UseAction.EAT;}
}
