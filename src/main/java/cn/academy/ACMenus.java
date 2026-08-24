package cn.academy;

import cn.academy.block.container.WirelessMatrixMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ACMenus {

    public static final DeferredRegister<MenuType<?>> REGISTER =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, AcademyCraft.MODID);

    public static final RegistryObject<MenuType<WirelessMatrixMenu>> WIRELESS_MATRIX =
            REGISTER.register("matrix", () -> IForgeMenuType.create(
                    (id, inv, buf) -> new WirelessMatrixMenu(id, inv, buf.readBlockPos())));

    public static final RegistryObject<MenuType<cn.academy.block.container.WindgenBaseMenu>> WINDGEN_BASE =
            REGISTER.register("windgen_base", () -> IForgeMenuType.create(
                    (id, inv, buf) -> new cn.academy.block.container.WindgenBaseMenu(id, inv, buf.readBlockPos())));

    public static final RegistryObject<MenuType<cn.academy.block.container.WindgenMainMenu>> WINDGEN_MAIN =
            REGISTER.register("windgen_main", () -> IForgeMenuType.create(
                    (id, inv, buf) -> new cn.academy.block.container.WindgenMainMenu(id, inv, buf.readBlockPos())));

    public static final RegistryObject<MenuType<cn.academy.block.container.WirelessGeneratorMenu>> WIRELESS_GENERATOR =
            REGISTER.register("wireless_generator", () -> IForgeMenuType.create(
                    (id, inv, buf) -> new cn.academy.block.container.WirelessGeneratorMenu(id, inv, buf.readBlockPos())));

    public static final RegistryObject<MenuType<cn.academy.block.container.ImagFusorMenu>> IMAG_FUSOR =
            REGISTER.register("imag_fusor", () -> IForgeMenuType.create(
                    (id, inv, buf) -> new cn.academy.block.container.ImagFusorMenu(id, inv, buf.readBlockPos())));

    public static final RegistryObject<MenuType<cn.academy.block.container.MetalFormerMenu>> METAL_FORMER =
            REGISTER.register("metal_former", () -> IForgeMenuType.create(
                    (id, inv, buf) -> new cn.academy.block.container.MetalFormerMenu(id, inv, buf.readBlockPos())));

    public static final RegistryObject<MenuType<cn.academy.block.container.AbilityInterfererMenu>> ABILITY_INTERFERER =
            REGISTER.register("ability_interferer", () -> IForgeMenuType.create(
                    (id, inv, buf) -> new cn.academy.block.container.AbilityInterfererMenu(id, inv, buf.readBlockPos())));

    public static final RegistryObject<MenuType<cn.academy.block.container.PhaseGenMenu>> PHASE_GEN =
            REGISTER.register("phase_gen", () -> IForgeMenuType.create(
                    (id, inv, buf) -> new cn.academy.block.container.PhaseGenMenu(id, inv, buf.readBlockPos())));

    public static final RegistryObject<MenuType<cn.academy.block.container.WirelessNodeMenu>> WIRELESS_NODE =
            REGISTER.register("wireless_node", () -> IForgeMenuType.create(
                    (id, inv, buf) -> new cn.academy.block.container.WirelessNodeMenu(id, inv, buf.readBlockPos())));

    private ACMenus() {}
}
