package net.tecnihardcore;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
public final class CrystalMountRenderer extends GeoEntityRenderer<CrystalMount>{
    public CrystalMountRenderer(EntityRendererFactory.Context c){super(c,new GeoModel<CrystalMount>(){
        public Identifier getModelResource(CrystalMount e){return Hardcore.id("geo/mount_"+e.tier()+".geo.json");}
        public Identifier getTextureResource(CrystalMount e){return Hardcore.id("textures/entity/mount_"+e.tier()+".png");}
        public Identifier getAnimationResource(CrystalMount e){return Hardcore.id("animations/mount.animation.json");}
    });shadowRadius=1.3F;addRenderLayer(new AutoGlowingGeoLayer<>(this));}
    @Override protected float getDeathMaxRotation(CrystalMount e){return 0;}
}
