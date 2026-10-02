package net.tecnihardcore;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.*;
import net.minecraft.text.Text;
public final class TotemGuideScreen extends Screen {
    private int selected;
    private int scroll;
    private final String[] names={"BRASA","BASTIÓN","ECO"};
    private final String[] effects={"Resistencia al fuego: 30 s. Debilidad II: 30 s.","Resistencia II: 10 s. Lentitud II: 20 s.","Invisibilidad: 15 s. Debilidad II: 20 s. Sin regeneración."};
    private final String[] seals={"Primer Wither derrotado: sello de Brasa.","Primer dragón derrotado: sello de Bastión.","Primer guardián anciano derrotado: sello de Eco."};
    public TotemGuideScreen(){super(Text.literal("Reliquias de TecniHardcore"));}
    @Override protected void init(){int w=Math.min(width-20,410),left=(width-w)/2;for(int i=0;i<3;i++){final int n=i;addDrawableChild(ButtonWidget.builder(Text.literal(names[i]),b->selected=n).dimensions(left+i*w/3,27,w/3-3,20).build());}addDrawableChild(ButtonWidget.builder(Text.literal("Cerrar"),b->close()).dimensions(width/2-45,height-24,90,20).build());}
    @Override public void render(DrawContext d,int mx,int my,float delta){
        renderBackground(d);int w=Math.min(width-20,410),left=(width-w)/2;d.fill(left-5,8,left+w+5,height-28,0xf01a232b);d.drawCenteredTextWithShadow(textRenderer,title,width/2,14,0xf0d191);
        Item relic=selected==0?Hardcore.BRASA:selected==1?Hardcore.BASTION:Hardcore.ECO;
        d.getMatrices().push();d.getMatrices().translate(left+8,55,0);d.getMatrices().scale(2,2,1);d.drawItem(new ItemStack(relic),0,0);d.getMatrices().pop();
        d.enableScissor(left,50,left+w,height-29);d.getMatrices().push();d.getMatrices().translate(0,-scroll,0);
        int y=55; y=lines(d,effects[selected],left+49,y,w-55,0xb9e5e4)+8;
        y=lines(d,"Deja 2 corazones. Debe estar en cualquiera de las manos.",left+49,y,w-55,0xe0d9cd)+10;
        y=Math.max(101,y);d.drawTextWithShadow(textRenderer,"FABRICACIÓN",left+8,y,0xf0d191);y+=14;
        Item seal=selected==0?Hardcore.SEAL_BRASA:selected==1?Hardcore.SEAL_BASTION:Hardcore.SEAL_ECO;
        Item[] ingredients={Items.TOTEM_OF_UNDYING,Items.NETHERITE_INGOT,Items.NETHER_STAR,seal};for(int i=0;i<4;i++){d.drawItem(new ItemStack(ingredients[i]),left+8+i*32,y);if(i<3)d.drawTextWithShadow(textRenderer,"+",left+29+i*32,y+5,0xffffff);}y+=23;
        y=lines(d,"1 tótem vanilla + 1 lingote de netherita + 1 estrella del Nether + 1 sello.",left+8,y,w-16,0xe0d9cd)+6;
        y=lines(d,seals[selected]+" El sello lo recibe quien da el golpe final, una vez por jefe y jugador.",left+8,y,w-16,0xb9e5e4)+8;
        lines(d,"Todos los tótems, incluido el vanilla, comparten 5 minutos de enfriamiento. No recuperan vidas. No salvan del vacío ni de /kill.",left+8,y,w-16,0xf3b79b);
        d.getMatrices().pop();d.disableScissor();
        super.render(d,mx,my,delta);
    }
    private int lines(DrawContext d,String s,int x,int y,int w,int color){for(var line:textRenderer.wrapLines(Text.literal(s),w)){d.drawTextWithShadow(textRenderer,line,x,y,color);y+=10;}return y;}
    @Override public boolean shouldPause(){return false;}
    @Override public boolean mouseScrolled(double x,double y,double amount){scroll=Math.max(0,Math.min(Math.max(0,290-height),(int)(scroll-amount*18)));return true;}
}
