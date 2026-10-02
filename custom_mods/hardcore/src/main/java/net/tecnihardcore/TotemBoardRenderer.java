package net.tecnihardcore;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.*;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.*;
import net.minecraft.text.Text;
import net.minecraft.util.math.RotationAxis;
public final class TotemBoardRenderer implements BlockEntityRenderer<TotemBoard.BoardEntity> {
    public TotemBoardRenderer(BlockEntityRendererFactory.Context c){}
    public void render(TotemBoard.BoardEntity e,float delta,MatrixStack m,VertexConsumerProvider v,int light,int overlay){
        var c=MinecraftClient.getInstance();m.push();m.translate(.5,1.50,.36);m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180));
        Item[] items={Hardcore.BRASA,Hardcore.BASTION,Hardcore.ECO};String[] names={"BRASA","BASTIÓN","ECO"};String[] info={"FUEGO 30s","RESISTENCIA II","INVISIBILIDAD"};
        for(int i=0;i<3;i++){m.push();m.translate((i-1)*.78,0,0);m.scale(.60F,.60F,.60F);c.getItemRenderer().renderItem(new ItemStack(items[i]),ModelTransformationMode.FIXED,15728880,overlay,m,v,e.getWorld(),i);m.pop();label(c,m,v,names[i],(i-1)*.78,.38,0xf0d191);label(c,m,v,info[i],(i-1)*.78,-.35,0xbce3d0);}
        label(c,m,v,"RELIQUIAS · INTERACTÚA PARA VER LAS RECETAS",0,.64,0xe5d9bb);m.pop();
    }
    private void label(MinecraftClient c,MatrixStack m,VertexConsumerProvider v,String s,double x,double y,int color){m.push();m.translate(x,y,.10);m.scale(.007F,-.007F,.007F);c.textRenderer.draw(Text.literal(s),-c.textRenderer.getWidth(s)/2F,0,color,false,m.peek().getPositionMatrix(),v,net.minecraft.client.font.TextRenderer.TextLayerType.NORMAL,0,15728880);m.pop();}
    @Override public boolean rendersOutsideBoundingBox(TotemBoard.BoardEntity e){return true;}
}
