package cn.academy;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ACBlocks {

    public static final DeferredRegister<Block> REGISTER =
            DeferredRegister.create(ForgeRegistries.BLOCKS, AcademyCraft.MODID);

    public static final RegistryObject<Block> MACHINE_FRAME = REGISTER.register("machine_frame",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(1.5F, 6.0F)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> CONSTRAINT_METAL = REGISTER.register("constraint_metal",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(4.0F, 3.0F)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> CRYSTAL_ORE = REGISTER.register("crystal_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.0F, 3.0F)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> IMAGSIL_ORE = REGISTER.register("imagsil_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.75F, 3.0F)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> RESO_ORE = REGISTER.register("reso_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.0F, 3.0F)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> DEEPSLATE_CONSTRAINT_METAL = REGISTER.register("deepslate_constraint_metal",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .strength(4.5F, 3.0F)
                    .sound(SoundType.DEEPSLATE)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> DEEPSLATE_CRYSTAL_ORE = REGISTER.register("deepslate_crystal_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .strength(4.5F, 3.0F)
                    .sound(SoundType.DEEPSLATE)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> DEEPSLATE_IMAGSIL_ORE = REGISTER.register("deepslate_imagsil_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .strength(4.5F, 3.0F)
                    .sound(SoundType.DEEPSLATE)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> DEEPSLATE_RESO_ORE = REGISTER.register("deepslate_reso_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .strength(4.5F, 3.0F)
                    .sound(SoundType.DEEPSLATE)
                    .requiresCorrectToolForDrops()));

    private static BlockBehaviour.Properties machineProps(MapColor color, float hardness) {
        return BlockBehaviour.Properties.of().mapColor(color)
                .strength(hardness, hardness).requiresCorrectToolForDrops();
    }

    public static final RegistryObject<Block> NODE_BASIC = REGISTER.register("node_basic",
            () -> new cn.academy.block.block.WirelessNodeBlock(
                    cn.academy.block.block.NodeType.BASIC, machineProps(MapColor.METAL, 2.5F)));

    public static final RegistryObject<Block> NODE_STANDARD = REGISTER.register("node_standard",
            () -> new cn.academy.block.block.WirelessNodeBlock(
                    cn.academy.block.block.NodeType.STANDARD, machineProps(MapColor.METAL, 2.5F)));

    public static final RegistryObject<Block> NODE_ADVANCED = REGISTER.register("node_advanced",
            () -> new cn.academy.block.block.WirelessNodeBlock(
                    cn.academy.block.block.NodeType.ADVANCED, machineProps(MapColor.METAL, 2.5F)));

    public static final RegistryObject<cn.academy.block.block.ImagPhaseBlock> IMAG_PHASE =
            REGISTER.register("imag_phase",
                    () -> new cn.academy.block.block.ImagPhaseBlock(ACFluids.IMAGPROJ,
                            BlockBehaviour.Properties.copy(net.minecraft.world.level.block.Blocks.WATER)
                                    .lightLevel(s -> 8)));

    public static final RegistryObject<Block> IMAG_FUSOR = REGISTER.register("imag_fusor",
            () -> new cn.academy.block.block.ImagFusorBlock(
                    machineProps(MapColor.METAL, 3.0F)
                            .lightLevel(s -> s.getValue(cn.academy.block.block.ImagFusorBlock.WORKING) ? 6 : 0)));

    public static final RegistryObject<Block> METAL_FORMER = REGISTER.register("metal_former",
            () -> new cn.academy.block.block.MetalFormerBlock(machineProps(MapColor.METAL, 3.0F)));

    public static final RegistryObject<Block> ABILITY_INTERFERER = REGISTER.register("ability_interferer",
            () -> new cn.academy.block.block.AbilityInterfererBlock(machineProps(MapColor.STONE, 3.0F)));

    public static final RegistryObject<Block> DEV_NORMAL = REGISTER.register("dev_normal",
            () -> new cn.academy.block.block.DeveloperBlock(
                    cn.academy.ability.develop.DeveloperType.NORMAL,
                    machineProps(MapColor.METAL, 4.0F).noOcclusion()));

    public static final RegistryObject<Block> DEV_ADVANCED = REGISTER.register("dev_advanced",
            () -> new cn.academy.block.block.DeveloperBlock(
                    cn.academy.ability.develop.DeveloperType.ADVANCED,
                    machineProps(MapColor.METAL, 4.0F).noOcclusion()));

    public static final RegistryObject<Block> PHASE_GEN = REGISTER.register("phase_gen",
            () -> new cn.academy.block.block.PhaseGenBlock(machineProps(MapColor.COLOR_PURPLE, 2.5F).noOcclusion()));

    public static final RegistryObject<Block> WIRELESS_GENERATOR = REGISTER.register("wireless_generator",
            () -> new cn.academy.block.block.WirelessGeneratorBlock(machineProps(MapColor.COLOR_RED, 1.5F).noOcclusion()));

    public static final RegistryObject<Block> WIRELESS_RECEIVER = REGISTER.register("wireless_receiver",
            () -> new cn.academy.block.block.WirelessReceiverBlock(machineProps(MapColor.COLOR_BLUE, 2.0F)));

    public static final RegistryObject<Block> WIRELESS_MATRIX = REGISTER.register("matrix",
            () -> new cn.academy.block.block.WirelessMatrixBlock(
                    machineProps(MapColor.COLOR_PURPLE, 3.0F).noOcclusion().lightLevel(s -> 15)));

    public static final RegistryObject<Block> WINDGEN_BASE = REGISTER.register("windgen_base",
            () -> new cn.academy.block.block.WindgenBaseBlock(machineProps(MapColor.COLOR_LIGHT_GRAY, 4.0F).noOcclusion()));

    public static final RegistryObject<Block> WINDGEN_PILLAR = REGISTER.register("windgen_pillar",
            () -> new Block(machineProps(MapColor.COLOR_LIGHT_GRAY, 4.0F).noOcclusion()));

    public static final RegistryObject<Block> WINDGEN_MAIN = REGISTER.register("windgen_main",
            () -> new cn.academy.block.block.WindgenMainBlock(machineProps(MapColor.COLOR_LIGHT_GRAY, 4.0F).noOcclusion()));

    private ACBlocks() {}
}
