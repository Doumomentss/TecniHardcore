package net.tecnihardcore;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.biome.v1.*;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.*;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.*;
import net.minecraft.world.biome.BiomeKeys;
import java.util.*;

public final class Sanctuaries {
    public static final SanctuaryBlock CORE=Registry.register(Registries.BLOCK,Hardcore.id("santuario"),new SanctuaryBlock());
    public static final SanctuaryCollision COLLISION=Registry.register(Registries.BLOCK,Hardcore.id("santuario_soporte"),new SanctuaryCollision());
    public static final BlockEntityType<SanctuaryEntity> ENTITY=Registry.register(Registries.BLOCK_ENTITY_TYPE,Hardcore.id("santuario"),FabricBlockEntityTypeBuilder.create(SanctuaryEntity::new,CORE).build());
    public static final Feature<DefaultFeatureConfig> FEATURE=Registry.register(Registries.FEATURE,Hardcore.id("sanctuary"),new SanctuaryFeature());
    public static void init() {
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld().and(c->!c.getBiomeKey().getValue().getPath().contains("ocean")&&!c.getBiomeKey().equals(BiomeKeys.RIVER)&&!c.getBiomeKey().equals(BiomeKeys.FROZEN_RIVER)),GenerationStep.Feature.SURFACE_STRUCTURES,RegistryKey.of(RegistryKeys.PLACED_FEATURE,Hardcore.id("sanctuary")));
        ServerChunkEvents.CHUNK_LOAD.register((world,chunk)->{if(Hardcore.souls!=null)for(var e:chunk.getBlockEntities().values())if(e instanceof SanctuaryEntity){remember(world,e.getPos());BlockPos p=e.getPos();world.getServer().execute(()->{if(world.getBlockState(p).isOf(CORE))SanctuaryCollision.install(world,p);});world.getChunkManager().getLightingProvider().checkBlock(e.getPos());}});
    }
    public static String key(World world,BlockPos p) {return world.getRegistryKey().getValue()+"|"+p.getX()+"|"+p.getY()+"|"+p.getZ();}
    public static void remember(World w,BlockPos p) {if(Hardcore.souls!=null && Hardcore.souls.data.sanctuaries.add(key(w,p)))Hardcore.souls.save();}
    public static void forget(World w,BlockPos p) {if(Hardcore.souls!=null && Hardcore.souls.data.sanctuaries.remove(key(w,p)))Hardcore.souls.save();}
    public static void repairKnown(net.minecraft.server.MinecraftServer server){
        for(String key:new ArrayList<>(Hardcore.souls.data.sanctuaries)){
            String[] parts=key.split("\\|");if(parts.length!=4)continue;
            var world=server.getWorld(RegistryKey.of(RegistryKeys.WORLD,new net.minecraft.util.Identifier(parts[0])));if(world==null)continue;
            var p=new BlockPos(Integer.parseInt(parts[1]),Integer.parseInt(parts[2]),Integer.parseInt(parts[3]));
            if(world.getChunkManager().isChunkLoaded(p.getX()>>4,p.getZ()>>4)&&world.getBlockState(p).isOf(CORE))SanctuaryCollision.install(world,p);
        }
    }
    private static boolean ground(BlockState s) {return s.isIn(BlockTags.DIRT)||s.isOf(Blocks.STONE)||s.isOf(Blocks.SAND)||s.isOf(Blocks.GRAVEL)||s.isOf(Blocks.DEEPSLATE)||s.isOf(Blocks.SNOW_BLOCK);}
    private static boolean empty(BlockState s) {return s.isAir()||s.isOf(Blocks.GRASS)||s.isOf(Blocks.TALL_GRASS)||s.isOf(Blocks.FERN)||s.isOf(Blocks.SNOW)||s.isIn(BlockTags.FLOWERS);}
    /** Core position is one block above the platform. Never replace player blocks or containers. */
    public static String problem(WorldAccess w,BlockPos core) {
        if(core.getY()<w.getBottomY()+2||core.getY()+14>=w.getTopY())return "Altura fuera del mundo.";
        for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++) {
            BlockPos floor=core.add(x,-1,z);
            boolean supported=false;
            for(int depth=0;depth<4;depth++) {
                BlockPos support=floor.down(depth);BlockState state=w.getBlockState(support);
                if(w.getBlockEntity(support)!=null)return "Hay una construcción debajo del santuario.";
                if(ground(state)){supported=true;break;}
                if(!empty(state))return "Necesita terreno natural seco de 11×11; no se sustituirán construcciones.";
            }
            if(!supported)return "Terreno demasiado irregular: máximo 3 bloques de desnivel.";
            for(int y=0;y<=13;y++) {
                BlockPos p=core.add(x,y,z);if(!empty(w.getBlockState(p))||w.getBlockEntity(p)!=null)return "Hay bloques ocupados en el espacio del santuario (11×11×14).";
            }
        }
        return null;
    }
    public static boolean place(WorldAccess w,BlockPos core) {
        if(problem(w,core)!=null)return false;
        for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++) {
            int a=Math.abs(x),b=Math.abs(z);
            Block block=a==5||b==5?Blocks.POLISHED_BLACKSTONE_BRICKS:(a+b)%4==0?Blocks.CHISELED_POLISHED_BLACKSTONE:Blocks.DEEPSLATE_TILES;
            if((a==3&&b==3)||(a==0&&b==4)||(b==0&&a==4))block=Blocks.GILDED_BLACKSTONE;
            for(int depth=1;depth<4;depth++) {BlockPos support=core.add(x,-1-depth,z);if(ground(w.getBlockState(support)))break;w.setBlockState(support,Blocks.DEEPSLATE_BRICKS.getDefaultState(),3);}
            w.setBlockState(core.add(x,-1,z),block.getDefaultState(),3);
            for(int y=0;y<=13;y++)if(!w.getBlockState(core.add(x,y,z)).isAir())w.setBlockState(core.add(x,y,z),Blocks.AIR.getDefaultState(),3);
        }
        for(int x:new int[]{-4,4})for(int z:new int[]{-4,4}) {
            for(int y=0;y<4;y++)w.setBlockState(core.add(x,y,z),(y==0?Blocks.CHISELED_POLISHED_BLACKSTONE:y==3?Blocks.GILDED_BLACKSTONE:Blocks.POLISHED_BLACKSTONE_BRICKS).getDefaultState(),3);
            w.setBlockState(core.add(x,4,z),Blocks.SOUL_LANTERN.getDefaultState(),3);
        }
        w.setBlockState(core,CORE.getDefaultState(),3);
        if(w instanceof World world){remember(world,core);SanctuaryCollision.install(world,core);}
        return true;
    }
    public static BlockPos nearby(ServerPlayerEntity p) {
        for(BlockPos b:BlockPos.iterate(p.getBlockPos().add(-4,-3,-4),p.getBlockPos().add(4,3,4)))if(p.getWorld().getBlockState(b).isOf(CORE)&&Rituals.near(p,b))return b.toImmutable();return null;
    }
    public static BlockPos nearest(ServerWorld world,BlockPos origin) {
        return Hardcore.souls.data.sanctuaries.stream().filter(k->k.startsWith(world.getRegistryKey().getValue()+"|"))
            .map(k->{String[] s=k.split("\\|");return new BlockPos(Integer.parseInt(s[1]),Integer.parseInt(s[2]),Integer.parseInt(s[3]));}).min(Comparator.comparingDouble(p->p.getSquaredDistance(origin))).orElse(null);
    }
    private static final class SanctuaryFeature extends Feature<DefaultFeatureConfig> {
        SanctuaryFeature(){super(DefaultFeatureConfig.CODEC);}
        @Override public boolean generate(net.minecraft.world.gen.feature.util.FeatureContext<DefaultFeatureConfig> c) {
            int cx=c.getOrigin().getX()>>4,cz=c.getOrigin().getZ()>>4,rx=Math.floorDiv(cx,96),rz=Math.floorDiv(cz,96);
            var random=new java.util.Random(c.getWorld().getSeed() ^ ((long)rx*341873128712L) ^ ((long)rz*132897987541L) ^ 0x534f554cL);
            if(cx!=rx*96+random.nextInt(64)||cz!=rz*96+random.nextInt(64))return false;
            int x=(cx<<4)+8,z=(cz<<4)+8,y=Integer.MIN_VALUE;
            for(int dx=-5;dx<=5;dx++)for(int dz=-5;dz<=5;dz++) {
                int height=c.getWorld().getTopY(Heightmap.Type.WORLD_SURFACE_WG,x+dx,z+dz);
                while(height>c.getWorld().getBottomY()&&empty(c.getWorld().getBlockState(new BlockPos(x+dx,height-1,z+dz))))height--;
                y=Math.max(y,height);
            }
            return place(c.getWorld(),new BlockPos(x,y,z));
        }
    }
}
