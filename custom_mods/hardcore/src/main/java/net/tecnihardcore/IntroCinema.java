package net.tecnihardcore;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import java.util.UUID;

/** Per-player, render-only camera. The server owns completion and immobilization. */
public final class IntroCinema {
    private static UUID id;
    private static String season;
    private static BlockPos center;
    private static long started;
    private static long lastReady, lastAck;
    private static boolean completed;
    private static boolean prepared;
    private static WelcomeSound soundtrack;
    private IntroCinema() {}

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(IntroServer.START,(c,h,b,r)->{
            UUID sequence=b.readUuid();String worldSeason=b.readString(64);BlockPos at=b.readBlockPos();long length=b.readVarLong();
            c.execute(()->{if(length!=IntroRules.DURATION_MS)return;stopSound(c);id=sequence;season=worldSeason;center=at;started=System.nanoTime();lastAck=0;completed=false;prepared=true;c.setScreen(null);if(c.player!=null){soundtrack=new WelcomeSound(sequence);c.getSoundManager().play(soundtrack);}});
        });
        ClientPlayNetworking.registerGlobalReceiver(IntroServer.PREPARE,(c,h,b,r)->{b.readUuid();c.execute(()->prepared=true);});
        ClientPlayNetworking.registerGlobalReceiver(IntroServer.DONE,(c,h,b,r)->{
            UUID sequence=b.readUuid();boolean recorded=b.readBoolean();c.execute(()->{if(id==null || id.equals(sequence)){stopSound(c);id=null;completed=recorded;}});
        });
        ClientPlayNetworking.registerGlobalReceiver(IntroServer.RESET,(c,h,b,r)->{
            b.readUuid();c.execute(()->{stopSound(c);id=null;completed=false;prepared=false;lastReady=0;});
        });
        ClientPlayConnectionEvents.DISCONNECT.register((h,c)->{stopSound(c);id=null;season=null;center=null;started=0;completed=false;prepared=false;lastReady=0;lastAck=0;});
        ClientTickEvents.END_CLIENT_TICK.register(c->{
            if(c.player==null || c.world==null)return;
            long now=System.nanoTime();
            if(id!=null){
                if(c.currentScreen!=null)c.setScreen(null);
                if(c.player.age%3==0 && center!=null){
                    double t=elapsed()/1000.0,a=t*1.7;
                    for(int i=0;i<3;i++){
                        double r=2.2+i*.85,theta=a+i*2.094;
                        c.world.addParticle(ParticleTypes.END_ROD,center.getX()+.5+Math.cos(theta)*r,center.getY()+1.2+i*.65,center.getZ()+.5+Math.sin(theta)*r,0,.012,0);
                    }
                    if(c.player.age%9==0)c.world.addParticle(ParticleTypes.GLOW,center.getX()+.5,center.getY()+5,center.getZ()+.5,0,.035,0);
                }
                if(elapsed()>=IntroRules.DURATION_MS && now-lastAck>1_000_000_000L){
                    var b=PacketByteBufs.create();b.writeUuid(id);ClientPlayNetworking.send(IntroServer.ACK,b);lastAck=now;
                }
            }else if(!completed && c.currentScreen==null && c.world.isChunkLoaded(c.player.getBlockX()>>4,c.player.getBlockZ()>>4) && (!prepared || c.world.isChunkLoaded(0,0)) && now-lastReady>1_000_000_000L){
                ClientPlayNetworking.send(IntroServer.READY,PacketByteBufs.empty());lastReady=now;
            }
        });
    }

    public static boolean active(){return id!=null && MinecraftClient.getInstance().player!=null && MinecraftClient.getInstance().world!=null;}
    private static void stopSound(MinecraftClient c){if(soundtrack!=null){c.getSoundManager().stop(soundtrack);soundtrack=null;}}
    private static final class WelcomeSound extends MovingSoundInstance {
        private final UUID sequence;
        WelcomeSound(UUID session){super(Registries.SOUND_EVENT.get(Hardcore.id("intro.welcome")),SoundCategory.MUSIC,Random.create());sequence=session;volume=.82F;pitch=1;relative=true;repeat=false;}
        @Override public void tick(){if(!sequence.equals(id)||elapsed()>IntroRules.TIMEOUT_MS)setDone();}
    }
    public static long elapsed(){return started==0?0:Math.max(0,(System.nanoTime()-started)/1_000_000);}
    public static Vec3d[] pose(float delta){
        MinecraftClient c=MinecraftClient.getInstance();
        if(!active() || center==null)return null;
        double t=Math.min(1,elapsed()/(double)IntroRules.DURATION_MS);
        Vec3d core=Vec3d.ofBottomCenter(center).add(0,4,0);
        Vec3d eye=c.player.getEyePos();
        double phase=IntroRules.ease(Math.min(1,t/.82));
        double angle=-Math.PI*.68+phase*Math.PI*2.04;
        double radius=46-25*phase;
        double height=62-46*phase;
        Vec3d orbit=core.add(Math.cos(angle)*radius,height,Math.sin(angle)*radius);
        Vec3d focus=core.add(0,3,0);
        if(t>.82){double finish=IntroRules.ease((t-.82)/.18);orbit=orbit.lerp(eye,finish);focus=focus.lerp(eye.add(c.player.getRotationVec(1).multiply(16)),finish);}
        return new Vec3d[]{orbit,focus};
    }
    public static void hud(DrawContext d,float delta){
        if(!active())return;
        MinecraftClient c=MinecraftClient.getInstance();int w=d.getScaledWindowWidth(),h=d.getScaledWindowHeight();
        int letter=Math.max(22,h/11);
        d.fill(0,0,w,letter,0xf0091018);d.fill(0,h-letter,w,h,0xf0091018);
        double t=IntroRules.progress(elapsed());
        String title=t<.14?"UN MUNDO NUEVO":t<.58?"TECNIHARDCORE":t<.82?"EL SANTUARIO DE LAS ALMAS":"TU HISTORIA COMIENZA";
        d.drawCenteredTextWithShadow(c.textRenderer,title,w/2,letter/2-5,0xffe3ba62);
        String line=t<.58?"Cinco vidas. Cada decisión cuenta.":t<.82?"Una plaza. Una segunda oportunidad.":"Sobrevive, explora y escribe tu leyenda.";
        d.drawCenteredTextWithShadow(c.textRenderer,line,w/2,h-letter+8,0xffd6eadb);
        int hearts=5, gap=23, left=(w-hearts*gap)/2+2;
        for(int i=0;i<hearts;i++){
            float a=(float)IntroRules.ease((t-.58-i*.035)/.13);
            if(a<=0)continue;
            d.setShaderColor(1,1,1,a);
            d.drawTexture(Hardcore.id("textures/gui/heart.png"),left+i*gap,h/2-12,0,0,20,20,20,20);
        }
        d.setShaderColor(1,1,1,1);
        int bar=Math.min(260,w-40),x=(w-bar)/2;
        d.fill(x,h-letter-7,x+bar,h-letter-4,0xff203746);
        d.fill(x,h-letter-7,x+(int)(bar*t),h-letter-4,0xffe3ba62);
        int fade=(int)(210*(1-IntroRules.ease(elapsed()/2100.0))+210*IntroRules.ease((t-.96)/.04));
        if(fade>0)d.fill(0,letter,w,h-letter,(Math.min(255,fade)<<24)|0x05080c);
    }
}
