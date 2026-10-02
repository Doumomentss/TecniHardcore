package net.tecnihardcore;
import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;

public final class SoulParticle extends SpriteBillboardParticle {
    SoulParticle(ClientWorld w,double x,double y,double z,double vx,double vy,double vz,SpriteProvider sprite,boolean ember){
        super(w,x,y,z,vx,vy,vz);velocityX=vx;velocityY=vy;velocityZ=vz;maxAge=24+random.nextInt(18);scale=.07F+random.nextFloat()*.1F;
        setColor(ember?1F:.3F,ember?.53F:.94F,ember?.14F:.74F);setSprite(sprite);collidesWithWorld=false;
    }
    @Override public ParticleTextureSheet getType(){return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;}
    @Override protected int getBrightness(float delta){return 0xf000f0;}
    @Override public void tick(){super.tick();alpha=Math.max(0,1F-(float)age/maxAge);velocityX*=.98;velocityZ*=.98;angle+=.08F;}
    public static final class Factory implements ParticleFactory<DefaultParticleType>{
        private final SpriteProvider sprites;private final boolean ember;
        public Factory(SpriteProvider sprites){this(sprites,false);}
        public Factory(SpriteProvider sprites,boolean ember){this.sprites=sprites;this.ember=ember;}
        @Override public Particle createParticle(DefaultParticleType type,ClientWorld w,double x,double y,double z,double vx,double vy,double vz){return new SoulParticle(w,x,y,z,vx,vy,vz,sprites,ember);}
    }
}
