package cn.academy.client.render;

import cn.academy.client.render.entity.ArcRenderer;
import cn.academy.client.render.entity.DiamondShieldRenderer;
import cn.academy.client.render.entity.DualWingRenderer;
import cn.academy.client.render.entity.MDRayRenderer;
import cn.academy.client.render.entity.MarkerRenderer;
import cn.academy.client.render.entity.MdBallRenderer;
import cn.academy.client.render.entity.MdRayBarrageRenderer;
import cn.academy.client.render.entity.MdRaySmallRenderer;
import cn.academy.client.render.entity.MdShieldRenderer;
import cn.academy.client.render.entity.ParabolaRenderer;
import cn.academy.client.render.entity.PlasmaBodyRenderer;
import cn.academy.client.render.entity.PlasmaTornadoRenderer;
import cn.academy.client.render.entity.RailgunFXRenderer;
import cn.academy.client.render.entity.RailgunHandRenderer;
import cn.academy.client.render.entity.RippleMarkRender;
import cn.academy.client.render.entity.GustTornadoRenderer;
import cn.academy.client.render.entity.StormWingRenderer;
import cn.academy.client.render.entity.SurroundArcRenderer;
import cn.academy.client.render.entity.TPMarkingRenderer;
import cn.academy.client.render.entity.ThunderStrikeRenderer;
import cn.academy.client.render.entity.WaveRenderer;
import cn.academy.entity.EntityArc;
import cn.academy.entity.EntityDiamondShield;
import cn.academy.entity.EntityDualWing;
import cn.academy.entity.EntityMDRay;
import cn.academy.entity.EntityMarker;
import cn.academy.entity.EntityMdBall;
import cn.academy.entity.EntityMdRayBarrage;
import cn.academy.entity.EntityMdRaySmall;
import cn.academy.entity.EntityMdShield;
import cn.academy.entity.EntityParabola;
import cn.academy.entity.EntityPlasmaBody;
import cn.academy.entity.EntityPlasmaTornado;
import cn.academy.entity.EntityRailgunFX;
import cn.academy.entity.EntityRailgunHand;
import cn.academy.entity.EntityRippleMark;
import cn.academy.entity.EntityGustTornado;
import cn.academy.entity.EntityStormWing;
import cn.academy.entity.EntitySurroundArc;
import cn.academy.entity.EntityTPMarking;
import cn.academy.entity.EntityThunderStrike;
import cn.academy.entity.EntityWave;
import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public final class EffectDrawers {

    private static final Logger LOGGER = LogUtils.getLogger();

    private EffectDrawers() {}

    @FunctionalInterface
    public interface Drawer {
        void draw(Entity e, EffectDrawCtx ctx);
    }

    private static final Map<Class<?>, Drawer> REG = new IdentityHashMap<>();

    private static final Map<Class<?>, Drawer> CACHE = new IdentityHashMap<>();

    private static final Set<Class<?>> WARNED = java.util.Collections.newSetFromMap(new IdentityHashMap<>());

    private static void reg(Class<? extends Entity> cls, Drawer d) {
        REG.put(cls, d);
    }

    public static synchronized void register(Class<? extends Entity> cls, Drawer d) {
        java.util.Objects.requireNonNull(cls, "cls");
        java.util.Objects.requireNonNull(d, "drawer");
        Drawer old = REG.put(cls, d);
        if (old != null) {

            LOGGER.warn("[AC] Effect renderer for {} was registered twice; the later one wins. "
                    + "Either two mods claim the same class, or one mod registered it twice.",
                    cls.getName());
        }

        CACHE.clear();
    }

    static {

        reg(EntitySurroundArc.class, (e, c) -> SurroundArcRenderer.draw((EntitySurroundArc) e, c.pose(), c.buffers()));
        reg(EntityRippleMark.class, (e, c) -> RippleMarkRender.draw((EntityRippleMark) e, c.pose(), c.buffers()));
        reg(EntityMdBall.class, (e, c) -> MdBallRenderer.draw((EntityMdBall) e, c.pose(), c.buffers()));
        reg(EntityMdShield.class, (e, c) -> MdShieldRenderer.draw((EntityMdShield) e, c.pose(), c.buffers()));
        reg(EntityDiamondShield.class, (e, c) -> DiamondShieldRenderer.draw((EntityDiamondShield) e, c.pose(), c.buffers()));
        reg(EntityThunderStrike.class, (e, c) -> ThunderStrikeRenderer.draw((EntityThunderStrike) e, c.pose(), c.buffers()));
        reg(EntityTPMarking.class, (e, c) -> TPMarkingRenderer.draw((EntityTPMarking) e, c.pose(), c.buffers()));
        reg(EntityWave.class, (e, c) -> WaveRenderer.draw((EntityWave) e, c.pose(), c.buffers()));
        reg(EntityPlasmaTornado.class, (e, c) -> PlasmaTornadoRenderer.draw((EntityPlasmaTornado) e, c.pose(), c.buffers()));

        reg(EntityGustTornado.class, (e, c) ->
                GustTornadoRenderer.draw((EntityGustTornado) e, c.pt(), c.cam(), c.pose(), c.buffers()));

        reg(EntityArc.class, (e, c) -> ArcRenderer.draw((EntityArc) e, c.pt(), c.pose(), c.buffers()));
        reg(EntityRailgunHand.class, (e, c) -> RailgunHandRenderer.draw((EntityRailgunHand) e, c.pt(), c.pose(), c.buffers()));
        reg(EntityMarker.class, (e, c) -> MarkerRenderer.draw((EntityMarker) e, c.pt(), c.pose(), c.buffers()));
        reg(EntityParabola.class, (e, c) -> ParabolaRenderer.draw((EntityParabola) e, c.pt(), c.pose(), c.buffers()));

        reg(EntityRailgunFX.class, (e, c) -> RailgunFXRenderer.draw((EntityRailgunFX) e, c.pt(), c.cam(), c.pose(), c.buffers()));
        reg(EntityMdRaySmall.class, (e, c) -> MdRaySmallRenderer.draw((EntityMdRaySmall) e, c.pt(), c.cam(), c.pose(), c.buffers()));
        reg(EntityMDRay.class, (e, c) -> MDRayRenderer.draw((EntityMDRay) e, c.pt(), c.cam(), c.pose(), c.buffers()));
        reg(EntityMdRayBarrage.class, (e, c) -> MdRayBarrageRenderer.draw((EntityMdRayBarrage) e, c.pt(), c.cam(), c.pose(), c.buffers()));
        reg(EntityStormWing.class, (e, c) -> StormWingRenderer.draw((EntityStormWing) e, c.pt(), c.cam(), c.pose(), c.buffers()));
        reg(EntityDualWing.class, (e, c) -> DualWingRenderer.draw((EntityDualWing) e, c.pt(), c.cam(), c.pose(), c.buffers()));
        reg(EntityPlasmaBody.class, (e, c) -> PlasmaBodyRenderer.draw((EntityPlasmaBody) e, c.pt(), c.cam(), c.pose(), c.buffers()));
    }

    private static final Set<Class<?>> BROKEN = java.util.Collections.newSetFromMap(new IdentityHashMap<>());

    public static void draw(Entity e, EffectDrawCtx ctx) {
        Class<?> cls = e.getClass();
        if (BROKEN.contains(cls)) {
            return;
        }
        Drawer d = find(cls);
        if (d == null) {
            if (WARNED.add(cls)) {
                LOGGER.error("[AC] {} implements ACEffect and entered the draw list, "
                        + "but has no renderer registered -- it will NOT be drawn. "
                        + "Addon mods: call ACEffectAPI.register(...). "
                        + "AcademyCraft itself: add an entry in EffectDrawers' static block.",
                        cls.getName());
            }
            return;
        }
        try {
            d.draw(e, ctx);
        } catch (Throwable t) {
            BROKEN.add(cls);
            LOGGER.error("[AC] Renderer for {} threw an exception. Drawing of this effect type "
                    + "is now DISABLED for the rest of this session (it will not be retried). "
                    + "If it comes from an addon mod, send this stack trace to its author.",
                    cls.getName(), t);
        }
    }

    private static Drawer find(Class<?> cls) {
        Drawer cached = CACHE.get(cls);
        if (cached != null) {
            return cached;
        }
        for (Class<?> k = cls; k != null && Entity.class.isAssignableFrom(k); k = k.getSuperclass()) {
            Drawer d = REG.get(k);
            if (d != null) {
                CACHE.put(cls, d);
                return d;
            }
        }
        return null;
    }
}
