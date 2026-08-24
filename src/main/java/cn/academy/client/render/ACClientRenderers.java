package cn.academy.client.render;

import cn.academy.ACBlockEntities;
import cn.academy.ACEntities;
import cn.academy.client.render.entity.ArcRenderer;
import cn.academy.client.render.entity.RippleMarkRender;
import cn.academy.client.render.entity.SurroundArcRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@OnlyIn(Dist.CLIENT)
public final class ACClientRenderers {

    public static BakedModel matrixShield;

    public static BakedModel matrixBase;

    public static BakedModel windgenFan;

    public static BakedModel developerNormal, developerAdvanced;

    public static BakedModel needleStuck;
    public static final net.minecraft.client.resources.model.ModelResourceLocation MODEL_NEEDLE_STUCK =
            new net.minecraft.client.resources.model.ModelResourceLocation("academy", "needle_stuck", "inventory");

    public static BakedModel silbarnCrystal;

    public static BakedModel silbarnIcon;

    public static BakedModel[] silbarnFrames = new BakedModel[0];
    public static final net.minecraft.client.resources.model.ModelResourceLocation MODEL_SILBARN_ICON =
            new net.minecraft.client.resources.model.ModelResourceLocation("academy", "silbarn_icon", "inventory");

    public static BakedModel developerPortable;
    public static final net.minecraft.resources.ResourceLocation MODEL_PORTABLE =
            new net.minecraft.resources.ResourceLocation("academy", "item/developer_portable_model");

    public static BakedModel devPortableIconEmpty, devPortableIconHalf, devPortableIconFull;
    public static final net.minecraft.resources.ResourceLocation
            ICON_PORTABLE_EMPTY = new net.minecraft.resources.ResourceLocation("academy", "item/developer_portable_icon_empty"),
            ICON_PORTABLE_HALF = new net.minecraft.resources.ResourceLocation("academy", "item/developer_portable_icon_half"),
            ICON_PORTABLE_FULL = new net.minecraft.resources.ResourceLocation("academy", "item/developer_portable_icon_full");

    public static BakedModel remoteControl, remoteControlIcon;
    public static final net.minecraft.resources.ResourceLocation MODEL_REMOTE =
            new net.minecraft.resources.ResourceLocation("academy", "item/remote_control_model");
    public static final net.minecraft.client.resources.model.ModelResourceLocation MODEL_REMOTE_ICON =
            new net.minecraft.client.resources.model.ModelResourceLocation("academy", "remote_control_icon", "inventory");

    private ACClientRenderers() {}

    public static void register(IEventBus modBus) {
        modBus.register(ACClientRenderers.class);
    }

    @SubscribeEvent
    public static void onRegisterReloadListeners(
            net.minecraftforge.client.event.RegisterClientReloadListenersEvent e) {
        e.registerReloadListener(
                (net.minecraft.server.packs.resources.ResourceManagerReloadListener)
                        rm -> cn.academy.client.gui.SvgShape.clearCache());
    }

    @SubscribeEvent
    public static void onRegisterParticles(net.minecraftforge.client.event.RegisterParticleProvidersEvent e) {
        e.registerSpriteSet(cn.academy.ACParticles.TP.get(),
                cn.academy.client.render.misc.TPParticle.Provider::new);
        e.registerSpriteSet(cn.academy.ACParticles.FORMULA.get(),
                cn.academy.client.render.misc.FormulaParticle.Provider::new);
        e.registerSpriteSet(cn.academy.ACParticles.MD.get(),
                cn.academy.client.render.misc.MdParticle.Provider::new);
        e.registerSpriteSet(cn.academy.ACParticles.SMOKE.get(),
                cn.academy.client.render.misc.SmokeParticle.Provider::new);
        e.registerSpriteSet(cn.academy.ACParticles.SILBARN_FRAG.get(),
                cn.academy.client.render.misc.SilbarnFragParticle.Provider::new);
        e.registerSpriteSet(cn.academy.ACParticles.FEATHER.get(),
                cn.academy.client.render.misc.FeatherMoteParticle.Provider::new);
        e.registerSpriteSet(cn.academy.ACParticles.GOLD_FEATHER.get(),
                cn.academy.client.render.misc.GoldFeatherParticle.Provider::new);
        e.registerSpriteSet(cn.academy.ACParticles.PLATINUM_FEATHER.get(),
                cn.academy.client.render.misc.PlatinumFeatherParticle.Provider::new);
        e.registerSpriteSet(cn.academy.ACParticles.SONIC_WAVE.get(),
                cn.academy.client.render.misc.SonicWaveParticle.Provider::new);

        e.registerSpecial(cn.academy.ACParticles.IRON_SAND.get(),
                new cn.academy.client.render.misc.IronSandParticle.PuffProvider());
        e.registerSpecial(cn.academy.ACParticles.IRON_SAND_FINE.get(),
                new cn.academy.client.render.misc.IronSandParticle.FineProvider());
        e.registerSpecial(cn.academy.ACParticles.IRON_SAND_WHIP.get(),
                new cn.academy.client.render.misc.IronSandParticle.WhipProvider());
    }

    @SubscribeEvent
    public static void onRegisterAdditional(ModelEvent.RegisterAdditional e) {
        e.register(WirelessMatrixRenderer.SHIELD_MODEL);
        e.register(WirelessMatrixRenderer.BASE_MODEL);
        e.register(WindgenMainRenderer.FAN_MODEL);
        e.register(DeveloperRenderer.MODEL_NORMAL);
        e.register(DeveloperRenderer.MODEL_ADVANCED);
        e.register(MODEL_PORTABLE);
        e.register(ICON_PORTABLE_EMPTY);
        e.register(ICON_PORTABLE_HALF);
        e.register(ICON_PORTABLE_FULL);
        e.register(MODEL_NEEDLE_STUCK);
        e.register(MODEL_REMOTE);
        e.register(MODEL_REMOTE_ICON);
        e.register(cn.academy.client.render.entity.SilbarnRenderer.MODEL);

        int frames = 0;
        while (frames < cn.academy.client.render.entity.SilbarnRenderer.MAX_FRAMES
                && cn.academy.client.render.entity.SilbarnRenderer.frameExists(frames + 1)) {
            frames++;
            e.register(cn.academy.client.render.entity.SilbarnRenderer.frameModel(frames));
        }
        cn.academy.client.render.entity.SilbarnRenderer.extraFrames = frames;
        e.register(MODEL_SILBARN_ICON);
    }

    @SubscribeEvent
    public static void onBakingCompleted(ModelEvent.BakingCompleted e) {
        matrixShield = e.getModels().get(WirelessMatrixRenderer.SHIELD_MODEL);
        matrixBase = e.getModels().get(WirelessMatrixRenderer.BASE_MODEL);
        windgenFan = e.getModels().get(WindgenMainRenderer.FAN_MODEL);
        developerNormal = e.getModels().get(DeveloperRenderer.MODEL_NORMAL);
        developerAdvanced = e.getModels().get(DeveloperRenderer.MODEL_ADVANCED);
        developerPortable = e.getModels().get(MODEL_PORTABLE);
        devPortableIconEmpty = e.getModels().get(ICON_PORTABLE_EMPTY);
        devPortableIconHalf = e.getModels().get(ICON_PORTABLE_HALF);
        devPortableIconFull = e.getModels().get(ICON_PORTABLE_FULL);
        needleStuck = e.getModels().get(MODEL_NEEDLE_STUCK);
        remoteControl = e.getModels().get(MODEL_REMOTE);
        remoteControlIcon = e.getModels().get(MODEL_REMOTE_ICON);
        silbarnCrystal = e.getModels().get(cn.academy.client.render.entity.SilbarnRenderer.MODEL);
        silbarnIcon = e.getModels().get(MODEL_SILBARN_ICON);

        int nf = cn.academy.client.render.entity.SilbarnRenderer.extraFrames;
        silbarnFrames = new BakedModel[nf];
        for (int i = 0; i < nf; i++) {
            silbarnFrames[i] = e.getModels().get(
                    cn.academy.client.render.entity.SilbarnRenderer.frameModel(i + 1));
        }
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerBlockEntityRenderer(ACBlockEntities.WIRELESS_MATRIX.get(), WirelessMatrixRenderer::new);
        e.registerBlockEntityRenderer(ACBlockEntities.WINDGEN_MAIN.get(), WindgenMainRenderer::new);

        e.registerBlockEntityRenderer(ACBlockEntities.DEVELOPER.get(), DeveloperRenderer::new);

        e.registerBlockEntityRenderer(ACBlockEntities.IMAG_PHASE.get(),
                cn.academy.client.render.block.ImagPhaseRenderer::new);

        e.registerEntityRenderer(ACEntities.ARC.get(), ArcRenderer::new);
        e.registerEntityRenderer(ACEntities.SURROUND_ARC.get(), SurroundArcRenderer::new);
        e.registerEntityRenderer(ACEntities.RIPPLE_MARK.get(), RippleMarkRender::new);
        e.registerEntityRenderer(ACEntities.WAVE.get(),
                cn.academy.client.render.entity.WaveRenderer::new);
        e.registerEntityRenderer(ACEntities.PARABOLA.get(),
                cn.academy.client.render.entity.ParabolaRenderer::new);

        e.registerEntityRenderer(ACEntities.STORM_WING.get(),
                cn.academy.client.render.entity.StormWingRenderer::new);

        e.registerEntityRenderer(ACEntities.DUAL_WING.get(),
                cn.academy.client.render.entity.DualWingRenderer::new);

        e.registerEntityRenderer(ACEntities.GUST_TORNADO.get(),
                cn.academy.client.render.entity.GustTornadoRenderer::new);

        e.registerEntityRenderer(ACEntities.PLASMA_BODY.get(),
                cn.academy.client.render.entity.PlasmaBodyRenderer::new);
        e.registerEntityRenderer(ACEntities.PLASMA_TORNADO.get(),
                cn.academy.client.render.entity.PlasmaTornadoRenderer::new);

        e.registerEntityRenderer(ACEntities.COIN_THROWING.get(),
                cn.academy.client.render.entity.CoinThrowingRenderer::new);

        e.registerEntityRenderer(ACEntities.RAILGUN_FX.get(),
                cn.academy.client.render.entity.RailgunFXRenderer::new);

        e.registerEntityRenderer(ACEntities.MD_BALL.get(),
                cn.academy.client.render.entity.MdBallRenderer::new);
        e.registerEntityRenderer(ACEntities.MD_RAY_SMALL.get(),
                cn.academy.client.render.entity.MdRaySmallRenderer::new);

        e.registerEntityRenderer(ACEntities.MD_RAY.get(),
                cn.academy.client.render.entity.MDRayRenderer::new);

        e.registerEntityRenderer(ACEntities.MD_RAY_BARRAGE.get(),
                cn.academy.client.render.entity.MdRayBarrageRenderer::new);

        e.registerEntityRenderer(ACEntities.SILBARN.get(),
                cn.academy.client.render.entity.SilbarnRenderer::new);

        e.registerEntityRenderer(ACEntities.MD_SHIELD.get(),
                cn.academy.client.render.entity.MdShieldRenderer::new);

        e.registerEntityRenderer(ACEntities.DIAMOND_SHIELD.get(),
                cn.academy.client.render.entity.DiamondShieldRenderer::new);

        e.registerEntityRenderer(ACEntities.RAILGUN_HAND.get(),
                cn.academy.client.render.entity.RailgunHandRenderer::new);

        e.registerEntityRenderer(ACEntities.THUNDER_STRIKE.get(),
                cn.academy.client.render.entity.ThunderStrikeRenderer::new);

        e.registerEntityRenderer(ACEntities.MARKER.get(),
                cn.academy.client.render.entity.MarkerRenderer::new);

        e.registerEntityRenderer(ACEntities.TP_MARKING.get(),
                cn.academy.client.render.entity.TPMarkingRenderer::new);

        e.registerEntityRenderer(ACEntities.SHIFT_BLOCK.get(),
                cn.academy.client.render.entity.ShiftBlockRenderer::new);

        e.registerEntityRenderer(ACEntities.SHIFT_NEEDLE.get(),
                cn.academy.client.render.entity.ShiftNeedleRenderer::new);
    }

    @SubscribeEvent
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void onAddLayers(EntityRenderersEvent.AddLayers e) {
        net.minecraft.client.renderer.entity.ItemRenderer itemRenderer =
                net.minecraft.client.Minecraft.getInstance().getItemRenderer();
        for (net.minecraft.world.entity.EntityType<?> type
                : net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE) {
            try {
                net.minecraft.client.renderer.entity.LivingEntityRenderer r = e.getRenderer((net.minecraft.world.entity.EntityType) type);
                if (r != null) {
                    r.addLayer(new cn.academy.client.render.entity.NeedleStuckLayer(r, itemRenderer));
                }
            } catch (Exception ignored) {

            }
        }
        for (String skin : e.getSkins()) {
            net.minecraft.client.renderer.entity.LivingEntityRenderer r = e.getSkin(skin);
            if (r != null) {
                r.addLayer(new cn.academy.client.render.entity.NeedleStuckLayer(r, itemRenderer));

                r.addLayer(new cn.academy.client.render.entity.DualWingLimbLayer(r));
            }
        }
    }
}
