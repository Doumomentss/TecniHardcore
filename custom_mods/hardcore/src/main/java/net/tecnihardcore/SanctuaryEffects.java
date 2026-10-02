package net.tecnihardcore;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.registry.*;
import net.minecraft.sound.SoundEvent;
public final class SanctuaryEffects {
    public static final DefaultParticleType SHARD=particle("soul_shard"),RUNE=particle("soul_rune"),EMBER=particle("relic_ember"),AZURE=particle("soul_azure");
    public static final SoundEvent WAKE=sound("sanctuary.wake"),CHANNEL=sound("sanctuary.channel"),COMPLETE=sound("sanctuary.complete"),CANCEL=sound("sanctuary.cancel");
    private static DefaultParticleType particle(String id){return Registry.register(Registries.PARTICLE_TYPE,Hardcore.id(id),FabricParticleTypes.simple());}
    private static SoundEvent sound(String id){return Registry.register(Registries.SOUND_EVENT,Hardcore.id(id),SoundEvent.of(Hardcore.id(id)));}
    public static void init(){}
}
