package net.tecnihardcore;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
public final class TrialBossRenderer extends GeoEntityRenderer<TrialBoss> {
    public TrialBossRenderer(EntityRendererFactory.Context c){super(c,new Model());shadowRadius=1.3F;addRenderLayer(new AutoGlowingGeoLayer<>(this));}
    private static final class Model extends GeoModel<TrialBoss>{
        public Identifier getModelResource(TrialBoss e){return Hardcore.id("geo/custodio.geo.json");}
        public Identifier getTextureResource(TrialBoss e){return Hardcore.id("textures/entity/custodio.png");}
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
