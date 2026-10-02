package net.tecnihardcore;
import net.minecraft.client.render.*;
public final class SanctuaryLayers extends RenderLayer {
    private SanctuaryLayers(){super("unused",VertexFormats.POSITION_COLOR,VertexFormat.DrawMode.QUADS,256,false,true,()->{},()->{});}
    public static final RenderLayer DOME=of("tecni_black_dome",VertexFormats.POSITION_COLOR,VertexFormat.DrawMode.QUADS,65536,false,true,
        MultiPhaseParameters.builder().program(COLOR_PROGRAM).transparency(TRANSLUCENT_TRANSPARENCY).cull(DISABLE_CULLING).writeMaskState(ALL_MASK).build(false));
    public static final RenderLayer GLOW=of("tecni_soul_glow",VertexFormats.POSITION_COLOR,VertexFormat.DrawMode.QUADS,32768,false,true,
        MultiPhaseParameters.builder().program(COLOR_PROGRAM).transparency(LIGHTNING_TRANSPARENCY).cull(DISABLE_CULLING).writeMaskState(COLOR_MASK).build(false));
}
