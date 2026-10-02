package net.tecnihardcore;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.core.animation.AnimationState;
import net.minecraft.util.Identifier;

public final class SanctuaryRenderer extends GeoBlockRenderer<SanctuaryEntity>{
    public SanctuaryRenderer(BlockEntityRendererFactory.Context context){super(new Model());addRenderLayer(new AutoGlowingGeoLayer<>(this));}
    @Override public int getRenderDistance(){return 80;}
    private static final class Model extends GeoModel<SanctuaryEntity>{
        @Override public Identifier getModelResource(SanctuaryEntity e){return Hardcore.id("geo/santuario.geo.json");}
        @Override public Identifier getTextureResource(SanctuaryEntity e){return Hardcore.id("textures/block/santuario.png");}
        @Override public Identifier getAnimationResource(SanctuaryEntity e){return Hardcore.id("animations/santuario.animation.json");}
        @Override public void setCustomAnimations(SanctuaryEntity e,long id,AnimationState<SanctuaryEntity> s){
            super.setCustomAnimations(e,id,s);var visual=RitualVisuals.at(e.getPos());double elapsed=visual==null?0:visual.elapsed(s.getPartialTick());
            double power=visual==null?0:Math.min(1,elapsed/80)*(elapsed>600?Math.max(0,(680-elapsed)/80):1),t=e.getWorld().getTime()+s.getPartialTick();
            getAnimationProcessor().getBone("ring_outer").setRotY((float)(t*.018+elapsed*.065));
            var inner=getAnimationProcessor().getBone("ring_inner");inner.setRotX((float)(.35+Math.sin(t*.022)*.14+power*.7));inner.setRotZ((float)(t*-.025-elapsed*.052));
            var crystal=getAnimationProcessor().getBone("crystal");crystal.setPosY((float)(Math.sin(t*.05)*1.5+power*8));crystal.setRotY((float)(t*.025+elapsed*.04));
            float scale=(float)(1+power*.16+power*.04*Math.sin(t*.22));crystal.setScaleX(scale);crystal.setScaleY(scale);crystal.setScaleZ(scale);
            double spread=visual==null?0:elapsed<80?0:elapsed<280?Math.min(1,(elapsed-80)/140):Math.max(0,1-(elapsed-280)/240);
            spread=spread*spread*(3-2*spread);
            for(int i=0;i<10;i++){
                var fragment=getAnimationProcessor().getBone("fragment_"+i);if(fragment==null)continue;double angle=i*2.399+t*.035;
                fragment.setPosX((float)(Math.cos(angle)*spread*(10+i*.6)));fragment.setPosZ((float)(Math.sin(angle)*spread*(10+i*.6)));fragment.setPosY((float)(spread*(i-4.5)*1.5));
                fragment.setRotX((float)(spread*Math.sin(t*.02+i)*.6));fragment.setRotZ((float)(spread*Math.cos(t*.02+i)*.6));
            }
        }
    }
}
