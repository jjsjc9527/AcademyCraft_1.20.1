package cn.academy;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Consumer;

public final class ACFluids {

    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, AcademyCraft.MODID);
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(ForgeRegistries.FLUIDS, AcademyCraft.MODID);

    private static final ResourceLocation TEX = new ResourceLocation(AcademyCraft.MODID, "block/black");

    public static final RegistryObject<FluidType> IMAGPROJ_TYPE = FLUID_TYPES.register("imagproj",
            () -> new FluidType(FluidType.Properties.create()
                    .lightLevel(8)
                    .density(1)
                    .viscosity(6000)
                    .temperature(0)
                    .canSwim(true)
                    .canDrown(true)
                    .supportsBoating(false)) {
                @Override
                public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                    consumer.accept(new IClientFluidTypeExtensions() {
                        @Override
                        public ResourceLocation getStillTexture() {
                            return TEX;
                        }

                        @Override
                        public ResourceLocation getFlowingTexture() {
                            return TEX;
                        }
                    });
                }
            });

    public static final RegistryObject<FlowingFluid> IMAGPROJ =
            FLUIDS.register("imagproj", () -> new ForgeFlowingFluid.Source(props()));

    public static final RegistryObject<FlowingFluid> IMAGPROJ_FLOWING =
            FLUIDS.register("imagproj_flowing", () -> new ForgeFlowingFluid.Flowing(props()));

    private static ForgeFlowingFluid.Properties props() {
        return new ForgeFlowingFluid.Properties(IMAGPROJ_TYPE, IMAGPROJ, IMAGPROJ_FLOWING)
                .block(ACBlocks.IMAG_PHASE)
                .levelDecreasePerBlock(3)
                .tickRate(30)
                .slopeFindDistance(2);
    }

    private ACFluids() {}
}
