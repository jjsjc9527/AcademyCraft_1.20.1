package cn.academy.item;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class MatterUnitItem extends Item {

    public static final class MatterMaterial {
        public final String name;

        private final Supplier<Block> block;
        int id;

        MatterMaterial(String name, Supplier<Block> block) {
            this.name = name;
            this.block = block;
        }

        public Block getBlock() {
            return block.get();
        }

        public int getId() {
            return id;
        }
    }

    private static final List<MatterMaterial> MATERIALS = new ArrayList<>();

    public static final MatterMaterial MAT_NONE = new MatterMaterial("none", () -> Blocks.AIR);
    public static final MatterMaterial MAT_PHASE_LIQUID =
            new MatterMaterial("phase_liquid", () -> cn.academy.ACBlocks.IMAG_PHASE.get());

    static {

        addMatterMaterial(MAT_NONE);
        addMatterMaterial(MAT_PHASE_LIQUID);
    }

    private static void addMatterMaterial(MatterMaterial mat) {
        for (MatterMaterial prev : MATERIALS) {
            if (prev.name.equals(mat.name)) {
                throw new RuntimeException("Duplicate MatterMaterial Key " + mat.name);
            }
        }
        mat.id = MATERIALS.size();
        MATERIALS.add(mat);
    }

    public static MatterMaterial getMatterMaterial(String name) {
        for (MatterMaterial mat : MATERIALS) {
            if (mat.name.equals(name)) return mat;
        }
        return null;
    }

    private static final String TAG_MATERIAL = "material";

    public MatterUnitItem() {
        super(new Item.Properties().stacksTo(64));
    }

    public MatterMaterial getMaterial(ItemStack stack) {
        if (stack.getItem() != this) return null;
        CompoundTag tag = stack.getTag();
        int id = tag == null ? 0 : tag.getInt(TAG_MATERIAL);
        return id >= 0 && id < MATERIALS.size() ? MATERIALS.get(id) : MAT_NONE;
    }

    public void setMaterial(ItemStack stack, MatterMaterial mat) {
        stack.getOrCreateTag().putInt(TAG_MATERIAL, mat.id);
    }

    public ItemStack create(MatterMaterial mat) {
        ItemStack ret = new ItemStack(this);
        setMaterial(ret, mat);
        return ret;
    }

    public boolean is(ItemStack stack, MatterMaterial mat) {
        return stack.getItem() == this && getMaterial(stack) == mat;
    }

    @Override
    public Component getName(ItemStack stack) {
        MatterMaterial mat = getMaterial(stack);
        return Component.translatable(getDescriptionId() + "_" + (mat == null ? "none" : mat.name));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        MatterMaterial mat = getMaterial(stack);
        boolean isNone = mat == MAT_NONE;

        BlockHitResult hit = getPlayerPOVHitResult(level, player,
                isNone ? ClipContext.Fluid.SOURCE_ONLY : ClipContext.Fluid.NONE);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(stack);
        }

        BlockPos pos = hit.getBlockPos();
        if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, hit.getDirection(), stack)) {
            return InteractionResultHolder.pass(stack);
        }

        if (isNone) {

            Block b = level.getBlockState(pos).getBlock();
            for (MatterMaterial m : MATERIALS) {
                if (m != MAT_NONE && m.getBlock() == b) {
                    if (!level.isClientSide) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                        giveOrDrop(player, create(m));
                        shrinkUnlessCreative(player, stack);
                        if (m == MAT_PHASE_LIQUID) {

                            cn.academy.advancements.ACAdvancements.trigger(
                                    player, cn.academy.advancements.ACAdvancements.GETTING_PHASE);
                        }
                    }
                    return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
                }
            }
            return InteractionResultHolder.pass(stack);
        }

        if (!level.isClientSide) {
            BlockPos target = level.getBlockState(pos).canBeReplaced()
                    ? pos : pos.relative(hit.getDirection());
            level.setBlock(target, mat.getBlock().defaultBlockState(), 3);
            giveOrDrop(player, create(MAT_NONE));
            shrinkUnlessCreative(player, stack);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private static void giveOrDrop(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private static void shrinkUnlessCreative(Player player, ItemStack stack) {
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }

    public static void registerProperties(Item item) {
        net.minecraft.client.renderer.item.ItemProperties.register(item,
                new ResourceLocation("material"),
                (stack, level, entity, seed) -> {
                    CompoundTag tag = stack.getTag();
                    return tag == null ? 0f : tag.getInt(TAG_MATERIAL);
                });
        net.minecraft.client.renderer.item.ItemProperties.register(item,
                new ResourceLocation("frame"),
                (stack, level, entity, seed) ->
                        (int) (cn.lambdalib2.util.GameTimer.getTime() * 4) % 4);
    }
}
