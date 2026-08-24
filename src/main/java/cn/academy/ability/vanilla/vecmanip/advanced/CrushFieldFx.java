package cn.academy.ability.vanilla.vecmanip.advanced;

import cn.academy.AcademyCraft;
import cn.academy.client.render.ACEffectShaders;
import cn.academy.client.render.ScreenCopy;
import cn.academy.config.Property;
import cn.lambdalib2.util.GameTimer;
import cn.lambdalib2.util.RandUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class CrushFieldFx {

    public static final String CATEGORY = "generic", KEY = "dualWingCrushFx";

    private static Property prop;

    private CrushFieldFx() {}

    public static void init() {
        prop = AcademyCraft.config.get(CATEGORY, KEY, true,
                "Show the space-distortion effect of the Dual Wings' gravity field. "
                        + "Turning it off also removes a per-frame full-screen copy.");
    }

    public static boolean enabled() {
        return prop == null || prop.getBoolean();
    }

    private static final double SPAWN_R = 18.0;

    private static final double SPAWN_NEAR = 3.5;

    private static final double BOTTOM_LOW = 1.5, BOTTOM_HIGH = 2.5;

    private static final double LEN_MIN = 3.0, LEN_MAX = 6.5;

    private static final int SPAWN_EVERY = 6;

    private static final int SPAWN_COUNT = 2;

    private static final double FOE_R = 20.0;

    private static final int FOE_EVERY = 10;

    private static final int MAX_FOES = 6;

    private static final double FOE_OFFSET_MIN = 0.2, FOE_OFFSET_MAX = 1.6;

    private static final double FOE_LEN_MIN = 2.2, FOE_LEN_MAX = 4.0;

    private static final double LIFE_MIN = 1.1, LIFE_MAX = 1.9;

    private static final int MAX_SEGS = 24;

    private static final double RADIUS_W = 2.2;

    private static final double PULL_W = 0.85;

    private static final float ALPHA = 1.0f;

    private static final float GLOW = 0.22f;

    private static final float MIN_R_PX = 6.0f;

    private static final class Seg {
        final double ax, ay, az, bx, by, bz;
        final double born, life;

        Seg(double ax, double ay, double az, double bx, double by, double bz, double life) {
            this.ax = ax;
            this.ay = ay;
            this.az = az;
            this.bx = bx;
            this.by = by;
            this.bz = bz;
            this.born = GameTimer.getPausableTime();
            this.life = life;
        }

        float envelope(double now) {
            double t = (now - born) / life;
            if (t <= 0 || t >= 1) {
                return 0;
            }
            if (t < 0.2) {
                return (float) (t / 0.2);
            }
            if (t > 0.65) {
                return (float) ((1 - t) / 0.35);
            }
            return 1;
        }

        boolean dead(double now) {
            return now - born >= life;
        }
    }

    private static final List<Seg> SEGS = new ArrayList<>();

    public static boolean hasAny() {
        return !SEGS.isEmpty();
    }

    public static void clear() {
        SEGS.clear();
    }

    public static final class State {
        private int spawnCd;
        private int foeCd;

        public void reset() {
            spawnCd = 0;
            foeCd = 0;
        }
    }

    public static void tick(Player owner, State st) {
        if (owner == null || !enabled()) {
            return;
        }
        java.util.List<LivingEntity> foes = nearbyFoes(owner);
        if (!foes.isEmpty()) {
            st.spawnCd = 0;
            if (--st.foeCd <= 0) {
                st.foeCd = FOE_EVERY;
                for (LivingEntity foe : foes) {
                    if (SEGS.size() >= MAX_SEGS) {
                        break;
                    }
                    spawnAt(foe);
                }
            }
            return;
        }
        if (--st.spawnCd <= 0) {
            st.spawnCd = SPAWN_EVERY;
            for (int i = 0; i < SPAWN_COUNT && SEGS.size() < MAX_SEGS; i++) {
                spawn(owner);
            }
        }
    }

    private static java.util.List<LivingEntity> nearbyFoes(Player owner) {
        AABB box = owner.getBoundingBox().inflate(FOE_R);
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : owner.level().getEntitiesOfClass(LivingEntity.class, box)) {
            if (e == owner || !e.isAlive() || e.isRemoved()) {
                continue;
            }
            if (e.distanceToSqr(owner) > FOE_R * FOE_R) {
                continue;
            }
            out.add(e);
        }
        if (out.size() > MAX_FOES) {
            out.sort(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(owner)));
            out = out.subList(0, MAX_FOES);
        }
        return out;
    }

    private static void spawnAt(LivingEntity foe) {

        double cx = foe.getX();
        double cy = foe.getY() + foe.getBbHeight() * 0.5;
        double cz = foe.getZ();
        double off = RandUtils.ranged(FOE_OFFSET_MIN, FOE_OFFSET_MAX);
        double[] o = randomDir();
        cx += o[0] * off;
        cy += o[1] * off;
        cz += o[2] * off;

        double[] d = randomDir();
        double half = RandUtils.ranged(FOE_LEN_MIN, FOE_LEN_MAX) * 0.5;
        SEGS.add(new Seg(
                cx - d[0] * half, cy - d[1] * half, cz - d[2] * half,
                cx + d[0] * half, cy + d[1] * half, cz + d[2] * half,
                RandUtils.ranged(LIFE_MIN, LIFE_MAX)));
    }

    private static double[] randomDir() {
        double theta = RandUtils.ranged(0, Math.PI * 2);
        double cosPhi = RandUtils.ranged(-1, 1);
        double sinPhi = Math.sqrt(Math.max(0, 1 - cosPhi * cosPhi));
        return new double[]{sinPhi * Math.cos(theta), cosPhi, sinPhi * Math.sin(theta)};
    }

    private static void spawn(Player owner) {

        double a = RandUtils.ranged(0, Math.PI * 2);
        double r = SPAWN_NEAR + (SPAWN_R - SPAWN_NEAR) * Math.sqrt(RandUtils.ranged(0, 1));
        double x = owner.getX() + r * Math.cos(a);
        double z = owner.getZ() + r * Math.sin(a);
        double y = owner.getY() + RandUtils.ranged(-BOTTOM_LOW, BOTTOM_HIGH);
        SEGS.add(new Seg(x, y, z,
                x, y + RandUtils.ranged(LEN_MIN, LEN_MAX), z,
                RandUtils.ranged(LIFE_MIN, LIFE_MAX)));
    }

    public static void drawAll(Matrix4f camRot, Vec3 cam) {
        double now = GameTimer.getPausableTime();
        SEGS.removeIf(s -> s.dead(now));
        if (SEGS.isEmpty() || !ScreenCopy.ready()) {
            return;
        }
        ShaderInstance shader = ACEffectShaders.gravityLens();
        if (shader == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        int sw = mc.getMainRenderTarget().width;
        int sh = mc.getMainRenderTarget().height;
        if (sw <= 0 || sh <= 0) {
            return;
        }

        Matrix4f mvp = new Matrix4f(RenderSystem.getProjectionMatrix()).mul(camRot);

        float m11 = RenderSystem.getProjectionMatrix().m11();
        float projYScale = m11 > 1.0e-4f ? m11 : 1.0f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();

        RenderSystem.depthMask(false);
        RenderSystem.setShaderTexture(0, ScreenCopy.textureId());
        RenderSystem.setShader(ACEffectShaders::gravityLens);

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder bb = tess.getBuilder();

        for (Seg s : SEGS) {
            float env = s.envelope(now);
            if (env <= 0) {
                continue;
            }
            drawOne(s, env, shader, bb, tess, mvp, cam, sw, sh, projYScale);
        }

        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private static void drawOne(Seg s, float env, ShaderInstance shader,
                                BufferBuilder bb, Tesselator tess,
                                Matrix4f mvp, Vec3 cam, int sw, int sh, float projYScale) {
        Vector4f a = project(s.ax, s.ay, s.az, mvp, cam);
        Vector4f b = project(s.bx, s.by, s.bz, mvp, cam);

        if (a == null || b == null) {
            return;
        }
        float ax = (a.x / a.w * 0.5f + 0.5f) * sw;
        float ay = (a.y / a.w * 0.5f + 0.5f) * sh;
        float bx = (b.x / b.w * 0.5f + 0.5f) * sw;
        float by = (b.y / b.w * 0.5f + 0.5f) * sh;

        float w = (a.w + b.w) * 0.5f;
        float rPx = (float) (RADIUS_W * projYScale * sh * 0.5f / w);
        if (rPx < MIN_R_PX) {
            return;
        }
        float pullPx = (float) (PULL_W * projYScale * sh * 0.5f / w) * env;

        float ndcZ = (a.z / a.w + b.z / b.w) * 0.5f;
        if (ndcZ < -1 || ndcZ > 1) {
            return;
        }

        float x0 = Math.min(ax, bx) - rPx, x1 = Math.max(ax, bx) + rPx;
        float y0 = Math.min(ay, by) - rPx, y1 = Math.max(ay, by) + rPx;

        if (x1 < 0 || x0 > sw || y1 < 0 || y0 > sh) {
            return;
        }
        float nx0 = x0 / sw * 2 - 1, nx1 = x1 / sw * 2 - 1;
        float ny0 = y0 / sh * 2 - 1, ny1 = y1 / sh * 2 - 1;

        shader.safeGetUniform("ScreenSize").set((float) sw, (float) sh);
        shader.safeGetUniform("SegA").set(ax, ay);
        shader.safeGetUniform("SegB").set(bx, by);
        shader.safeGetUniform("LensParam").set(rPx, pullPx, ALPHA * env, GLOW * env);

        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        bb.vertex(nx0, ny0, ndcZ).endVertex();
        bb.vertex(nx1, ny0, ndcZ).endVertex();
        bb.vertex(nx1, ny1, ndcZ).endVertex();
        bb.vertex(nx0, ny1, ndcZ).endVertex();
        tess.end();
    }

    private static Vector4f project(double wx, double wy, double wz, Matrix4f mvp, Vec3 cam) {
        Vector4f v = new Vector4f(
                (float) (wx - cam.x), (float) (wy - cam.y), (float) (wz - cam.z), 1.0f);
        mvp.transform(v);
        return v.w > 1.0e-4f ? v : null;
    }
}
