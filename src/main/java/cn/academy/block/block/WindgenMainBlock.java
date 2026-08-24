package cn.academy.block.block;

import cn.academy.block.tileentity.WindgenMainBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public class WindgenMainBlock extends ACMultiBlock {

    public static final IntegerProperty SUB = IntegerProperty.create("sub", 0, 2);

    private static final int[][] SUBS = {
            {0, 0, 0},
            {0, 0, -1},
            {0, 0, 1},
    };

    public WindgenMainBlock(Properties p) {
        super(p);
    }

    @Override
    protected IntegerProperty subProperty() {
        return SUB;
    }

    @Override
    protected int[][] subs() {
        return SUBS;
    }

    @Override
    protected RenderShape originRenderShape() {
        return RenderShape.MODEL;
    }

    @Override
    protected RenderShape subRenderShape() {
        return RenderShape.MODEL;
    }

    @Override
    protected BlockEntity createOriginBlockEntity(BlockPos pos, BlockState state) {
        return new WindgenMainBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            if (ACMultiBlock.getOriginTile(level, pos) instanceof WindgenMainBlockEntity be
                    && player instanceof ServerPlayer sp) {
                NetworkHooks.openScreen(sp, be, buf -> buf.writeBlockPos(be.getBlockPos()));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {

        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof WindgenMainBlockEntity be) {
            be.dropContents();
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || subOf(state) != 0) {
            return null;
        }
        return (BlockEntityTicker<T>) (BlockEntityTicker<WindgenMainBlockEntity>)
                (lvl, pos, st, be) -> be.serverTick();
    }

    public static float drMapOf(Direction dir) {
        return switch (dir) {
            case NORTH -> 180f;
            case SOUTH -> 0f;
            case WEST -> -90f;
            case EAST -> 90f;
            default -> 0f;
        };
    }
}
