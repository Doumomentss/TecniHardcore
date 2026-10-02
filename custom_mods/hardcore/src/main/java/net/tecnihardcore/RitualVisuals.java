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
import net.minecraft.client.network.OtherClientPlayerEntity;
import com.mojang.authlib.GameProfile;
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
        final UUID id,caster,target;final BlockPos altar;final String name;int serverAge,state;long received;boolean completed,cameraCue,appearanceCue;ChannelSound sound;Vec3d anchor,landing;OtherClientPlayerEntity actor;
        Visual(UUID id,UUID caster,UUID target,BlockPos altar,String name){this.id=id;this.caster=caster;this.target=target;this.altar=altar;this.name=name;}
        public double elapsed(float delta){var w=MinecraftClient.getInstance().world;return serverAge+(w==null?0:Math.max(0,w.getTime()-received))+delta;}
    }
    private static final Map<UUID,Visual> visuals=new HashMap<>();
    private static final java.util.Random random=new java.util.Random();
    private static net.minecraft.client.world.ClientWorld lastWorld;
    private static float intensity=1;public static int heartFlash;
    public static Visual at(BlockPos pos){return visuals.values().stream().filter(v->v.altar.equals(pos)).findFirst().orElse(null);}
    public static Visual nearest(){var c=MinecraftClient.getInstance();return c.player==null?null:visuals.values().stream().filter(v->c.player.squaredDistanceTo(Vec3d.ofCenter(v.altar))<=1024).min(Comparator.comparingDouble(v->c.player.squaredDistanceTo(Vec3d.ofCenter(v.altar)))).orElse(null);}
    public static double darkness(){var c=MinecraftClient.getInstance();if(c.player==null)return 0;double value=0;for(var v:visuals.values())value=Math.max(value,CinematicRules.darkness(v.elapsed(c.getTickDelta()),c.player.getPos().distanceTo(Vec3d.ofBottomCenter(v.altar).add(0,4,0))));return value;}
    public static boolean hideBody(UUID target,float delta){for(var v:visuals.values())if(v.target.equals(target)&&v.state==1&&v.elapsed(delta)<680)return true;return false;}
    public static Vec3d actorPosition(Visual v,double age){Vec3d top=Vec3d.ofBottomCenter(v.altar).add(0,CinematicRules.CREST_HEIGHT,0);double t=CinematicRules.descent(age);return top.lerp(v.landing,t).add(0,Math.sin(t*Math.PI)*.8,0);}
    public static void init(){
        ParticleFactoryRegistry.getInstance().register(SanctuaryEffects.SHARD,SoulParticle.Factory::new);
        ParticleFactoryRegistry.getInstance().register(SanctuaryEffects.RUNE,SoulParticle.Factory::new);
        ParticleFactoryRegistry.getInstance().register(SanctuaryEffects.EMBER,s->new SoulParticle.Factory(s,true));
        ParticleFactoryRegistry.getInstance().register(SanctuaryEffects.AZURE,s->(type,w,x,y,z,vx,vy,vz)->{var p=new SoulParticle(w,x,y,z,vx,vy,vz,s,false);p.setColor(.25F,.65F,1F);return p;});
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
            UUID id=b.readUuid();BlockPos altar=b.readBlockPos();UUID caster=b.readUuid(),target=b.readUuid();Vec3d anchor=new Vec3d(b.readDouble(),b.readDouble(),b.readDouble()),landing=new Vec3d(b.readDouble(),b.readDouble(),b.readDouble());int elapsed=b.readVarInt(),state=b.readUnsignedByte();String name=b.readString(32);
            c.execute(()->{
                if(c.world==null)return;Visual v=visuals.get(id);
                if(state==2||state==3){if(v!=null){stop(c,v);if(state==2)sound(c,SanctuaryEffects.CANCEL,altar,.7F);visuals.remove(id);}return;}
                if(v==null){v=new Visual(id,caster,target,altar,name);visuals.put(id,v);if(elapsed<80)sound(c,SanctuaryEffects.WAKE,altar,.7F);}
                v.serverAge=elapsed;v.received=c.world.getTime();v.state=state;v.anchor=anchor;v.landing=landing;
                if(state==1&&!v.completed){v.completed=true;stop(c,v);sound(c,SanctuaryEffects.COMPLETE,altar,.8F);burst(c,altar,80);}
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(RitualNetwork.RELIC,(c,h,b,r)->{int id=b.readInt(),type=b.readUnsignedByte();c.execute(()->{
            if(c.world==null)return;var entity=c.world.getEntityById(id);if(entity==null)return;
            if(type==3&&c.player!=null&&c.player.getId()==id&&RitualCinema.current()==null)heartFlash=60;
            int count=(int)(45*amount(c));for(int i=0;i<count;i++){double a=i*Math.PI*2/Math.max(1,count),rad=.5+random.nextDouble()*.2;c.world.addParticle(type==0?SanctuaryEffects.EMBER:type==1?SanctuaryEffects.RUNE:SanctuaryEffects.SHARD,entity.getX()+Math.cos(a)*rad,entity.getY()+.4+random.nextDouble()*1.5,entity.getZ()+Math.sin(a)*rad,Math.cos(a)*.055,.045,Math.sin(a)*.055);}
        });});
        ClientTickEvents.END_CLIENT_TICK.register(c->{
            if(lastWorld!=c.world){clear(c);lastWorld=c.world;}if(heartFlash>0)heartFlash--;if(c.world==null||c.player==null){clear(c);return;}
            var it=visuals.values().iterator();int emitted=0;while(it.hasNext()){
                Visual v=it.next();double age=v.elapsed(0),dist=c.player.squaredDistanceTo(v.altar.getX()+.5,v.altar.getY()+.5,v.altar.getZ()+.5);
                if(age>700||dist>6400||c.world.getTime()-v.received>60){stop(c,v);it.remove();continue;}
                if(v.state==0&&age>=80&&v.sound==null){v.sound=new ChannelSound(v);c.getSoundManager().play(v.sound);}
                if(age>=200&&!v.cameraCue){v.cameraCue=true;if(age<220)sound(c,SanctuaryEffects.WAKE,v.altar,.4F);}
                if(age>=520&&!v.appearanceCue){v.appearanceCue=true;if(age<540)sound(c,SanctuaryEffects.COMPLETE,v.altar,.35F);}
                if(dist>4096||emitted>=24)continue;
                int count=Math.max(0,(int)(12*amount(c)*(dist<1024?1:.3)));emitted+=count;
                for(int i=0;i<count;i++){
                    double a=age*.11+random.nextDouble()*Math.PI*2,radius=v.completed?Math.min(4,(age-600)*.05):1.6;
                    double x=v.altar.getX()+.5+Math.cos(a)*radius,z=v.altar.getZ()+.5+Math.sin(a)*radius,y=v.altar.getY()+.15+random.nextDouble()*(age>280?2.8:1.5);
                    double vx=-Math.cos(a)*.025,vy=.04,vz=-Math.sin(a)*.025;
                    if(age>=80&&age<280){
                        var target=c.world.getPlayerByUuid(v.target);if(target!=null){double f=random.nextDouble();x=target.getX()+(v.altar.getX()+.5-target.getX())*f+.12*Math.sin(a);y=target.getY()+1+(v.altar.getY()+2.3-target.getY()-1)*f;z=target.getZ()+(v.altar.getZ()+.5-target.getZ())*f+.12*Math.cos(a);Vec3d direction=new Vec3d(v.altar.getX()+.5-x,v.altar.getY()+2.3-y,v.altar.getZ()+.5-z).normalize().multiply(.06);vx=direction.x;vy=direction.y;vz=direction.z;}
                    }else if(age>=280&&age<520){y=v.altar.getY()+1+random.nextDouble()*2;vx=-Math.cos(a)*.065;vz=-Math.sin(a)*.065;}
                    y=v.altar.getY()+(y-v.altar.getY())*3;
                    if(age>=520){Vec3d ap=actorPosition(v,age);x=ap.x+Math.cos(a)*.9;z=ap.z+Math.sin(a)*.9;y=ap.y+random.nextDouble()*2;vx=Math.cos(a)*.025;vz=Math.sin(a)*.025;}
                    c.world.addParticle(age>=520?SanctuaryEffects.AZURE:i%3==0?SanctuaryEffects.RUNE:SanctuaryEffects.SHARD,x,y,z,vx,vy,vz);
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
                dome(m,context.consumers().getBuffer(SanctuaryLayers.DOME),CinematicRules.domeRadius(age),(float)(CinematicRules.fade(age)*.98));
                VertexConsumer buffer=context.consumers().getBuffer(SanctuaryLayers.GLOW);
                ring(m,buffer,3.5,.08,age*.013,alpha*.9F,.1);ring(m,buffer,4,.05,-age*.009,alpha*.7F,.12);
                for(int n=0;n<6;n++)ring(m,buffer,1.5+n*.4,.04,age*(.008+n*.003),alpha*.5F,1+n*1.5+Math.sin(age*.02+n)*.2);
                if(age>280){
                    if(v.actor==null)v.actor=new OtherClientPlayerEntity(c.world,new GameProfile(v.target,v.name));
                    var p=c.world.getPlayerByUuid(v.target);if(!(p instanceof AbstractClientPlayerEntity))p=v.actor;
                    if(p instanceof AbstractClientPlayerEntity player&&c.getEntityRenderDispatcher().getRenderer(player) instanceof PlayerEntityRenderer renderer){
                        float formed=(float)CinematicRules.ease((age-280)/240),ghostAlpha=age>=600?1:formed*.8F;
                        Vec3d actor=actorPosition(v,age),relative=actor.subtract(Vec3d.ofBottomCenter(v.altar));
                        m.push();m.translate(relative.x,relative.y,relative.z);double yaw=Math.atan2(v.anchor.x-actor.x,v.anchor.z-actor.z);
                        m.multiply(RotationAxis.POSITIVE_Y.rotation((float)(Math.PI-yaw)));m.scale(-1,-1,1);m.translate(0,-1.501,0);
                        var model=renderer.getModel();model.setVisible(true);model.setAngles(player,0,0,(float)age,0,0);
                        model.render(m,context.consumers().getBuffer(RenderLayer.getEntityTranslucent(player.getSkinTexture())),0xf000f0,OverlayTexture.DEFAULT_UV,age>=600?1:.6F,age>=600?1:.85F,1,ghostAlpha);m.pop();
                        buffer=context.consumers().getBuffer(SanctuaryLayers.GLOW);
                        m.push();m.translate(relative.x,relative.y,relative.z);for(int n=0;n<5;n++)azureRing(m,buffer,.85+Math.sin(age*.035+n)*.15,.09,age*.05+n,Math.max(.25F,alpha)*.8F,n*.4);aura(m,buffer,age,Math.max(.3F,alpha));m.pop();
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
    private static void azureRing(MatrixStack m,VertexConsumer v,double r,double width,double angle,float alpha,double y){
        Matrix4f mat=m.peek().getPositionMatrix();for(int i=0;i<64;i++){double a=i*Math.PI/32+angle,b=(i+1)*Math.PI/32+angle;
            for(double[] p:new double[][]{{a,r},{b,r},{b,r+width},{a,r+width}})v.vertex(mat,(float)(Math.cos(p[0])*p[1]),(float)y,(float)(Math.sin(p[0])*p[1])).color(.25F,.65F,1F,alpha).next();
        }
    }
    private static void aura(MatrixStack m,VertexConsumer v,double age,float alpha){
        Matrix4f mat=m.peek().getPositionMatrix();for(int strand=0;strand<3;strand++)for(int i=0;i<64;i++){
            double t=i/64.0,next=(i+1)/64.0,a=t*Math.PI*4+age*.045+strand*Math.PI*2/3,b=next*Math.PI*4+age*.045+strand*Math.PI*2/3;
            for(double[] point:new double[][]{{a,t,0},{b,next,0},{b,next,.055},{a,t,.055}}){double r=.78+point[2];v.vertex(mat,(float)(Math.cos(point[0])*r),(float)(point[1]*2.4),(float)(Math.sin(point[0])*r)).color(.2F,.6F,1F,alpha*.8F).next();}
        }
        for(int i=0;i<16;i++){double a=i*Math.PI/8,b=(i+1)*Math.PI/8;for(double[] point:new double[][]{{a,0},{b,0},{b,2.3},{a,2.3}})v.vertex(mat,(float)(Math.cos(point[0])*.62),(float)point[1],(float)(Math.sin(point[0])*.62)).color(.05F,.25F,.8F,alpha*.16F).next();}
    }
    private static void dome(MatrixStack m,VertexConsumer v,double r,float alpha){
        if(r<.5||alpha<.01)return;Matrix4f mat=m.peek().getPositionMatrix();
        for(int j=0;j<24;j++)for(int i=0;i<48;i++){
            double a=i*Math.PI/24,b=(i+1)*Math.PI/24,p=-Math.PI/2+j*Math.PI/24,q=p+Math.PI/24;
            domeVertex(v,mat,a,p,r,alpha);domeVertex(v,mat,a,q,r,alpha);domeVertex(v,mat,b,q,r,alpha);domeVertex(v,mat,b,p,r,alpha);
        }
    }
    private static void domeVertex(VertexConsumer v,Matrix4f mat,double a,double p,double r,float alpha){v.vertex(mat,(float)(Math.cos(a)*Math.cos(p)*r),(float)(4+Math.sin(p)*r),(float)(Math.sin(a)*Math.cos(p)*r)).color(.002F,.003F,.008F,alpha).next();}
    private static void burst(MinecraftClient c,BlockPos p,int count){for(int i=0;i<(int)(count*amount(c));i++){double a=random.nextDouble()*Math.PI*2;c.world.addParticle(SanctuaryEffects.SHARD,p.getX()+.5,p.getY()+2,p.getZ()+.5,Math.cos(a)*(.04+random.nextDouble()*.08),random.nextDouble()*.12,Math.sin(a)*.12);}}
    private static void sound(MinecraftClient c,SoundEvent e,BlockPos p,float volume){c.getSoundManager().play(new PositionedSoundInstance(e,SoundCategory.BLOCKS,volume,1,Random.create(),p.getX()+.5,p.getY()+1,p.getZ()+.5));}
    private static void stop(MinecraftClient c,Visual v){if(v.sound!=null){c.getSoundManager().stop(v.sound);v.sound=null;}}
    private static void clear(MinecraftClient c){for(var v:visuals.values())stop(c,v);visuals.clear();heartFlash=0;RitualCinema.clear();}
    private static final class ChannelSound extends MovingSoundInstance{
        final Visual visual;ChannelSound(Visual v){super(SanctuaryEffects.CHANNEL,SoundCategory.BLOCKS,Random.create());visual=v;x=v.altar.getX()+.5;y=v.altar.getY()+1;z=v.altar.getZ()+.5;volume=.45F;repeat=true;repeatDelay=0;}
        @Override public void tick(){pitch=(float)(1+Math.min(600,visual.elapsed(0))/600*.18);if(visual.completed||!visuals.containsKey(visual.id)||visual.elapsed(0)>620)setDone();}
    }
}
