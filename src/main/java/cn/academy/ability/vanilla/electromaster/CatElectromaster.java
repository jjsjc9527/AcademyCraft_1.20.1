package cn.academy.ability.vanilla.electromaster;

import cn.academy.ability.Category;
import cn.academy.ability.CategoryManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Set;

public final class CatElectromaster {

    public static final Category CATEGORY = new Category("electromaster");

    private CatElectromaster() {}

    private static final Set<Block> NORMAL_METAL = Set.of(
            Blocks.RAIL, Blocks.ACTIVATOR_RAIL, Blocks.DETECTOR_RAIL, Blocks.POWERED_RAIL,
            Blocks.IRON_BARS, Blocks.IRON_BLOCK, Blocks.STICKY_PISTON, Blocks.PISTON);

    private static final Set<Block> WEAK_METAL = Set.of(
            Blocks.DISPENSER, Blocks.HOPPER, Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE);

    private static final Set<EntityType<?>> METAL_ENTITIES = Set.of(
            EntityType.MINECART, EntityType.CHEST_MINECART, EntityType.FURNACE_MINECART,
            EntityType.TNT_MINECART, EntityType.HOPPER_MINECART, EntityType.SPAWNER_MINECART,
            EntityType.COMMAND_BLOCK_MINECART, EntityType.IRON_GOLEM);

    public static boolean isNormalMetalBlock(Block block) { return NORMAL_METAL.contains(block); }
    public static boolean isWeakMetalBlock(Block block) { return WEAK_METAL.contains(block); }
    public static boolean isMetalBlock(Block block) { return isNormalMetalBlock(block) || isWeakMetalBlock(block); }
    public static boolean isEntityMetallic(Entity e) { return METAL_ENTITIES.contains(e.getType()); }

    public static void register() {

        CATEGORY.setColorStyle(20, 113, 208, 100);

        ElectricArc.INSTANCE.setPosition(24, 46);
        MagMovement.INSTANCE.setPosition(137, 35);
        BodyIntensify.INSTANCE.setPosition(97, 15);
        ThunderBolt.INSTANCE.setPosition(39, 72);
        Railgun.INSTANCE.setPosition(116, 65);
        ThunderClap.INSTANCE.setPosition(170, 73);

        CATEGORY.addSkill(ElectricArc.INSTANCE);
        CATEGORY.addSkill(MagMovement.INSTANCE);
        CATEGORY.addSkill(BodyIntensify.INSTANCE);
        CATEGORY.addSkill(ThunderBolt.INSTANCE);
        CATEGORY.addSkill(Railgun.INSTANCE);
        CATEGORY.addSkill(ThunderClap.INSTANCE);

        MagMovement.INSTANCE.setParent(ElectricArc.INSTANCE);
        BodyIntensify.INSTANCE.setParent(ElectricArc.INSTANCE, 1f);
        ThunderBolt.INSTANCE.setParent(ElectricArc.INSTANCE);

        Railgun.INSTANCE.setParent(ElectricArc.INSTANCE, 0.3f);
        ThunderClap.INSTANCE.setParent(Railgun.INSTANCE, 0.3f);

        cn.academy.ability.vanilla.VanillaCategories.addGenericSkills(CATEGORY);

        MagFieldControl.INSTANCE.setPosition(204, 33);
        CATEGORY.addSkill(MagFieldControl.INSTANCE);

        MagFieldControl.INSTANCE.setParent(MagMovement.INSTANCE, 0.5f);

        CurrentCharging.INSTANCE.setPosition(20, 8);
        CATEGORY.addSkill(CurrentCharging.INSTANCE);
        CurrentCharging.INSTANCE.setParent(ElectricArc.INSTANCE, 0.3f);

        IronSandControl.INSTANCE.setPosition(86, 100);

        CATEGORY.addSkill(IronSandControl.INSTANCE);
        IronSandControl.INSTANCE.setParent(ThunderBolt.INSTANCE, 0.8f);
        IronSandControl.init();

        CategoryManager.INSTANCE.register(CATEGORY);

        cn.academy.ability.vanilla.VanillaCategories.addLateGenericSkills(CATEGORY);
    }
}
