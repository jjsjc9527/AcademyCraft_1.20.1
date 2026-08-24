package cn.academy.entity;

import cn.academy.ACEntities;
import cn.academy.ability.vanilla.meltdowner.skill.MdBarrage;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EntityMdRayBarrage extends EntityRayBase {

    public record SubRay(float yawOff, float pitchOff, double reach) {}

    private SubRay[] subrays = new SubRay[0];

    private int seed = 0;

    private long flickerMs = 250;

    private int lastFlicker = -1;

    private long inMs = 83, shrinkMs = 125, outMs = 166;

    private long flickerDt() {
        return getDeltaTime() % Math.max(1, flickerMs);
    }

    public void updateFlicker(Vec3 axis) {
        int idx = (int) (getDeltaTime() / Math.max(1, flickerMs));
        if (idx != lastFlicker) {
            lastFlicker = idx;
            reroll(axis);
            playFlickerSound();
        }
    }

    private static final long SOUND_MERGE_MS = 120;

    private static long lastSoundMs = Long.MIN_VALUE / 2;

    private void playFlickerSound() {
        long now = System.currentTimeMillis();
        if (now - lastSoundMs < SOUND_MERGE_MS) {
            return;
        }
        lastSoundMs = now;
        level().playLocalSound(getX(), getY(), getZ(),
                cn.academy.ACSounds.MD_MEL_LASER.get(),
                net.minecraft.sounds.SoundSource.AMBIENT, 0.8f, 1.0f, false);
    }

    @Override
    public double getLength() {
        long dt = flickerDt();
        return (inMs > 0 && dt < inMs ? (double) dt / inMs : 1) * length;
    }

    @Override
    public double getAlpha() {
        long dt = flickerDt();
        long from = flickerMs - outMs;
        return outMs > 0 && dt > from ? Math.max(0, 1 - (double) (dt - from) / outMs) : 1.0;
    }

    @Override
    public double getWidth() {
        long dt = flickerDt();
        long from = flickerMs - shrinkMs;
        double w = shrinkMs > 0 && dt > from ? 1 - (double) (dt - from) / shrinkMs : 1.0;

        return Math.max(0, widthWiggle + w);
    }

    private void reroll(Vec3 axis) {
        int pass = Math.max(0, lastFlicker);
        float spread = MdBarrage.spreadFor(seed, pass);
        Vec3[] basis = MdBarrage.basisOf(axis);
        Vec3 at = position();

        java.util.List<net.minecraft.world.entity.Entity> candidates =
                MdBarrage.candidatesAround(level(), getPlayer(), at, length);
        for (int i = 0; i < subrays.length; i++) {
            float y = MdBarrage.yawOffFor(seed, pass, i, spread);
            float p = MdBarrage.pitchOffFor(seed, pass, i, spread);
            Vec3 dir = MdBarrage.subRayDir(axis, basis[0], basis[1], y, p);
            subrays[i] = new SubRay(y, p,
                    MdBarrage.traceRay(level(), getPlayer(), at, dir, length, candidates).reach());
        }
    }

    public EntityMdRayBarrage(Level level) {
        super(ACEntities.MD_RAY_BARRAGE.get(), level);

        this.blendInTime = 200;
        this.blendOutTime = 400;
        this.life = 50;
        this.length = 15.0;
        this.viewOptimize = false;
        noCulling = true;
    }

    public SubRay[] getSubrays() {
        return subrays;
    }

    public void burst(Vec3 from, Vec3 dir, double len, int seed, int count,
                      int flickerInterval, int flickerTicks) {
        Vec3 d = dir.normalize();
        setPos(from.x, from.y, from.z);
        xOld = getX();
        yOld = getY();
        zOld = getZ();

        double dxz = Math.sqrt(d.x * d.x + d.z * d.z);
        setYRot((float) (-Math.atan2(d.x, d.z) * 180 / Math.PI));
        setXRot((float) (-Math.atan2(d.y, dxz) * 180 / Math.PI));
        this.length = len;
        this.seed = seed;
        this.flickerMs = Math.max(1, flickerInterval) * 50L;
        this.life = Math.max(1, flickerTicks);

        long need = blendInTime + Math.max(blendOutTime, widthShrinkTime);
        double k = need <= 0 ? 1 : Math.min(1.0, (double) flickerMs / need);
        this.inMs = (long) (blendInTime * k);
        this.shrinkMs = (long) (widthShrinkTime * k);
        this.outMs = (long) (blendOutTime * k);

        subrays = new SubRay[Math.max(1, count)];
        java.util.Arrays.fill(subrays, new SubRay(0, 0, 0));
    }
}
