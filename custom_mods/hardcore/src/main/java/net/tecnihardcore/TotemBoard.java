package net.tecnihardcore;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.networking.v1.*;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.*;
import net.minecraft.block.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.*;
import static net.minecraft.server.command.CommandManager.*;
public final class TotemBoard {
    public static final Identifier OPEN=Hardcore.id("board_v1");
    public static final BoardBlock BLOCK=Registry.register(Registries.BLOCK,Hardcore.id("tablon_reliquias"),new BoardBlock());
    public static final Block PANEL=Registry.register(Registries.BLOCK,Hardcore.id("tablon_panel"),new PanelBlock());
    public static final BlockEntityType<BoardEntity> ENTITY=Registry.register(Registries.BLOCK_ENTITY_TYPE,Hardcore.id("tablon_reliquias"),FabricBlockEntityTypeBuilder.create(BoardEntity::new,BLOCK).build());
    public static void init(){
        CommandRegistrationCallback.EVENT.register((d,a,e)->d.register(literal("tecni").then(literal("tablon").requires(s->s.hasPermissionLevel(2)).then(literal("crear").then(argument("pos",net.minecraft.command.argument.BlockPosArgumentType.blockPos()).executes(c->{
            var p=net.minecraft.command.argument.BlockPosArgumentType.getLoadedBlockPos(c,"pos");var w=c.getSource().getWorld();
            for(int x=-1;x<=1;x++)for(int y=0;y<3;y++)if(!w.getBlockState(p.add(x,y,0)).isAir()){c.getSource().sendError(Text.literal("El tablón necesita tres bloques de ancho y tres de alto libres."));return 0;}
            if(!w.getBlockState(p.down()).isSolidBlock(w,p.down())){c.getSource().sendError(Text.literal("Falta suelo sólido."));return 0;}
            w.setBlockState(p,BLOCK.getDefaultState());for(int x=-1;x<=1;x++)for(int y=0;y<=1;y++)if(x!=0||y!=0)w.setBlockState(p.add(x,y,0),PANEL.getDefaultState());return 1;
        }))))));
    }
    public static final class BoardEntity extends BlockEntity {public BoardEntity(BlockPos p,BlockState s){super(ENTITY,p,s);}}
    public static final class BoardBlock extends BlockWithEntity {
        BoardBlock(){super(AbstractBlock.Settings.create().strength(-1,3600000).nonOpaque().pistonBehavior(net.minecraft.block.piston.PistonBehavior.BLOCK));}
        @Override public BlockEntity createBlockEntity(BlockPos p,BlockState s){return new BoardEntity(p,s);}
        @Override public BlockRenderType getRenderType(BlockState s){return BlockRenderType.MODEL;}
        @Override public VoxelShape getOutlineShape(BlockState s,BlockView w,BlockPos p,ShapeContext c){return Block.createCuboidShape(0,0,6,16,16,10);}
        @Override public ActionResult onUse(BlockState s,World w,BlockPos p,PlayerEntity player,Hand h,BlockHitResult hit){
            if(player instanceof ServerPlayerEntity sp&&AuthBootstrap.authenticated(sp)&&ServerPlayNetworking.canSend(sp,OPEN))ServerPlayNetworking.send(sp,OPEN,PacketByteBufs.create());
            return ActionResult.SUCCESS;
        }
        @Override public void onStateReplaced(BlockState s,World w,BlockPos p,BlockState next,boolean moved){if(!w.isClient&&!next.isOf(this))for(int x=-1;x<=1;x++)for(int y=0;y<=1;y++){var q=p.add(x,y,0);if(w.getBlockState(q).isOf(PANEL))w.removeBlock(q,false);}super.onStateReplaced(s,w,p,next,moved);}
    }
    private static final class PanelBlock extends Block {
        PanelBlock(){super(AbstractBlock.Settings.create().strength(-1,3600000).nonOpaque().pistonBehavior(net.minecraft.block.piston.PistonBehavior.BLOCK));}
        @Override public BlockRenderType getRenderType(BlockState s){return BlockRenderType.INVISIBLE;}
        @Override public VoxelShape getOutlineShape(BlockState s,BlockView w,BlockPos p,ShapeContext c){return Block.createCuboidShape(0,0,6,16,16,10);}
        @Override public ActionResult onUse(BlockState s,World w,BlockPos p,PlayerEntity player,Hand h,BlockHitResult hit){if(player instanceof ServerPlayerEntity sp&&AuthBootstrap.authenticated(sp)&&ServerPlayNetworking.canSend(sp,OPEN))ServerPlayNetworking.send(sp,OPEN,PacketByteBufs.create());return ActionResult.SUCCESS;}
    }
}
