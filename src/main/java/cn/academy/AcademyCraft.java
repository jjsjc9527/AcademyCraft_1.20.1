package cn.academy;

/*
 * 原作者:LambdaInnovation。本项目基于 GPLv3 进行 1.20.1 移植/二次开发,完全免费,禁止任何形式收费。
 * 原仓库:https://github.com/LambdaInnovation/AcademyCraft
 */
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(AcademyCraft.MODID)
public final class AcademyCraft {

    public static final String MODID = "academy";

    public static final Logger LOGGER = LogUtils.getLogger();

    public static final boolean DEBUG_MODE = false;

    public static void debug(Object message) {
        if (DEBUG_MODE) {
            LOGGER.info(String.valueOf(message));
        }
    }

    public static cn.academy.config.Configuration config;

    public AcademyCraft() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        config = new cn.academy.config.Configuration(
                net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get().resolve("academy-craft.toml"));

        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(
                net.minecraftforge.fml.config.ModConfig.Type.COMMON,
                cn.academy.config.InterfererConfig.SPEC, "academy-ability-interferer.toml");

        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(
                net.minecraftforge.fml.config.ModConfig.Type.COMMON,
                cn.academy.config.AbilityConfig.SPEC, "academy-ability.toml");

        ACBlocks.REGISTER.register(modBus);
        ACItems.REGISTER.register(modBus);

        ACFluids.FLUID_TYPES.register(modBus);
        ACFluids.FLUIDS.register(modBus);
        ACBlockEntities.REGISTER.register(modBus);
        ACCreativeTabs.REGISTER.register(modBus);
        ACMenus.REGISTER.register(modBus);

        ACSounds.REGISTER.register(modBus);

        ACEntities.REGISTER.register(modBus);

        ACParticles.REGISTER.register(modBus);

        cn.lambdalib2.datapart.DataPartCapability.bootstrap(modBus);

        modBus.addListener(this::commonSetup);
        modBus.addListener(this::clientSetup);

        MinecraftForge.EVENT_BUS.register(this);

        MinecraftForge.EVENT_BUS.register(new cn.academy.datapart.CPData.Events());

        MinecraftForge.EVENT_BUS.register(new cn.academy.datapart.PresetData.Events());
        MinecraftForge.EVENT_BUS.register(new cn.academy.datapart.CooldownData.Events());

        MinecraftForge.EVENT_BUS.register(new cn.academy.datapart.RemoteData.RemoteEvents());

        MinecraftForge.EVENT_BUS.register(new cn.academy.util.ACPierce.Events());

        MinecraftForge.EVENT_BUS.addListener(cn.academy.command.ACCommands::onRegisterCommands);

        net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> cn.academy.client.render.ACClientRenderers.register(modBus));

        net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> cn.academy.client.render.ACGuiShaders.register(modBus));

        net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> cn.academy.client.render.ACEffectShaders.register(modBus));

        LOGGER.info("AcademyCraft (1.20.1 port) is loading -- GPLv3 derivative work, original author LambdaInnovation.");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {

        cn.lambdalib2.datapart.DataPartCapability.init();

        cn.academy.network.ACNetwork.register();

        cn.academy.ability.context.ContextManager.bootstrap();

        cn.academy.ability.AbilityPipeline.init();

        cn.academy.ability.AbilitySerialization.register();
        cn.academy.ability.vanilla.electromaster.CatElectromaster.register();

        cn.academy.ability.vanilla.teleporter.CatTeleporter.register();
        cn.academy.ability.vanilla.vecmanip.CatVecManip.register();
        cn.academy.ability.vanilla.meltdowner.CatMeltdowner.register();

        cn.academy.ability.vanilla.mentalout.CatMentalOut.register();

        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
                new cn.academy.api.ACRegisterCategoriesEvent());
        cn.academy.ability.CategoryManager.INSTANCE.bake();

        cn.academy.ability.vanilla.electromaster.Railgun.init();

        cn.academy.item.CoinItem.init();

        cn.academy.energy.impl.WirelessSystem.bootstrap();

        cn.academy.crafting.ImagFusorRecipes.register();
        cn.academy.crafting.MetalFormerRecipes.register();

        cn.academy.terminal.ACApps.register();

        cn.academy.tutorial.Conditions.init();
        cn.academy.tutorial.TutorialInit.init();

        event.enqueueWork(cn.academy.advancements.ACAdvancements::init);

        cn.academy.ability.vanilla.mentalout.ProxyState.init();
    }

    private void clientSetup(final FMLClientSetupEvent event) {

        cn.academy.ability.context.ClientContext.scanAndRegister();

        cn.lambdalib2.util.GameTimer.extraFreeze =
                cn.academy.ability.vanilla.mentalout.DazeState::isLocalPlayerDazed;

        cn.academy.ability.vanilla.mentalout.FaintCameraTilt.init();

        cn.academy.client.sound.ACSoundVolume.init();

        cn.academy.ability.vanilla.mentalout.SelfLossBlackout.init();

        cn.academy.ability.vanilla.mentalout.ControlClientDrive.init();

        cn.academy.ability.vanilla.mentalout.ProxyClientDrive.init();

        cn.academy.ability.vanilla.mentalout.HelplessClientInput.init();

        cn.academy.ability.vanilla.mentalout.skill.ForcedControl.MiddleKey.init();

        cn.lambdalib2.input.InputGate.bootstrap();

        cn.academy.client.gui.config.ACConfigEntryButton.bootstrap();

        cn.lambdalib2.auxgui.AuxGuiHandler.init();

        cn.lambdalib2.util.ControlOverrider.init();

        cn.academy.client.auxgui.TerminalUI.registerKeyHandler();

        cn.academy.client.auxgui.DebugConsole.init();

        cn.academy.ability.vanilla.teleporter.skill.ShiftTeleport.NeedleKeyHandler.init();

        cn.academy.ability.vanilla.vecmanip.skill.VecDeviation.ModeKeyHandler.init();

        cn.academy.ability.vanilla.meltdowner.skill.ElectronMissile.ModeKeyHandler.init();

        cn.academy.ability.vanilla.electromaster.IronSandControl.ModeKeyHandler.init();
        cn.academy.ability.vanilla.vecmanip.skill.WaveRippleUI.init();

        cn.academy.ability.vanilla.vecmanip.advanced.CrushFieldFx.init();

        cn.academy.client.auxgui.CPBarSettings.init();

        cn.academy.client.compat.EpicFightCompat.init();

        cn.academy.client.auxgui.ACHud.init();

        cn.academy.client.auxgui.CPBar.init();
        cn.academy.client.auxgui.KeyHintUI.init();
        cn.academy.client.auxgui.BackgroundMask.init();

        cn.academy.client.auxgui.QuickPlanSelector.init();

        cn.academy.client.auxgui.RemotePlanHint.init();

        cn.academy.client.gui.NotifyUI.init();
        cn.academy.ability.context.ClientRuntime.bootstrap();
        cn.academy.ability.ctrl.ClientHandler.init();

        cn.academy.client.gui.CustomizeUI.register();

        for (net.minecraft.world.level.block.Block b : new net.minecraft.world.level.block.Block[]{
                ACBlocks.WINDGEN_BASE.get(), ACBlocks.WINDGEN_MAIN.get(),
                ACBlocks.WIRELESS_MATRIX.get(), ACBlocks.DEV_NORMAL.get(), ACBlocks.DEV_ADVANCED.get()}) {
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
                    b, net.minecraft.client.renderer.RenderType.cutout());
        }

        event.enqueueWork(() -> {
            net.minecraft.client.gui.screens.MenuScreens.register(
                    ACMenus.WIRELESS_MATRIX.get(), cn.academy.client.gui.WirelessMatrixScreen::new);
            net.minecraft.client.gui.screens.MenuScreens.register(
                    ACMenus.WINDGEN_BASE.get(), cn.academy.client.gui.WindgenBaseScreen::new);
            net.minecraft.client.gui.screens.MenuScreens.register(
                    ACMenus.WINDGEN_MAIN.get(), cn.academy.client.gui.WindgenMainScreen::new);
            net.minecraft.client.gui.screens.MenuScreens.register(
                    ACMenus.WIRELESS_GENERATOR.get(), cn.academy.client.gui.WirelessGeneratorScreen::new);
            net.minecraft.client.gui.screens.MenuScreens.register(
                    ACMenus.WIRELESS_NODE.get(), cn.academy.client.gui.WirelessNodeScreen::new);
            net.minecraft.client.gui.screens.MenuScreens.register(
                    ACMenus.PHASE_GEN.get(), cn.academy.client.gui.PhaseGenScreen::new);
            net.minecraft.client.gui.screens.MenuScreens.register(
                    ACMenus.IMAG_FUSOR.get(), cn.academy.client.gui.ImagFusorScreen::new);
            net.minecraft.client.gui.screens.MenuScreens.register(
                    ACMenus.METAL_FORMER.get(), cn.academy.client.gui.MetalFormerScreen::new);
            net.minecraft.client.gui.screens.MenuScreens.register(
                    ACMenus.ABILITY_INTERFERER.get(), cn.academy.client.gui.AbilityInterfererScreen::new);

            cn.academy.item.ItemEnergyBase.registerProperties(ACItems.ENERGY_UNIT.get());

            cn.academy.item.MatterUnitItem.registerProperties(ACItems.MATTER_UNIT.get());

            cn.academy.item.InductionFactorItem.registerProperties(ACItems.INDUCTION_FACTOR.get());
        });
    }
}
