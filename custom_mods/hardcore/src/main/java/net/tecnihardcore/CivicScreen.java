package net.tecnihardcore;
import com.google.gson.*;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.*;
import net.minecraft.text.Text;
import net.minecraft.item.ItemStack;
import java.util.*;

public final class CivicScreen extends Screen {
    private final JsonObject data;private final String kind;private int left,top,w,h,rows,page,bodyOffset;
    private TextFieldWidget price,name,node,text,label,next,action,remove;
    private record Entry(String id,String label,String action,ItemStack item){}
    private final List<Entry> entries=new ArrayList<>();
    public CivicScreen(JsonObject json){super(Text.literal(json.get("title").getAsString()));data=json;kind=json.get("kind").getAsString();for(var raw:json.getAsJsonArray("entries")){var e=raw.getAsJsonObject();ItemStack stack=ItemStack.EMPTY;if(e.has("item"))try{stack=ItemStack.fromNbt(net.minecraft.nbt.StringNbtReader.parse(e.get("item").getAsString()));}catch(Exception ignored){}entries.add(new Entry(e.get("id").getAsString(),e.get("label").getAsString(),e.get("action").getAsString(),stack));}}
    public static void initNetwork(){CivicNpcRenderer.init();net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(CivicNpcEntity.TYPE,CivicNpcRenderer::new);ClientPlayNetworking.registerGlobalReceiver(CivicNetwork.OPEN,(c,h,b,r)->{String json=b.readString(32767);c.execute(()->{try{c.setScreen(new CivicScreen(JsonParser.parseString(json).getAsJsonObject()));}catch(Exception e){Hardcore.LOG.warn("Invalid civic screen");}});});}
    @Override protected void init(){w=Math.min(420,width-16);h=Math.min(kind.equals("editor")?230:kind.equals("npc")?260:340,height-16);left=(width-w)/2;top=(height-h)/2;rows=Math.max(1,(h-(kind.equals("market")?135:115))/28);page=Math.min(page,Math.max(0,(entries.size()-1)/rows));
        if(kind.equals("editor")){int half=(w-34)/2;name=field(left+12,top+37,half,18,data.get("name").getAsString(),64,"Nombre");node=field(left+22+half,top+37,half,18,data.get("node").getAsString(),32,"Nodo");text=field(left+12,top+59,w-24,18,data.get("text").getAsString(),2048,"Texto del diálogo");label=field(left+12,top+81,half,18,"",80,"Nueva opción");next=field(left+22+half,top+81,half,18,"",32,"Nodo de destino");action=field(left+12,top+103,w-90,18,"",80,"Acción (opcional)");remove=field(left+w-70,top+103,58,18,"",2,"Borrar #");button("Guardar",left+12,top+h-27,85,()->{var request=new JsonObject();request.addProperty("name",name.getText());request.addProperty("node",node.getText());request.addProperty("text",text.getText());request.addProperty("label",label.getText());request.addProperty("next",next.getText());request.addProperty("action",action.getText());int index=-1;try{index=Integer.parseInt(remove.getText())-1;}catch(Exception ignored){}request.addProperty("remove",index);send("edit","",request.toString());});}
        else{
            int first=kind.equals("market")?top+82:top+78;
            for(int i=0;i<rows&&page*rows+i<entries.size();i++){var e=entries.get(page*rows+i);if(kind.equals("market"))button(e.action.equals("buy")?"Comprar":e.action.equals("cancel")?"Cancelar":"Retirar",left+w-80,first+i*28,68,()->send(e.action,e.id,""));else button(textRenderer.trimToWidth(e.label,w-40),left+12,first+i*28,w-24,()->send(e.action,e.id,""));}
            if(kind.equals("market")){button("Ofertas",left+12,top+53,72,()->send("tab","all",""));button("Mis ventas",left+88,top+53,88,()->send("tab","mine",""));button("Buzón",left+180,top+53,65,()->send("tab","mail",""));price=field(left+12,top+h-53,85,18,"",7,"Precio CT");button("Publicar lote en mano",left+103,top+h-53,w-115,()->send("publish","",price.getText()));button("‹",left+12,top+h-27,22,()->{if(page>0){page--;clearChildren();init();}else send("page","-1","");});button("›",left+38,top+h-27,22,()->{if((page+1)*rows<entries.size()){page++;clearChildren();init();}else send("page","1","");});}
            else if(entries.size()>rows){button("‹",left+12,top+h-27,22,()->{page=Math.max(0,page-1);clearChildren();init();});button("›",left+38,top+h-27,22,()->{page=Math.min((entries.size()-1)/rows,page+1);clearChildren();init();});}
        }
        button("Cerrar",left+w-74,top+h-27,62,this::close);
    }
    private TextFieldWidget field(int x,int y,int width,int height,String initial,int max,String hint){var field=new TextFieldWidget(textRenderer,x,y,width,height,Text.literal(hint));field.setMaxLength(max);field.setText(initial);field.setPlaceholder(Text.literal(hint));addDrawableChild(field);return field;}
    private void button(String label,int x,int y,int width,Runnable action){addDrawableChild(ButtonWidget.builder(Text.literal(label),b->action.run()).dimensions(x,y,width,20).build());}
    private void send(String action,String id,String value){var b=PacketByteBufs.create();b.writeUuid(UUID.fromString(data.get("nonce").getAsString()));b.writeString(action,24);b.writeString(id,64);b.writeString(value,4096);ClientPlayNetworking.send(CivicNetwork.ACTION,b);}
    @Override public void render(DrawContext d,int mx,int my,float delta){renderBackground(d);d.fill(left-1,top-1,left+w+1,top+h+1,0xffa9894a);d.fillGradient(left,top,left+w,top+h,0xf0192a30,0xf008121a);d.drawCenteredTextWithShadow(textRenderer,title,left+w/2,top+12,0xffedcf85);
        if(kind.equals("editor")){d.drawTextWithShadow(textRenderer,textRenderer.trimToWidth(data.get("body").getAsString(),w-24),left+12,top+25,0xff8dc4af);int y=top+128;for(var entry:entries){if(y>top+h-40)break;d.drawTextWithShadow(textRenderer,textRenderer.trimToWidth(entry.label,w-24),left+12,y,0xff9caaa8);y+=10;}}
        else if(kind.equals("market")){d.drawTextWithShadow(textRenderer,textRenderer.trimToWidth(data.get("body").getAsString(),w-24),left+12,top+30,0xff9cd5b7);for(int i=0;i<rows&&page*rows+i<entries.size();i++){var entry=entries.get(page*rows+i);int y=top+82+i*28;d.drawItem(entry.item,left+12,y);d.drawTextWithShadow(textRenderer,textRenderer.trimToWidth(entry.item.getName().getString()+" ×"+entry.item.getCount(),w-125),left+34,y,0xffe3e2d9);d.drawTextWithShadow(textRenderer,textRenderer.trimToWidth(entry.label,w-125),left+34,y+11,0xffb4bbac);if(mx>=left+12&&mx<left+32&&my>=y&&my<y+20)d.drawItemTooltip(textRenderer,entry.item,mx,my);}d.drawTextWithShadow(textRenderer,"Página "+data.get("page").getAsInt()+"/"+data.get("pages").getAsInt()+(entries.size()>rows?" · "+(page+1)+"/"+((entries.size()+rows-1)/rows):""),left+70,top+h-22,0xff7f999a);}
        else{var lines=textRenderer.wrapLines(Text.literal(data.get("body").getAsString()),w-24);bodyOffset=Math.min(bodyOffset,Math.max(0,lines.size()-4));for(int i=0;i<4&&bodyOffset+i<lines.size();i++)d.drawTextWithShadow(textRenderer,lines.get(bodyOffset+i),left+12,top+29+i*10,0xffc6d4ce);if(lines.size()>4)d.drawTextWithShadow(textRenderer,"Rueda: leer más",left+w-105,top+h-39,0xff81999b);}
        if(kind.equals("market")&&entries.isEmpty())d.drawTextWithShadow(textRenderer,data.get("tab").getAsString().equals("mail")?"Tu buzón está vacío.":"No hay ofertas en esta pestaña.",left+12,top+85,0xffb4bbac);
        super.render(d,mx,my,delta);
    }
    @Override public boolean mouseScrolled(double x,double y,double amount){if(kind.equals("npc")){bodyOffset=Math.max(0,bodyOffset-(int)Math.signum(amount));return true;}return super.mouseScrolled(x,y,amount);}
    @Override public boolean shouldPause(){return false;}
}
