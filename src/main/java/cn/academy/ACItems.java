package cn.academy;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ACItems {

    public static final DeferredRegister<Item> REGISTER =
            DeferredRegister.create(ForgeRegistries.ITEMS, AcademyCraft.MODID);

    public static final RegistryObject<BlockItem> MACHINE_FRAME = REGISTER.register("machine_frame",
            () -> new BlockItem(ACBlocks.MACHINE_FRAME.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> CONSTRAINT_METAL = REGISTER.register("constraint_metal",
            () -> new BlockItem(ACBlocks.CONSTRAINT_METAL.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> CRYSTAL_ORE = REGISTER.register("crystal_ore",
            () -> new BlockItem(ACBlocks.CRYSTAL_ORE.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> IMAGSIL_ORE = REGISTER.register("imagsil_ore",
            () -> new BlockItem(ACBlocks.IMAGSIL_ORE.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> RESO_ORE = REGISTER.register("reso_ore",
            () -> new BlockItem(ACBlocks.RESO_ORE.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> DEEPSLATE_CONSTRAINT_METAL = REGISTER.register("deepslate_constraint_metal",
            () -> new BlockItem(ACBlocks.DEEPSLATE_CONSTRAINT_METAL.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> DEEPSLATE_CRYSTAL_ORE = REGISTER.register("deepslate_crystal_ore",
            () -> new BlockItem(ACBlocks.DEEPSLATE_CRYSTAL_ORE.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> DEEPSLATE_IMAGSIL_ORE = REGISTER.register("deepslate_imagsil_ore",
            () -> new BlockItem(ACBlocks.DEEPSLATE_IMAGSIL_ORE.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> DEEPSLATE_RESO_ORE = REGISTER.register("deepslate_reso_ore",
            () -> new BlockItem(ACBlocks.DEEPSLATE_RESO_ORE.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> NODE_BASIC = REGISTER.register("node_basic",
            () -> new BlockItem(ACBlocks.NODE_BASIC.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> NODE_STANDARD = REGISTER.register("node_standard",
            () -> new BlockItem(ACBlocks.NODE_STANDARD.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> NODE_ADVANCED = REGISTER.register("node_advanced",
            () -> new BlockItem(ACBlocks.NODE_ADVANCED.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> WIRELESS_GENERATOR = REGISTER.register("wireless_generator",
            () -> new BlockItem(ACBlocks.WIRELESS_GENERATOR.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> PHASE_GEN = REGISTER.register("phase_gen",
            () -> new BlockItem(ACBlocks.PHASE_GEN.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> IMAG_FUSOR = REGISTER.register("imag_fusor",
            () -> new BlockItem(ACBlocks.IMAG_FUSOR.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> METAL_FORMER = REGISTER.register("metal_former",
            () -> new BlockItem(ACBlocks.METAL_FORMER.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> ABILITY_INTERFERER = REGISTER.register("ability_interferer",
            () -> new BlockItem(ACBlocks.ABILITY_INTERFERER.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> DEV_NORMAL = REGISTER.register("dev_normal",
            () -> new BlockItem(ACBlocks.DEV_NORMAL.get(), new Item.Properties()));

    public static final RegistryObject<Item> COIN = REGISTER.register("coin",
            () -> new cn.academy.item.CoinItem(new Item.Properties()));

    public static final RegistryObject<Item> TUTORIAL = REGISTER.register("tutorial",
            () -> new cn.academy.item.ItemTutorial(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> LOGO = REGISTER.register("logo",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<BlockItem> DEV_ADVANCED = REGISTER.register("dev_advanced",
            () -> new BlockItem(ACBlocks.DEV_ADVANCED.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> IMAG_PHASE = REGISTER.register("imag_phase",
            () -> new BlockItem(ACBlocks.IMAG_PHASE.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> WIRELESS_RECEIVER = REGISTER.register("wireless_receiver",
            () -> new BlockItem(ACBlocks.WIRELESS_RECEIVER.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> WIRELESS_MATRIX = REGISTER.register("matrix",
            () -> new BlockItem(ACBlocks.WIRELESS_MATRIX.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> WINDGEN_BASE = REGISTER.register("windgen_base",
            () -> new BlockItem(ACBlocks.WINDGEN_BASE.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> WINDGEN_PILLAR = REGISTER.register("windgen_pillar",
            () -> new BlockItem(ACBlocks.WINDGEN_PILLAR.get(), new Item.Properties()));

    public static final RegistryObject<BlockItem> WINDGEN_MAIN = REGISTER.register("windgen_main",
            () -> new BlockItem(ACBlocks.WINDGEN_MAIN.get(), new Item.Properties()));

    public static final RegistryObject<Item> WINDGEN_FAN = REGISTER.register("windgen_fan",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CRYSTAL_LOW = REGISTER.register("crystal_low",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CRYSTAL_NORMAL = REGISTER.register("crystal_normal",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CRYSTAL_PURE = REGISTER.register("crystal_pure",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> RESO_CRYSTAL = REGISTER.register("reso_crystal",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CONSTRAINT_INGOT = REGISTER.register("constraint_ingot",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> IMAG_SILICON_INGOT = REGISTER.register("imag_silicon_ingot",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> WAFER = REGISTER.register("wafer",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> IMAG_SILICON_PIECE = REGISTER.register("imag_silicon_piece",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> REINFORCED_IRON_PLATE = REGISTER.register("reinforced_iron_plate",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> NEEDLE = REGISTER.register("needle",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> SILBARN = REGISTER.register("silbarn",
            () -> new cn.academy.item.SilbarnItem(new Item.Properties()));

    public static final RegistryObject<Item> REMOTE_CONTROL = REGISTER.register("remote_control",
            () -> new cn.academy.item.RemoteControlItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> DATA_CHIP = REGISTER.register("data_chip",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CALC_CHIP = REGISTER.register("calc_chip",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> ENERGY_CONVERT_COMPONENT = REGISTER.register("energy_convert_component",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> BRAIN_COMPONENT = REGISTER.register("brain_component",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> INFO_COMPONENT = REGISTER.register("info_component",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> RESONANCE_COMPONENT = REGISTER.register("resonance_component",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<cn.academy.item.InductionFactorItem> INDUCTION_FACTOR =
            REGISTER.register("induction_factor",
                    () -> new cn.academy.item.InductionFactorItem(new Item.Properties()));

    public static final RegistryObject<Item> MAGNETIC_COIL = REGISTER.register("magnetic_coil",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CONSTRAINT_PLATE = REGISTER.register("constraint_plate",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<cn.academy.item.MatrixCoreItem> MAT_CORE_1 = REGISTER.register("mat_core_1",
            () -> new cn.academy.item.MatrixCoreItem(1, new Item.Properties()));

    public static final RegistryObject<cn.academy.item.MatrixCoreItem> MAT_CORE_2 = REGISTER.register("mat_core_2",
            () -> new cn.academy.item.MatrixCoreItem(2, new Item.Properties()));

    public static final RegistryObject<cn.academy.item.MatrixCoreItem> MAT_CORE_3 = REGISTER.register("mat_core_3",
            () -> new cn.academy.item.MatrixCoreItem(3, new Item.Properties()));

    public static final RegistryObject<cn.academy.item.ItemEnergyBase> ENERGY_UNIT = REGISTER.register("energy_unit",
            () -> new cn.academy.item.ItemEnergyBase(10000, 20));

    public static final RegistryObject<cn.academy.item.ItemDeveloper> DEVELOPER_PORTABLE =
            REGISTER.register("developer_portable", cn.academy.item.ItemDeveloper::new);

    public static final RegistryObject<cn.academy.item.MatterUnitItem> MATTER_UNIT = REGISTER.register("matter_unit",
            cn.academy.item.MatterUnitItem::new);

    public static final RegistryObject<cn.academy.item.ItemTerminalInstaller> TERMINAL_INSTALLER =
            REGISTER.register("terminal_installer",
                    () -> new cn.academy.item.ItemTerminalInstaller(new Item.Properties()));

    public static final RegistryObject<cn.academy.item.ItemApp> APP_SKILL_TREE =
            REGISTER.register("app_skill_tree",
                    () -> new cn.academy.item.ItemApp(new Item.Properties(), "skill_tree"));

    public static final RegistryObject<cn.academy.item.ItemApp> APP_FREQ_TRANSMITTER =
            REGISTER.register("app_freq_transmitter",
                    () -> new cn.academy.item.ItemApp(new Item.Properties(), "freq_transmitter"));

    private ACItems() {}
}
