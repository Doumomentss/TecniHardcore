package net.tecnihardcore;
import com.google.gson.*;
import java.nio.file.*;
public final class PublicNews {
    private static long next;
    private static JsonArray current=defaults();
    private static JsonArray defaults(){var a=new JsonArray();for(String[] v:new String[][]{{"plaza","Plaza y santuario","El santuario está en el centro del spawn. La plaza está protegida; puedes usar sus mesas y el tablón."},{"reliquias","Conoce tus tótems","Brasa, Bastión y Eco tienen efectos y penalizaciones propios. Interactúa con el tablón para ver imágenes y recetas."}}){var o=new JsonObject();o.addProperty("id",v[0]);o.addProperty("title",v[1]);o.addProperty("body",v[2]);a.add(o);}return a;}
    public static JsonArray get(){
        if(Hardcore.server==null||System.currentTimeMillis()<next)return current;next=System.currentTimeMillis()+5000;
        Path p=Hardcore.server.getRunDirectory().toPath().resolve("config/tecnihardcore/public-news.json");
        try{if(!Files.exists(p)){Files.createDirectories(p.getParent());Files.writeString(p,new GsonBuilder().setPrettyPrinting().create().toJson(defaults()));}
            JsonArray source=JsonParser.parseString(Files.readString(p)).getAsJsonArray(),clean=new JsonArray();
            for(var e:source){if(clean.size()==5)break;var o=e.getAsJsonObject();String title=o.get("title").getAsString(),body=o.get("body").getAsString();if(title.length()>100||body.length()>600)continue;var v=new JsonObject();v.addProperty("title",title);v.addProperty("body",body);clean.add(v);}current=clean;
        }catch(Exception e){Hardcore.LOG.warn("No se pudieron cargar las novedades públicas: {}",e.getClass().getSimpleName());}
        return current;
    }
}
