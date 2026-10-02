package net.tecnihardcore;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import java.util.*;

public final class SanctuaryScreen extends Screen {
    public record Candidate(UUID id,String name,int count){}
    private final BlockPos altar;private UUID nonce,selected;private List<Candidate> candidates;private String problem;private int page,ticks;
    private int left,top,w,h,rows;private ButtonWidget start;
    public SanctuaryScreen(BlockPos altar,UUID nonce,String problem,List<Candidate> candidates){super(Text.literal("Santuario de las Almas"));this.altar=altar;update(nonce,problem,candidates);}
    public BlockPos altar(){return altar;}
    public boolean hasCandidates(){return !candidates.isEmpty();}
    public void update(UUID nonce,String problem,List<Candidate> candidates){this.nonce=nonce;this.problem=problem;this.candidates=candidates;if(selected!=null&&candidates.stream().noneMatch(c->c.id.equals(selected)))selected=null;if(client!=null)init(client,width,height);}
    @Override protected void init(){
        w=Math.min(370,width-16);h=Math.min(262,height-16);left=(width-w)/2;top=(height-h)/2;rows=Math.max(1,(h-132)/24);page=Math.min(page,Math.max(0,(candidates.size()-1)/rows));
        for(int i=0;i<rows&&page*rows+i<candidates.size();i++){
            Candidate candidate=candidates.get(page*rows+i);boolean chosen=candidate.id.equals(selected);
            addDrawableChild(ButtonWidget.builder(Text.literal((chosen?"◆ ":"")+candidate.name+" · "+candidate.count+" resurrecciones"),b->{selected=candidate.id;clearChildren();init();}).dimensions(left+14,top+81+i*24,w-28,20).build());
        }
        start=addDrawableChild(ButtonWidget.builder(Text.literal("INICIAR RESURRECCIÓN"),b->{
            if(selected==null)return;var data=PacketByteBufs.create();data.writeBlockPos(altar);data.writeUuid(nonce);data.writeUuid(selected);ClientPlayNetworking.send(RitualNetwork.SELECT,data);client.setScreen(null);
        }).dimensions(left+14,top+h-49,w-28,20).build());start.active=selected!=null&&problem.isEmpty();
        addDrawableChild(ButtonWidget.builder(Text.literal("Cerrar"),b->close()).dimensions(left+w-69,top+h-24,55,18).build());
        if(candidates.size()>rows){addDrawableChild(ButtonWidget.builder(Text.literal("‹"),b->{page=Math.max(0,page-1);clearChildren();init();}).dimensions(left+14,top+h-24,20,18).build());addDrawableChild(ButtonWidget.builder(Text.literal("›"),b->{page=Math.min((candidates.size()-1)/rows,page+1);clearChildren();init();}).dimensions(left+38,top+h-24,20,18).build());}
    }
    @Override public void tick(){if(++ticks%40==0){var data=PacketByteBufs.create();data.writeBlockPos(altar);ClientPlayNetworking.send(RitualNetwork.QUERY,data);}if(client.player==null||client.player.squaredDistanceTo(altar.getX()+.5,altar.getY()+.5,altar.getZ()+.5)>20)close();}
    @Override public void render(DrawContext d,int mx,int my,float delta){
        renderBackground(d);d.fill(left-1,top-1,left+w+1,top+h+1,0xffb99852);d.fillGradient(left,top,left+w,top+h,0xf0142226,0xf0061018);
        d.drawCenteredTextWithShadow(textRenderer,title,left+w/2,top+12,0xffefd3a0);
        d.drawCenteredTextWithShadow(textRenderer,Text.literal("Un alma perdida. Una nueva oportunidad."),left+w/2,top+27,0xff8bcbb6);
        d.drawTextWithShadow(textRenderer,"Ofrenda: 1 Corazón Sagrado · Regresa con 1 vida",left+14,top+44,0xffdfdfd5);
        String status=problem.isEmpty()?(candidates.isEmpty()?"No hay eliminados autenticados cerca.":"Elige un alma · Canalización: 30 segundos"):problem;
        d.drawTextWithShadow(textRenderer,textRenderer.trimToWidth(status,w-28),left+14,top+61,problem.isEmpty()?0xff9dcac1:0xffffa274);
        if(candidates.isEmpty())d.drawCenteredTextWithShadow(textRenderer,Text.literal("Acerca al eliminado al núcleo."),left+w/2,top+88,0xff7f969e);
        d.drawTextWithShadow(textRenderer,"Distancia máxima: 4 bloques",left+66,top+h-18,0xff81999c);
        super.render(d,mx,my,delta);
    }
    @Override public boolean shouldPause(){return false;}
}
