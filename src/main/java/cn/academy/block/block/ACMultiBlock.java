package cn.academy.block.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jetbrains.annotations.Nullable;

public abstract class ACMultiBlock extends Block implements EntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    private static boolean breaking = false;

    protected ACMultiBlock(Properties p) {
        super(p);
        BlockState st = getStateDefinition().any().setValue(subProperty(), 0);
        if (hasFacing()) st = st.setValue(FACING, Direction.NORTH);
        registerDefaultState(st);
    }

    protected abstract IntegerProperty subProperty();

    protected abstract int[][] subs();

    protected boolean hasFacing() {
        return true;
    }

    protected RenderShape originRenderShape() {
        return RenderShape.INVISIBLE;
    }

    protected RenderShape subRenderShape() {
        return RenderShape.INVISIBLE;
    }

    protected abstract BlockEntity createOriginBlockEntity(BlockPos pos, BlockState state);

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        if (hasFacing()) builder.add(FACING);
        builder.add(subProperty());
    }

    public final Direction facingOf(BlockState st) {
        return hasFacing() ? st.getValue(FACING) : Direction.NORTH;
    }

    public final int subOf(BlockState st) {
        return st.getValue(subProperty());
    }

    @Override
    @SuppressWarnings("deprecation")
    public RenderShape getRenderShape(BlockState state) {
        return subOf(state) == 0 ? originRenderShape() : subRenderShape();
    }

    public static BlockPos rotate(int[] s, Direction dir) {
        return switch (dir) {
            case EAST -> new BlockPos(-s[2], s[1], s[0]);
            case WEST -> new BlockPos(s[2], s[1], -s[0]);
            case SOUTH -> new BlockPos(-s[0], s[1], -s[2]);
            default -> new BlockPos(s[0], s[1], s[2]);
        };
    }

    @Nullable
    public static BlockPos getOrigin(BlockState state, BlockPos pos) {
        if (!(state.getBlock() instanceof ACMultiBlock mb)) return null;
        return pos.subtract(rotate(mb.subs()[mb.subOf(state)], mb.facingOf(state)));
    }

    @Nullable
    public static BlockEntity getOriginTile(Level level, BlockPos pos) {
        BlockPos origin = getOrigin(level.getBlockState(pos), pos);
        return origin == null ? null : level.getBlockEntity(origin);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction dir = hasFacing() ? ctx.getHorizontalDirection().getOpposite() : Direction.NORTH;
        BlockPos origin = ctx.getClickedPos();
        Level level = ctx.getLevel();
        for (int[] s : subs()) {
            BlockPos p = origin.offset(rotate(s, dir));

            if (!p.equals(origin) && !level.getBlockState(p).canBeReplaced()) {
                return null;
            }
        }
        BlockState st = defaultBlockState().setValue(subProperty(), 0);
        return hasFacing() ? st.setValue(FACING, dir) : st;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide) return;
        Direction dir = facingOf(state);
        int[][] subs = subs();
        for (int i = 1; i < subs.length; i++) {
            BlockPos p = pos.offset(rotate(subs[i], dir));
            BlockState sub = defaultBlockState().setValue(subProperty(), i);
            if (hasFacing()) sub = sub.setValue(FACING, dir);
            level.setBlock(p, sub, 3);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !breaking && !level.isClientSide) {
            BlockPos origin = getOrigin(state, pos);
            if (origin != null) {
                Direction dir = facingOf(state);
                breaking = true;
                try {
                    for (int[] s : subs()) {
                        BlockPos p = origin.offset(rotate(s, dir));
                        if (p.equals(pos)) continue;
                        if (level.getBlockState(p).is(this)) {
                            level.setBlock(p, Blocks.AIR.defaultBlockState(), 35);
                        }
                    }
                } finally {
                    breaking = false;
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Nullable
    @Override
    public final BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return subOf(state) == 0 ? createOriginBlockEntity(pos, state) : null;
    }
}
