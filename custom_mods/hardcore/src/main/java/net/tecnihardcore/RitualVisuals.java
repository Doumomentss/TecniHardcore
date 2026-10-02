package net.tecnihardcore;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.sound.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.*;
import net.minecraft.text.Text;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import org.joml.Matrix4f;
import java.util.*;
import java.nio.file.*;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public final class RitualVisuals {
    public static final class Visual {
        final UUID id,target;final BlockPos altar;final String name;int serverAge,state;long received;boolean completed;ChannelSound sound;
        Visual(UUID id,UUID target,BlockPos altar,String name){this.id=id;this.target=target;this.altar=altar;this.name=name;}
        public double elapsed(float delta){var w=MinecraftClient.getInstance().world;return serverAge+(w==null?0:Math.max(0,w.getTime()-received))+delta;}
    }
    private static final Map<UUID,Visual> visuals=new HashMap<>();
    private static final java.util.Random random=new java.util.Random();
    private static net.minecraft.client.world.ClientWorld lastWorld;
    private static float intensity=1;public static int heartFlash;
    public static Visual at(BlockPos pos){return visuals.values().stream().filter(v->v.altar.equals(pos)).findFirst().orElse(null);}
    public static Visual nearest(){var c=MinecraftClient.getInstance();return c.player==null?null:visuals.values().stream().filter(v->c.player.squaredDistanceTo(v.altar.getX()+.5,v.altar.getY()+.5,v.altar.getZ()+.5)<=256).min(Comparator.comparingDouble(v->c.player.squaredDistanceTo(v.altar.getX(),v.altar.getY(),v.altar.getZ()))).orElse(null);}
    public static void init(){
        ParticleFactoryRegistry.getInstance().register(SanctuaryEffects.SHARD,SoulParticle.Factory::new);
        ParticleFactoryRegistry.getInstance().register(SanctuaryEffects.RUNE,SoulParticle.Factory::new);
        ParticleFactoryRegistry.getInstance().register(SanctuaryEffects.EMBER,s->new SoulParticle.Factory(s,true));
        Path config=MinecraftClient.getInstance().runDirectory.toPath().resolve("config/tecnihardcore-effects.txt");
        try{if(Files.exists(config))intensity=MathHelper.clamp(Float.parseFloat(Files.readString(config).trim()),.25F,1);}catch(Exception ignored){}
        ClientCommandRegistrationCallback.EVENT.register((dispatcher,registry)->dispatcher.register(literal("tecni-efectos").executes(ctx->{
            intensity=intensity>.8?.5F:intensity>.4?.25F:1F;try{Files.createDirectories(config.getParent());Files.writeString(config,Float.toString(intensity));}catch(Exception e){Hardcore.LOG.warn("Cannot save effects preference");}
            ctx.getSource().sendFeedback(Text.literal("Efectos TecniHardcore: "+(int)(intensity*100)+"%"));return 1;
        })));
        ClientPlayNetworking.registerGlobalReceiver(RitualNetwork.OPEN,(c,h,b,r)->{
            BlockPos pos=b.readBlockPos();UUID nonce=b.readUuid();String problem=b.readString(256);int n=b.readVarInt();if(n<0||n>128)return;
            List<SanctuaryScreen.Candidate> entries=new ArrayList<>();for(int i=0;i<n;i++)entries.add(new SanctuaryScreen.Candidate(b.readUuid(),b.readString(32),b.readVarInt()));
            c.execute(()->{if(c.currentScreen instanceof SanctuaryScreen screen&&screen.altar().equals(pos))screen.update(nonce,problem,entries);else c.setScreen(new SanctuaryScreen(pos,nonce,problem,entries));});
        });
        ClientPlayNetworking.registerGlobalReceiver(RitualNetwork.STATE,(c,h,b,r)->{
            UUID id=b.readUuid();BlockPos altar=b.readBlockPos();UUID target=b.readUuid();int elapsed=b.readVarInt(),state=b.readUnsignedByte();String name=b.readString(32);
            c.execute(()->{
                if(c.world==null)return;Visual v=visuals.get(id);
                if(state==2||state==3){if(v!=null){stop(c,v);if(state==2)sound(c,SanctuaryEffects.CANCEL,altar,.7F);visuals.remove(id);}return;}
                if(v==null){v=new Visual(id,target,altar,name);visuals.put(id,v);if(elapsed<80)sound(c,SanctuaryEffects.WAKE,altar,.7F);}
                v.serverAge=elapsed;v.received=c.world.getTime();v.state=state;
                if(state==1&&!v.completed){v.completed=true;stop(c,v);sound(c,SanctuaryEffects.COMPLETE,altar,.8F);burst(c,altar,80);}
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(RitualNetwork.RELIC,(c,h,b,r)->{int id=b.readInt(),type=b.readUnsignedByte();c.execute(()->{
            if(c.world==null)return;var entity=c.world.getEntityById(id);if(entity==null)return;
            if(type==3&&c.player!=null&&c.player.getId()==id)heartFlash=60;
            int count=(int)(45*amount(c));for(int i=0;i<count;i++){double a=i*Math.PI*2/Math.max(1,count),rad=.5+random.nextDouble()*.2;c.world.addParticle(type==0?SanctuaryEffects.EMBER:type==1?SanctuaryEffects.RUNE:SanctuaryEffects.SHARD,entity.getX()+Math.cos(a)*rad,entity.getY()+.4+random.nextDouble()*1.5,entity.getZ()+Math.sin(a)*rad,Math.cos(a)*.055,.045,Math.sin(a)*.055);}
        });});
        ClientTickEvents.END_CLIENT_TICK.register(c->{
            if(lastWorld!=c.world){clear(c);lastWorld=c.world;}if(heartFlash>0)heartFlash--;if(c.world==null||c.player==null){clear(c);return;}
            var it=visuals.values().iterator();int emitted=0;while(it.hasNext()){
                Visual v=it.next();double age=v.elapsed(0),dist=c.player.squaredDistanceTo(v.altar.getX()+.5,v.altar.getY()+.5,v.altar.getZ()+.5);
                if(age>700||dist>6400||c.world.getTime()-v.received>60){stop(c,v);it.remove();continue;}
                if(v.state==0&&age>=80&&v.sound==null){v.sound=new ChannelSound(v);c.getSoundManager().play(v.sound);}
                if(dist>4096||emitted>=8)continue;
                int count=Math.max(0,(int)(4*amount(c)*(dist<1024?1:.4)));emitted+=count;
                for(int i=0;i<count;i++){
                    double a=age*.11+random.nextDouble()*Math.PI*2,radius=v.completed?Math.min(4,(age-600)*.05):1.6;
                    double x=v.altar.getX()+.5+Math.cos(a)*radius,z=v.altar.getZ()+.5+Math.sin(a)*radius,y=v.altar.getY()+.15+random.nextDouble()*(age>280?2.8:1.5);
                    double vx=-Math.cos(a)*.025,vy=.04,vz=-Math.sin(a)*.025;
                    if(age>=80&&age<280){
                        var target=c.world.getPlayerByUuid(v.target);if(target!=null){double f=random.nextDouble();x=target.getX()+(v.altar.getX()+.5-target.getX())*f+.12*Math.sin(a);y=target.getY()+1+(v.altar.getY()+2.3-target.getY()-1)*f;z=target.getZ()+(v.altar.getZ()+.5-target.getZ())*f+.12*Math.cos(a);Vec3d direction=new Vec3d(v.altar.getX()+.5-x,v.altar.getY()+2.3-y,v.altar.getZ()+.5-z).normalize().multiply(.06);vx=direction.x;vy=direction.y;vz=direction.z;}
                    }else if(age>=280&&age<520){y=v.altar.getY()+1+random.nextDouble()*2;vx=-Math.cos(a)*.065;vz=-Math.sin(a)*.065;}
                    c.world.addParticle(age>520&&i%4==0?SanctuaryEffects.EMBER:i%3==0?SanctuaryEffects.RUNE:SanctuaryEffects.SHARD,x,y,z,vx,vy,vz);
                }
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((h,c)->clear(c));
        WorldRenderEvents.AFTER_ENTITIES.register(context->{
            var c=MinecraftClient.getInstance();if(c.world==null||c.player==null||context.matrixStack()==null)return;
            MatrixStack m=context.matrixStack();Vec3d camera=context.camera().getPos();m.push();m.translate(-camera.x,-camera.y,-camera.z);
            for(Visual v:visuals.values()){
                if(c.player.squaredDistanceTo(v.altar.getX()+.5,v.altar.getY(),v.altar.getZ()+.5)>4096)continue;
                double age=v.elapsed(context.tickDelta());float alpha=(float)(v.completed?Math.max(0,(680-age)/80):Math.min(1,age/80));
                m.push();m.translate(v.altar.getX()+.5,v.altar.getY()+.04,v.altar.getZ()+.5);
                VertexConsumer buffer=context.consumers().getBuffer(RenderLayer.getLightning());
                ring(m,buffer,1.6,.024,age*.013,alpha*.5F,0);ring(m,buffer,2.1,.015,-age*.009,alpha*.35F,0);
                ring(m,buffer,v.completed?1.5+(age-600)*.07:1.1,.045,age*.017,alpha*.35F,v.completed?.12:1.9+Math.sin(age*.06)*.1);
                if(age>80&&!v.completed){
                    var p=c.world.getPlayerByUuid(v.target);
                    if(p instanceof AbstractClientPlayerEntity player&&c.getEntityRenderDispatcher().getRenderer(player) instanceof PlayerEntityRenderer renderer){
                        float formed=(float)Math.min(1,(age-80)/200),ghostScale=.35F+.5F*formed,ghostAlpha=alpha*.32F*formed*(float)(age>520?Math.max(0,(600-age)/80):1);
                        m.push();m.translate(0,1.05+Math.sin(age*.025)*.08,0);m.multiply(RotationAxis.POSITIVE_Y.rotation((float)(age*.007)));m.scale(-ghostScale,-ghostScale,ghostScale);m.translate(0,-1.5,0);
                        var model=renderer.getModel();model.setVisible(true);model.setAngles(player,0,0,(float)age,0,0);
                        model.render(m,context.consumers().getBuffer(RenderLayer.getEntityTranslucent(player.getSkinTexture())),0xf000f0,OverlayTexture.DEFAULT_UV,.55F,1F,.86F,ghostAlpha);m.pop();
                    }
                }
                m.pop();
            }
            m.pop();
        });
    }
    private static float amount(MinecraftClient c){return intensity*(c.options.getParticles().getValue()==net.minecraft.client.option.ParticlesMode.MINIMAL?.25F:c.options.getParticles().getValue()==net.minecraft.client.option.ParticlesMode.DECREASED?.6F:1);}
    private static void ring(MatrixStack m,VertexConsumer v,double radius,double thickness,double angle,float alpha,double y){
        Matrix4f mat=m.peek().getPositionMatrix();for(int i=0;i<64;i++){if(i%8==7)continue;double a=i*Math.PI/32+angle,b=(i+1)*Math.PI/32+angle;
            vertex(v,mat,a,radius,y,alpha);vertex(v,mat,b,radius,y,alpha);vertex(v,mat,b,radius+thickness,y,alpha);vertex(v,mat,a,radius+thickness,y,alpha);
        }
    }
    private static void vertex(VertexConsumer v,Matrix4f mat,double a,double r,double y,float alpha){v.vertex(mat,(float)(Math.cos(a)*r),(float)y,(float)(Math.sin(a)*r)).color(.25F,.95F,.7F,alpha).next();}
    private static void burst(MinecraftClient c,BlockPos p,int count){for(int i=0;i<(int)(count*amount(c));i++){double a=random.nextDouble()*Math.PI*2;c.world.addParticle(SanctuaryEffects.SHARD,p.getX()+.5,p.getY()+2,p.getZ()+.5,Math.cos(a)*(.04+random.nextDouble()*.08),random.nextDouble()*.12,Math.sin(a)*.12);}}
    private static void sound(MinecraftClient c,SoundEvent e,BlockPos p,float volume){c.getSoundManager().play(new PositionedSoundInstance(e,SoundCategory.BLOCKS,volume,1,Random.create(),p.getX()+.5,p.getY()+1,p.getZ()+.5));}
    private static void stop(MinecraftClient c,Visual v){if(v.sound!=null){c.getSoundManager().stop(v.sound);v.sound=null;}}
    private static void clear(MinecraftClient c){for(var v:visuals.values())stop(c,v);visuals.clear();heartFlash=0;}
    private static final class ChannelSound extends MovingSoundInstance{
        final Visual visual;ChannelSound(Visual v){super(SanctuaryEffects.CHANNEL,SoundCategory.BLOCKS,Random.create());visual=v;x=v.altar.getX()+.5;y=v.altar.getY()+1;z=v.altar.getZ()+.5;volume=.45F;repeat=true;repeatDelay=0;}
        @Override public void tick(){pitch=(float)(1+Math.min(600,visual.elapsed(0))/600*.18);if(visual.completed||!visuals.containsKey(visual.id)||visual.elapsed(0)>620)setDone();}
    }
}
