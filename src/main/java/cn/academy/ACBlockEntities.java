package cn.academy;

import cn.academy.block.tileentity.WindgenBaseBlockEntity;
import cn.academy.block.tileentity.WindgenMainBlockEntity;
import cn.academy.block.tileentity.WirelessGeneratorBlockEntity;
import cn.academy.block.tileentity.WirelessMatrixBlockEntity;
import cn.academy.block.tileentity.WirelessNodeBlockEntity;
import cn.academy.block.tileentity.WirelessReceiverBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ACBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> REGISTER =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, AcademyCraft.MODID);

    public static final RegistryObject<BlockEntityType<WirelessNodeBlockEntity>> WIRELESS_NODE =
            REGISTER.register("wireless_node", () -> BlockEntityType.Builder
                    .of(WirelessNodeBlockEntity::new,
                            ACBlocks.NODE_BASIC.get(),
                            ACBlocks.NODE_STANDARD.get(),
                            ACBlocks.NODE_ADVANCED.get())
                    .build(null));

    public static final RegistryObject<BlockEntityType<cn.academy.block.tileentity.ImagPhaseBlockEntity>> IMAG_PHASE =
            REGISTER.register("imag_phase", () -> BlockEntityType.Builder
                    .of(cn.academy.block.tileentity.ImagPhaseBlockEntity::new, ACBlocks.IMAG_PHASE.get())
                    .build(null));

    public static final RegistryObject<BlockEntityType<cn.academy.block.tileentity.ImagFusorBlockEntity>> IMAG_FUSOR =
            REGISTER.register("imag_fusor", () -> BlockEntityType.Builder
                    .of(cn.academy.block.tileentity.ImagFusorBlockEntity::new, ACBlocks.IMAG_FUSOR.get())
                    .build(null));

    public static final RegistryObject<BlockEntityType<cn.academy.block.tileentity.DeveloperBlockEntity>> DEVELOPER =
            REGISTER.register("developer", () -> BlockEntityType.Builder

                    .of(cn.academy.block.tileentity.DeveloperBlockEntity::new,
                            ACBlocks.DEV_NORMAL.get(), ACBlocks.DEV_ADVANCED.get())
                    .build(null));

    public static final RegistryObject<BlockEntityType<cn.academy.block.tileentity.MetalFormerBlockEntity>> METAL_FORMER =
            REGISTER.register("metal_former", () -> BlockEntityType.Builder
                    .of(cn.academy.block.tileentity.MetalFormerBlockEntity::new, ACBlocks.METAL_FORMER.get())
                    .build(null));

    public static final RegistryObject<BlockEntityType<cn.academy.block.tileentity.AbilityInterfererBlockEntity>> ABILITY_INTERFERER =
            REGISTER.register("ability_interferer", () -> BlockEntityType.Builder
                    .of(cn.academy.block.tileentity.AbilityInterfererBlockEntity::new, ACBlocks.ABILITY_INTERFERER.get())
                    .build(null));

    public static final RegistryObject<BlockEntityType<cn.academy.block.tileentity.PhaseGenBlockEntity>> PHASE_GEN =
            REGISTER.register("phase_gen", () -> BlockEntityType.Builder
                    .of(cn.academy.block.tileentity.PhaseGenBlockEntity::new, ACBlocks.PHASE_GEN.get())
                    .build(null));

    public static final RegistryObject<BlockEntityType<WirelessGeneratorBlockEntity>> WIRELESS_GENERATOR =
            REGISTER.register("wireless_generator", () -> BlockEntityType.Builder
                    .of(WirelessGeneratorBlockEntity::new, ACBlocks.WIRELESS_GENERATOR.get())
                    .build(null));

    public static final RegistryObject<BlockEntityType<WirelessReceiverBlockEntity>> WIRELESS_RECEIVER =
            REGISTER.register("wireless_receiver", () -> BlockEntityType.Builder
                    .of(WirelessReceiverBlockEntity::new, ACBlocks.WIRELESS_RECEIVER.get())
                    .build(null));

    public static final RegistryObject<BlockEntityType<WirelessMatrixBlockEntity>> WIRELESS_MATRIX =
            REGISTER.register("matrix", () -> BlockEntityType.Builder
                    .of(WirelessMatrixBlockEntity::new, ACBlocks.WIRELESS_MATRIX.get())
                    .build(null));

    public static final RegistryObject<BlockEntityType<WindgenBaseBlockEntity>> WINDGEN_BASE =
            REGISTER.register("windgen_base", () -> BlockEntityType.Builder
                    .of(WindgenBaseBlockEntity::new, ACBlocks.WINDGEN_BASE.get())
                    .build(null));

    public static final RegistryObject<BlockEntityType<WindgenMainBlockEntity>> WINDGEN_MAIN =
            REGISTER.register("windgen_main", () -> BlockEntityType.Builder
                    .of(WindgenMainBlockEntity::new, ACBlocks.WINDGEN_MAIN.get())
                    .build(null));

    private ACBlockEntities() {}
}
