package cn.academy.block.block;

import cn.academy.ability.develop.DeveloperType;
import cn.academy.block.tileentity.DeveloperBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class DeveloperBlock extends ACMultiBlock {

    public static final IntegerProperty SUB = IntegerProperty.create("sub", 0, 7);

    private static final int[][] SUBS = {
            {0, 0, 0},
            {0, 1, 0},
            {0, 0, 1}, {0, 1, 1}, {0, 2, 1},
            {0, 0, 2}, {0, 1, 2}, {0, 2, 2},
    };

    public static final int SUB_COUNT = SUBS.length;

    public final DeveloperType type;

    public DeveloperBlock(DeveloperType type, Properties p) {
        super(p);
        this.type = type;
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
    protected RenderShape originRenderShape() { return RenderShape.MODEL; }

    @Override
    protected RenderShape subRenderShape() { return RenderShape.MODEL; }

    @Nullable
    public static DeveloperBlockEntity getOriginTile(Level level, BlockPos pos) {
        return ACMultiBlock.getOriginTile(level, pos) instanceof DeveloperBlockEntity be ? be : null;
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && !player.isShiftKeyDown()) {
            DeveloperBlockEntity be = getOriginTile(level, pos);
            if (be != null && be.getUser() == null) {
                be.use(player);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected BlockEntity createOriginBlockEntity(BlockPos pos, BlockState state) {
        return new DeveloperBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> t) {
        if (level.isClientSide || state.getValue(SUB) != 0) return null;
        return (BlockEntityTicker<T>) (BlockEntityTicker<DeveloperBlockEntity>)
                (lvl, pos, st, be) -> be.serverTick();
    }
}
