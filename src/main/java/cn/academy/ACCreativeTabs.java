package cn.academy;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ACCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> REGISTER =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AcademyCraft.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = REGISTER.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.academy"))
                    .icon(() -> new ItemStack(ACItems.LOGO.get()))
                    .displayItems((params, output) -> {

                        output.accept(ACItems.MACHINE_FRAME.get());
                        output.accept(ACItems.CONSTRAINT_METAL.get());
                        output.accept(ACItems.CRYSTAL_ORE.get());
                        output.accept(ACItems.IMAGSIL_ORE.get());
                        output.accept(ACItems.RESO_ORE.get());
                        output.accept(ACItems.DEEPSLATE_CONSTRAINT_METAL.get());
                        output.accept(ACItems.DEEPSLATE_CRYSTAL_ORE.get());
                        output.accept(ACItems.DEEPSLATE_IMAGSIL_ORE.get());
                        output.accept(ACItems.DEEPSLATE_RESO_ORE.get());
                        output.accept(ACItems.NODE_BASIC.get());
                        output.accept(ACItems.NODE_STANDARD.get());
                        output.accept(ACItems.NODE_ADVANCED.get());
                        output.accept(ACItems.WIRELESS_GENERATOR.get());
                        output.accept(ACItems.PHASE_GEN.get());
                        output.accept(ACItems.IMAG_FUSOR.get());
                        output.accept(ACItems.METAL_FORMER.get());
                        output.accept(ACItems.ABILITY_INTERFERER.get());
                        output.accept(ACItems.DEV_NORMAL.get());
                        output.accept(ACItems.DEV_ADVANCED.get());

                        output.accept(cn.academy.energy.api.IFItemManager.instance
                                .createEmpty(ACItems.DEVELOPER_PORTABLE.get()));
                        output.accept(cn.academy.energy.api.IFItemManager.instance
                                .createFull(ACItems.DEVELOPER_PORTABLE.get()));
                        output.accept(ACItems.COIN.get());
                        output.accept(ACItems.IMAG_PHASE.get());
                        output.accept(ACItems.WIRELESS_RECEIVER.get());
                        output.accept(ACItems.WIRELESS_MATRIX.get());
                        output.accept(ACItems.WINDGEN_BASE.get());
                        output.accept(ACItems.WINDGEN_PILLAR.get());
                        output.accept(ACItems.WINDGEN_MAIN.get());
                        output.accept(ACItems.WINDGEN_FAN.get());

                        output.accept(ACItems.CRYSTAL_LOW.get());
                        output.accept(ACItems.CRYSTAL_NORMAL.get());
                        output.accept(ACItems.CRYSTAL_PURE.get());
                        output.accept(ACItems.RESO_CRYSTAL.get());
                        output.accept(ACItems.CONSTRAINT_PLATE.get());

                        output.accept(ACItems.CONSTRAINT_INGOT.get());
                        output.accept(ACItems.IMAG_SILICON_INGOT.get());
                        output.accept(ACItems.WAFER.get());
                        output.accept(ACItems.IMAG_SILICON_PIECE.get());
                        output.accept(ACItems.REINFORCED_IRON_PLATE.get());
                        output.accept(ACItems.NEEDLE.get());
                        output.accept(ACItems.SILBARN.get());
                        output.accept(ACItems.REMOTE_CONTROL.get());
                        output.accept(ACItems.DATA_CHIP.get());
                        output.accept(ACItems.CALC_CHIP.get());

                        output.accept(ACItems.ENERGY_CONVERT_COMPONENT.get());
                        output.accept(ACItems.BRAIN_COMPONENT.get());
                        output.accept(ACItems.INFO_COMPONENT.get());
                        output.accept(ACItems.RESONANCE_COMPONENT.get());

                        output.accept(ACItems.TERMINAL_INSTALLER.get());
                        output.accept(ACItems.APP_SKILL_TREE.get());
                        output.accept(ACItems.APP_FREQ_TRANSMITTER.get());
                        output.accept(ACItems.TUTORIAL.get());

                        output.accept(ACItems.MAGNETIC_COIL.get());

                        for (cn.academy.ability.Category c
                                : cn.academy.ability.CategoryManager.INSTANCE.getCategories()) {
                            output.accept(ACItems.INDUCTION_FACTOR.get().create(c));
                        }
                        output.accept(ACItems.MAT_CORE_1.get());
                        output.accept(ACItems.MAT_CORE_2.get());
                        output.accept(ACItems.MAT_CORE_3.get());

                        output.accept(cn.academy.energy.api.IFItemManager.instance
                                .createEmpty(ACItems.ENERGY_UNIT.get()));
                        output.accept(cn.academy.energy.api.IFItemManager.instance
                                .createFull(ACItems.ENERGY_UNIT.get()));

                        output.accept(ACItems.MATTER_UNIT.get()
                                .create(cn.academy.item.MatterUnitItem.MAT_NONE));
                        output.accept(ACItems.MATTER_UNIT.get()
                                .create(cn.academy.item.MatterUnitItem.MAT_PHASE_LIQUID));
                    })
                    .build());

    private ACCreativeTabs() {}
}
