package cn.academy.client.render;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "academy", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class AllyCamFeed {

    private static final int FEED_H = 128;

    private static double frameBudgetMs() {
        return Math.max(0.5, cn.academy.config.AbilityConfig
                .stat("wide_cast", "cam_budget_ms", 0f));
    }

    private static double maxFeedDist() {
        return Math.max(0.0, cn.academy.config.AbilityConfig
                .stat("wide_cast", "cam_range", 0f));
    }

    private static final long IDLE_DROP_MS = 2000;

    private static final int MAX_FEEDS = 12;

    private static final Map<UUID, Feed> FEEDS = new HashMap<>();

    private static final Set<UUID> WANTED = new LinkedHashSet<>();

    private static UUID focused;

    private static long wantedAt;

    private static boolean rendering;

    public static boolean isRendering() {
        return rendering;
    }

    private static com.mojang.blaze3d.pipeline.RenderTarget currentTarget;

    public static com.mojang.blaze3d.pipeline.RenderTarget currentTarget() {
        return currentTarget;
    }

    private AllyCamFeed() {}

    private static final class Feed {
        TextureTarget target;
        int w, h;
        long lastAt;

        boolean live;

        boolean broken;
    }

    public static void want(UUID id, boolean focus) {
        if (id != null) {
            WANTED.add(id);
            if (focus) {
                focused = id;
            }
            wantedAt = Util.getMillis();
        }
    }

    public static boolean live(UUID id) {
        Feed f = FEEDS.get(id);
        return f != null && f.live && f.target != null;
    }

    public static int[] texture(UUID id) {
        Feed f = FEEDS.get(id);
        if (f == null || f.target == null || !f.live) {
            return null;
        }
        return new int[]{f.target.getColorTextureId(), f.w, f.h};
    }

    public static void release() {
        for (Feed f : FEEDS.values()) {
            if (f.target != null) {
                f.target.destroyBuffers();
            }
        }
        FEEDS.clear();
        WANTED.clear();

        RESOLVED.clear();
    }

    public static boolean shaderActive() {
        if (!irisPresent()) {
            return false;
        }
        resolveIrisMethods();
        try {
            if (irisGetInstance == null || irisInUse == null) {
                return true;
            }
            return (Boolean) irisInUse.invoke(irisGetInstance.invoke(null));
        } catch (Throwable ignored) {
            return true;
        }
    }

    public static boolean shaderBlocked() {
        return shaderActive();
    }

    public static boolean jammed(UUID id) {
        Minecraft mc = Minecraft.getInstance();
        if (id == null || mc.player == null) {
            return false;
        }
        cn.academy.datapart.RemoteData rd = cn.academy.datapart.RemoteData.get(mc.player);
        return rd != null && rd.isJammed(id);
    }

    private static Boolean irisPresent;
    private static Method irisGetInstance;
    private static Method irisInUse;
    private static boolean irisMethodsResolved;

    private static boolean irisPresent() {
        Boolean v = irisPresent;
        if (v != null) {
            return v;
        }
        ModList ml = ModList.get();
        if (ml == null) {
            return false;
        }
        boolean r = ml.isLoaded("oculus") || ml.isLoaded("iris");
        irisPresent = r;
        return r;
    }

    private static void resolveIrisMethods() {
        if (irisMethodsResolved) {
            return;
        }
        irisMethodsResolved = true;
        try {
            Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            irisGetInstance = api.getMethod("getInstance");
            irisInUse = api.getMethod("isShaderPackInUse");
        } catch (Throwable ignored) {

        }
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        long now = Util.getMillis();
        if (WANTED.isEmpty()) {
            if (!FEEDS.isEmpty() && now - wantedAt > IDLE_DROP_MS) {
                release();
            }
            return;
        }
        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null || mc.player == null || mc.gameMode == null || shaderBlocked()) {
            WANTED.clear();
            return;
        }

        long deadline = Util.getNanos() + (long) (frameBudgetMs() * 1.0e6);
        double maxDistSqr = maxFeedDist() * maxFeedDist();

        Entity viewer = mc.getCameraEntity() == null ? mc.player : mc.getCameraEntity();
        for (UUID id : orderedWanted()) {
            if (Util.getNanos() >= deadline) {
                break;
            }
            Entity cam = resolve(id);
            Feed f = FEEDS.computeIfAbsent(id, k -> new Feed());

            if (cam == null || !cam.isAlive() || cam == mc.player
                    || viewer.distanceToSqr(cam) > maxDistSqr || jammed(id)) {
                f.live = false;
                f.lastAt = now;
            } else {
                renderOne(mc, f, cam, event.renderTickTime);
            }
        }
        WANTED.clear();
        focused = null;
    }

    private static java.util.List<UUID> orderedWanted() {
        java.util.List<UUID> out = new java.util.ArrayList<>(WANTED.size());
        for (UUID id : WANTED) {
            Feed f = FEEDS.get(id);
            if (f != null && f.broken) {
                continue;
            }
            if (f == null && FEEDS.size() >= MAX_FEEDS) {
                continue;
            }
            out.add(id);
        }
        out.sort((a, b) -> {
            if (a.equals(focused) != b.equals(focused)) {
                return a.equals(focused) ? -1 : 1;
            }
            long ta = FEEDS.containsKey(a) ? FEEDS.get(a).lastAt : 0L;
            long tb = FEEDS.containsKey(b) ? FEEDS.get(b).lastAt : 0L;
            return Long.compare(ta, tb);
        });
        return out;
    }

    public static Entity resolve(UUID id) {
        Minecraft mc = Minecraft.getInstance();
        if (id == null || mc.level == null) {
            return null;
        }
        Entity cached = cachedResolve(id);
        if (cached != null) {
            return cached;
        }
        Entity p = mc.level.getPlayerByUUID(id);
        if (p != null) {
            return remember(id, p);
        }
        for (Entity e : mc.level.entitiesForRendering()) {
            if (id.equals(e.getUUID())) {
                return remember(id, e);
            }
        }
        return null;
    }

    private static final Map<UUID, java.lang.ref.WeakReference<Entity>> RESOLVED = new HashMap<>();

    private static Entity cachedResolve(UUID id) {
        java.lang.ref.WeakReference<Entity> ref = RESOLVED.get(id);
        if (ref == null) {
            return null;
        }
        Entity e = ref.get();
        if (e == null || e.isRemoved() || !id.equals(e.getUUID())) {
            RESOLVED.remove(id);
            return null;
        }
        return e;
    }

    private static Entity remember(UUID id, Entity e) {
        RESOLVED.put(id, new java.lang.ref.WeakReference<>(e));
        return e;
    }

    private static void ensureTarget(Minecraft mc, Feed f) {
        int winW = Math.max(1, mc.getWindow().getWidth());
        int winH = Math.max(1, mc.getWindow().getHeight());

        int h = FEED_H;
        int w = Math.max(16, Math.round((float) FEED_H * winW / winH));
        if (f.target == null || f.w != w || f.h != h) {
            if (f.target != null) {
                f.target.destroyBuffers();
            }
            f.target = new TextureTarget(w, h, true, Minecraft.ON_OSX);
            f.w = w;
            f.h = h;
        }
    }

    private static void renderOne(Minecraft mc, Feed f, Entity cam, float partialTick) {
        ensureTarget(mc, f);
        Entity prevCam = mc.getCameraEntity();
        HitResult prevHit = mc.hitResult;
        Entity prevCross = mc.crosshairPickEntity;

        boolean prevHand = ((cn.academy.mixin.client.GameRendererAccessor) mc.gameRenderer)
                .academy$isRenderHand();

        cn.academy.mixin.client.LevelRendererAccessor lrAcc =
                (cn.academy.mixin.client.LevelRendererAccessor) mc.levelRenderer;
        net.minecraft.client.renderer.PostChain prevChain = lrAcc.academy$getTransparencyChain();

        float prevSpin = 0f, prevOSpin = 0f;
        boolean portalTouched = mc.player != null;
        if (portalTouched) {
            prevSpin = mc.player.spinningEffectIntensity;
            prevOSpin = mc.player.oSpinningEffectIntensity;
        }

        rendering = true;
        currentTarget = f.target;
        try {
            if (portalTouched) {
                mc.player.spinningEffectIntensity = 0f;
                mc.player.oSpinningEffectIntensity = 0f;
            }
            lrAcc.academy$setTransparencyChain(null);
            mc.gameRenderer.setRenderHand(false);

            mc.cameraEntity = cam;
            f.target.setClearColor(0.0f, 0.0f, 0.0f, 1.0f);
            f.target.clear(Minecraft.ON_OSX);
            f.target.bindWrite(true);
            mc.gameRenderer.renderLevel(partialTick, Util.getNanos(), new PoseStack());
            f.live = true;
        } catch (Throwable ex) {

            f.live = false;
            f.broken = true;
            cn.academy.AcademyCraft.LOGGER.warn(
                    "[AllyCamFeed] this feed failed to render and was disabled: {}", cam.getName().getString(), ex);
        } finally {

            rendering = false;
            currentTarget = null;

            if (portalTouched) {
                mc.player.spinningEffectIntensity = prevSpin;
                mc.player.oSpinningEffectIntensity = prevOSpin;
            }
            lrAcc.academy$setTransparencyChain(prevChain);
            mc.cameraEntity = prevCam;
            mc.hitResult = prevHit;
            mc.crosshairPickEntity = prevCross;
            mc.gameRenderer.setRenderHand(prevHand);
            mc.getMainRenderTarget().bindWrite(true);
            f.lastAt = Util.getMillis();
        }
    }
}
