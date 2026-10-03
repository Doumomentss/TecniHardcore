package net.tecnihardcore;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
public final class TrialBossRenderer extends GeoEntityRenderer<TrialBoss> {
    public TrialBossRenderer(EntityRendererFactory.Context c){super(c,new Model());shadowRadius=1.3F;addRenderLayer(new AutoGlowingGeoLayer<>(this));}
    @Override protected float getDeathMaxRotation(TrialBoss e){return 0;}
    // Keep the final crystal and emissive texture visible throughout the six-second defeat.
    @Override public int getPackedOverlay(TrialBoss e,float white){return e.dying()>8?net.minecraft.client.render.OverlayTexture.DEFAULT_UV:super.getPackedOverlay(e,white);}
    private static final class Model extends GeoModel<TrialBoss>{
        public Identifier getModelResource(TrialBoss e){return Hardcore.id("geo/"+(e.phase()==3&&e.transition()<=60?"espectro":"custodio")+".geo.json");}
        public Identifier getTextureResource(TrialBoss e){return Hardcore.id("textures/entity/"+(e.phase()==3&&e.transition()<=60?"espectro":"custodio")+".png");}
        public Identifier getAnimationResource(TrialBoss e){return Hardcore.id("animations/custodio.animation.json");}
    }
    public static final class ShardRenderer extends GeoEntityRenderer<TrialShard>{
        public ShardRenderer(EntityRendererFactory.Context c){super(c,new GeoModel<TrialShard>(){
            public Identifier getModelResource(TrialShard e){return Hardcore.id("geo/trial_shard.geo.json");}
            public Identifier getTextureResource(TrialShard e){return Hardcore.id("textures/entity/custodio.png");}
            public Identifier getAnimationResource(TrialShard e){return Hardcore.id("animations/custodio.animation.json");}
        });addRenderLayer(new AutoGlowingGeoLayer<>(this));}
    }
}
