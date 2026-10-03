package net.tecnihardcore;
import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;
public final class AcidDrop extends SpriteBillboardParticle {
    private AcidDrop(ClientWorld w,double x,double y,double z,SpriteProvider sprite){super(w,x,y,z);velocityY=-1.1;gravityStrength=.3F;maxAge=24;scale=.09F;setColor(1,.88F,.12F);setSprite(sprite);collidesWithWorld=true;}
    @Override public ParticleTextureSheet getType(){return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;}
    @Override public void tick(){super.tick();if(onGround){velocityY=0;scale*=1.25F;alpha*=.55F;if(alpha<.08F)markDead();}}
    public static final class Factory implements ParticleFactory<DefaultParticleType>{private final SpriteProvider sprites;public Factory(SpriteProvider sprites){this.sprites=sprites;}public Particle createParticle(DefaultParticleType type,ClientWorld w,double x,double y,double z,double vx,double vy,double vz){return new AcidDrop(w,x,y,z,sprites);}}
}
