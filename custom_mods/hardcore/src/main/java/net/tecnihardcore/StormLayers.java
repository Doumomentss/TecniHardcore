package net.tecnihardcore;
import net.minecraft.client.render.*;
import net.minecraft.util.Identifier;
/** World geometry with depth testing, independent of a shader pack's terrain fog distance. */
final class StormLayers extends RenderLayer {
    private static net.minecraft.client.gl.ShaderProgram shader;
    private static final ShaderProgram STORM_PROGRAM=new ShaderProgram(()->shader);
    static void init(){net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback.EVENT.register(context->context.register(Hardcore.id("storm"),VertexFormats.POSITION_COLOR_TEXTURE,program->shader=program));}
    private StormLayers(){super("tecni_storm",VertexFormats.POSITION_COLOR_TEXTURE,VertexFormat.DrawMode.QUADS,65536,false,true,()->{},()->{});}
    private static final java.util.Map<Identifier,RenderLayer> layers=new java.util.HashMap<>();
    private static final RenderLayer SIGNAL=of("tecni_rescue_ray",VertexFormats.LINES,VertexFormat.DrawMode.LINES,4096,false,true,MultiPhaseParameters.builder().program(LINES_PROGRAM).transparency(TRANSLUCENT_TRANSPARENCY).cull(DISABLE_CULLING).depthTest(ALWAYS_DEPTH_TEST).writeMaskState(COLOR_MASK).lineWidth(new LineWidth(java.util.OptionalDouble.of(3))).build(false));
    static RenderLayer signal(){return SIGNAL;}
    static RenderLayer texture(Identifier texture){return layers.computeIfAbsent(texture,t->of("tecni_storm_"+t.getPath(),VertexFormats.POSITION_COLOR_TEXTURE,VertexFormat.DrawMode.QUADS,65536,false,true,MultiPhaseParameters.builder().program(STORM_PROGRAM).texture(new Texture(t,false,false)).transparency(TRANSLUCENT_TRANSPARENCY).cull(DISABLE_CULLING).depthTest(LEQUAL_DEPTH_TEST).writeMaskState(COLOR_MASK).build(false)));}
}
